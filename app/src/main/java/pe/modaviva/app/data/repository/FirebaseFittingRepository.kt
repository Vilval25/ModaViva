package pe.modaviva.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.google.mlkit.vision.pose.PoseLandmark
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import pe.modaviva.app.domain.model.ConstanciaEliminacionFoto
import pe.modaviva.app.domain.model.FittingPhotoError
import pe.modaviva.app.domain.model.FittingPhotoException
import pe.modaviva.app.domain.model.sinFotoProbador
import pe.modaviva.app.domain.repository.FittingRepository
import pe.modaviva.app.domain.repository.SessionRepository
import java.io.ByteArrayOutputStream
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseFittingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val sessionRepository: SessionRepository,
) : FittingRepository {

    override suspend fun validateImageMetadata(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val contentResolver = context.contentResolver

            // 1. Validar formato (JPG o PNG)
            val mimeType = contentResolver.getType(uri)?.lowercase()
            val isJpeg = mimeType == "image/jpeg" || mimeType == "image/jpg"
            val isPng = mimeType == "image/png"
            if (!isJpeg && !isPng) {
                // Verificar por extensión si el mimeType es genérico
                val path = uri.path?.lowercase().orEmpty()
                val hasValidExt = path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png")
                if (!hasValidExt) {
                    throw FittingPhotoException(FittingPhotoError.InvalidFormat)
                }
            }

            // 2. Validar tamaño (máximo 10 MB)
            var fileSize: Long = -1
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1 && cursor.moveToFirst()) {
                    fileSize = cursor.getLong(sizeIndex)
                }
            }
            if (fileSize <= 0) {
                fileSize = contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1
            }
            val maxBytes = 10L * 1024 * 1024
            if (fileSize > maxBytes) {
                throw FittingPhotoException(FittingPhotoError.FileTooLarge(10))
            }

            // 3. Validar dimensiones (mínimo 720 × 1280 px)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            val width = options.outWidth
            val height = options.outHeight

            val meetsPortrait = width >= 720 && height >= 1280
            val meetsLandscape = width >= 1280 && height >= 720
            if (!meetsPortrait && !meetsLandscape) {
                throw FittingPhotoException(FittingPhotoError.ResolutionTooLow(720, 1280))
            }
        }
    }

    override suspend fun validatePose(bitmap: Bitmap): Result<Boolean> = withContext(Dispatchers.Default) {
        runCatching {
            val options = PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
                .build()
            val detector = PoseDetection.getClient(options)
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val pose = detector.process(inputImage).await()

            val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
            val leftEye = pose.getPoseLandmark(PoseLandmark.LEFT_EYE)
            val rightEye = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE)
            val hasHead = (nose?.inFrameLikelihood ?: 0f) > 0.35f ||
                    (leftEye?.inFrameLikelihood ?: 0f) > 0.35f ||
                    (rightEye?.inFrameLikelihood ?: 0f) > 0.35f

            val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
            val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
            val hasShoulders = (leftShoulder?.inFrameLikelihood ?: 0f) > 0.35f &&
                    (rightShoulder?.inFrameLikelihood ?: 0f) > 0.35f

            val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
            val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
            val hasHips = (leftHip?.inFrameLikelihood ?: 0f) > 0.35f &&
                    (rightHip?.inFrameLikelihood ?: 0f) > 0.35f

            val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
            val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
            val hasKnees = (leftKnee?.inFrameLikelihood ?: 0f) > 0.35f ||
                    (rightKnee?.inFrameLikelihood ?: 0f) > 0.35f

            val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
            val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
            val hasAnkles = (leftAnkle?.inFrameLikelihood ?: 0f) > 0.35f ||
                    (rightAnkle?.inFrameLikelihood ?: 0f) > 0.35f

            val isFullBody = hasHead && hasShoulders && hasHips && hasKnees && hasAnkles
            if (!isFullBody) {
                throw FittingPhotoException(FittingPhotoError.NotFullBody)
            }
            true
        }
    }

    override suspend fun uploadFittingPhoto(
        bitmap: Bitmap,
        onProgress: (Float) -> Unit,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val user = auth.currentUser
                ?: throw IllegalStateException("Debes iniciar sesión para registrar tu foto")

            // Comprimir y optimizar la imagen convirtiéndola a WebP (CA-04)
            val scaledBitmap = scaleBitmapIfNeeded(bitmap, maxDimension = 1920)
            val outputStream = ByteArrayOutputStream()

            val compressFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            scaledBitmap.compress(compressFormat, 80, outputStream)
            val bytes = outputStream.toByteArray()

            // Liberar memoria del bitmap escalado si es diferente del original
            if (scaledBitmap !== bitmap) {
                scaledBitmap.recycle()
            }

            val photoRef = storage.reference.child("clientes/${user.uid}/probador/foto_cuerpo_completo.webp")
            val metadata = StorageMetadata.Builder()
                .setContentType("image/webp")
                .setCustomMetadata("subidoPor", user.uid)
                .build()

            // Subir con seguimiento de progreso (CA-04)
            val uploadTask = photoRef.putBytes(bytes, metadata)
            uploadTask.addOnProgressListener { taskSnapshot ->
                val total = taskSnapshot.totalByteCount
                if (total > 0) {
                    val progress = taskSnapshot.bytesTransferred.toFloat() / total.toFloat()
                    onProgress(progress.coerceIn(0f, 1f))
                }
            }.await()

            val downloadUrl = photoRef.downloadUrl.await().toString()

            // Extraer proporciones anatómicas (ratios hombro y cadera) de la pose
            val (rHombro, rCadera) = try {
                val poseOptions = PoseDetectorOptions.Builder()
                    .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
                    .build()
                val detector = PoseDetection.getClient(poseOptions)
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val pose = detector.process(inputImage).await()

                val lShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
                val rShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
                val lHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
                val rHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
                val lAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
                val rAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
                val nose = pose.getPoseLandmark(PoseLandmark.NOSE)

                if (lShoulder != null && rShoulder != null && lHip != null && rHip != null && nose != null) {
                    val shoulderWidth = kotlin.math.abs(lShoulder.position.x - rShoulder.position.x)
                    val hipWidth = kotlin.math.abs(lHip.position.x - rHip.position.x)
                    val ankleY = if (lAnkle != null && rAnkle != null) {
                        (lAnkle.position.y + rAnkle.position.y) / 2f
                    } else {
                        (lAnkle ?: rAnkle)?.position?.y ?: 0f
                    }
                    val bodyHeight = if (ankleY > nose.position.y) {
                        kotlin.math.abs(ankleY - nose.position.y) * 1.15f
                    } else {
                        1f
                    }
                    val rH = (shoulderWidth / bodyHeight).toDouble().coerceIn(0.18, 0.32)
                    val rC = (hipWidth / bodyHeight).toDouble().coerceIn(0.16, 0.30)
                    rH to rC
                } else {
                    null to null
                }
            } catch (_: Exception) {
                null to null
            }

            // Actualizar documento de Firestore
            val probadorMap = mutableMapOf<String, Any>(
                "fotoUrl" to downloadUrl,
                "fotoActualizadaEn" to FieldValue.serverTimestamp(),
            )
            if (rHombro != null) probadorMap["ratioHombro"] = rHombro
            if (rCadera != null) probadorMap["ratioCadera"] = rCadera

            val updates = mutableMapOf<String, Any>(
                "fotoProbadorUrl" to downloadUrl,
                "probador" to probadorMap,
                "actualizadoEn" to FieldValue.serverTimestamp(),
            )
            if (rHombro != null) updates["ratioHombro"] = rHombro
            if (rCadera != null) updates["ratioCadera"] = rCadera

            firestore.collection("clientes").document(user.uid)
                .set(updates, SetOptions.merge())
                .await()

            // Actualizar sesión local
            val currentProfile = sessionRepository.currentUser.value
            if (currentProfile != null) {
                sessionRepository.signIn(
                    currentProfile.copy(
                        fotoProbadorUrl = downloadUrl,
                        ratioHombro = rHombro ?: currentProfile.ratioHombro,
                        ratioCadera = rCadera ?: currentProfile.ratioCadera,
                    )
                )
            }

            downloadUrl
        }
    }

    override suspend fun updateHeight(heightCm: Int): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (heightCm !in 100..230) {
                throw IllegalArgumentException("Ingresa una altura válida entre 100 y 230 cm")
            }

            val user = auth.currentUser
                ?: throw IllegalStateException("Debes iniciar sesión para guardar tu altura")

            val updates = mapOf(
                "alturaCm" to heightCm,
                "probador.alturaCm" to heightCm,
                "actualizadoEn" to FieldValue.serverTimestamp(),
            )
            firestore.collection("clientes").document(user.uid)
                .set(updates, SetOptions.merge())
                .await()

            val currentProfile = sessionRepository.currentUser.value
            if (currentProfile != null) {
                sessionRepository.signIn(currentProfile.copy(alturaCm = heightCm))
            }
        }
    }

    /**
     * HU-17: borra la foto de Storage y luego la quita del perfil.
     *
     * Storage va primero: si falla (por ejemplo, sin conexión), la foto sigue
     * registrada y el cliente puede reintentar, en lugar de quedar un archivo
     * huérfano que ya no se ve en la app. Un archivo que ya no existe no es un
     * error. El perfil guarda en `probador.fotoEliminadaEn` el registro de la
     * eliminación.
     */
    override suspend fun deleteFittingPhoto(): Result<ConstanciaEliminacionFoto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val user = auth.currentUser
                    ?: throw IllegalStateException("Debes iniciar sesión para eliminar tu foto")

                EliminacionFotoFirestore.ARCHIVOS_FOTO.forEach { archivo ->
                    borrarSiExiste(storage.reference.child("clientes/${user.uid}/probador/$archivo"))
                }

                val cambios = buildMap<String, Any> {
                    EliminacionFotoFirestore.CAMPOS_BORRADOS.forEach { put(it, FieldValue.delete()) }
                    put(EliminacionFotoFirestore.CAMPO_REGISTRO, FieldValue.serverTimestamp())
                    put("actualizadoEn", FieldValue.serverTimestamp())
                }
                firestore.collection("clientes").document(user.uid)
                    .update(cambios)
                    .await()

                sessionRepository.currentUser.value?.let { perfil ->
                    sessionRepository.signIn(perfil.sinFotoProbador())
                }
                ConstanciaEliminacionFoto(eliminadaEn = Instant.now())
            }
        }

    private suspend fun borrarSiExiste(archivo: StorageReference) {
        try {
            archivo.delete().await()
        } catch (e: StorageException) {
            if (e.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) throw e
        }
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}

