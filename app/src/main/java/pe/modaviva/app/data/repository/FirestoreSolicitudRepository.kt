package pe.modaviva.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import pe.modaviva.app.domain.model.EstadoSolicitud
import pe.modaviva.app.domain.model.NuevaSolicitud
import pe.modaviva.app.domain.model.SolicitudPostventa
import pe.modaviva.app.domain.model.TipoNotificacion
import pe.modaviva.app.domain.model.TipoSolicitud
import pe.modaviva.app.domain.repository.AnalyticsRepository
import pe.modaviva.app.domain.repository.NotificacionRepository
import pe.modaviva.app.domain.repository.SolicitudRepository
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * HU-24 — Solicitudes de cambio o devolución en clientes/{uid}/solicitudes/{id} y sus fotos en
 * Storage (clientes/{uid}/solicitudes/{id}/foto_N.jpg). La app solo crea y lee; el estado y la
 * resolución los cambia el back-office (CA-07).
 */
@Singleton
class FirestoreSolicitudRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
    private val analytics: AnalyticsRepository,
    private val notificaciones: NotificacionRepository,
) : SolicitudRepository {

    override fun observarSolicitudes(): Flow<List<SolicitudPostventa>> = observar { it }

    override fun observarSolicitudesDePedido(pedidoId: String): Flow<List<SolicitudPostventa>> =
        observar { it.whereEqualTo("pedidoId", pedidoId) }

    override fun observarSolicitud(solicitudId: String): Flow<SolicitudPostventa?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val registro = coleccion(uid).document(solicitudId).addSnapshotListener { snap, error ->
            trySend(if (error != null) null else snap?.data?.let { mapear(snap.id, it) })
        }
        awaitClose { registro.remove() }
    }

    private fun observar(filtro: (Query) -> Query): Flow<List<SolicitudPostventa>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registro = filtro(coleccion(uid)).addSnapshotListener { snap, error ->
            if (error != null || snap == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            trySend(
                snap.documents
                    .mapNotNull { doc -> doc.data?.let { mapear(doc.id, it) } }
                    .sortedByDescending { it.creadaEnMillis },
            )
        }
        awaitClose { registro.remove() }
    }

    override suspend fun registrar(
        nueva: NuevaSolicitud,
        fotos: List<Uri>,
        onProgreso: (Float) -> Unit,
    ): Result<SolicitudPostventa> = runCatching {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Inicia sesión para registrar la solicitud.")
        require(fotos.size <= SolicitudPostventa.MAX_FOTOS) { "Puedes adjuntar como máximo 3 fotos." }

        val col = coleccion(uid)
        val docRef = col.document()

        // 1. Fotos comprimidas a Storage (CA-04).
        val urls = fotos.mapIndexed { i, uri ->
            val bytes = withContext(Dispatchers.IO) { comprimir(uri) }
            val ref = storage.reference.child("clientes/$uid/solicitudes/${docRef.id}/foto_${i + 1}.jpg")
            ref.putBytes(bytes, StorageMetadata.Builder().setContentType("image/jpeg").build()).await()
            onProgreso((i + 1f) / (fotos.size + 1f))
            ref.downloadUrl.await().toString()
        }

        // 2. Número: SOL-<número del pedido>-<n.º de solicitud de ese pedido> (CA-05).
        val previas = col.whereEqualTo("pedidoId", nueva.pedido.id).get().await().size()
        val digitos = nueva.pedido.numeroPedido.filter(Char::isDigit).ifBlank { nueva.pedido.id.takeLast(6) }
        val numero = "SOL-$digitos-${previas + 1}"

        // 3. Prueba virtual previa según el historial de pruebas o la marca del pedido (CA-06).
        val historial = runCatching { analytics.observarPruebasVirtuales().first() }.getOrDefault(emptyList())
        val tuvoPrueba = nueva.item.usoProbadorVirtual ||
            historial.any { it.prendaCodigo == nueva.item.codigo }

        val ahora = System.currentTimeMillis()
        val solicitud = SolicitudPostventa(
            id = docRef.id,
            numeroSolicitud = numero,
            pedidoId = nueva.pedido.id,
            numeroPedido = nueva.pedido.numeroPedido,
            prendaCodigo = nueva.item.codigo,
            prendaNombre = nueva.item.nombre,
            prendaFotoUrl = nueva.item.fotoUrl,
            talla = nueva.item.talla,
            tipo = nueva.tipo,
            motivo = nueva.motivo,
            detalleOtro = nueva.detalleOtro?.trim()?.ifBlank { null },
            fotosUrls = urls,
            tuvoPruebaVirtual = tuvoPrueba,
            estado = EstadoSolicitud.EN_EVALUACION,
            creadaEnMillis = ahora,
        )

        docRef.set(
            mapOf(
                "numeroSolicitud" to solicitud.numeroSolicitud,
                "pedidoId" to solicitud.pedidoId,
                "numeroPedido" to solicitud.numeroPedido,
                "prendaCodigo" to solicitud.prendaCodigo,
                "prendaNombre" to solicitud.prendaNombre,
                "prendaFotoUrl" to solicitud.prendaFotoUrl,
                "talla" to solicitud.talla,
                "tipo" to solicitud.tipo.codigo,
                "motivo" to solicitud.motivo,
                "detalleOtro" to solicitud.detalleOtro,
                "fotosUrls" to solicitud.fotosUrls,
                "tuvoPruebaVirtual" to solicitud.tuvoPruebaVirtual,
                "estado" to solicitud.estado.codigo,
                "creadaEnMillis" to ahora,
                "creadaEn" to FieldValue.serverTimestamp(),
            ),
        ).await()
        onProgreso(1f)

        notificaciones.crear(
            tipo = TipoNotificacion.SOLICITUD,
            titulo = "Solicitud ${solicitud.numeroSolicitud} registrada",
            texto = "Tu solicitud de ${solicitud.tipo.etiqueta.lowercase()} de ${solicitud.prendaNombre} está En evaluación.",
            refId = solicitud.id,
        )
        solicitud
    }

    private fun coleccion(uid: String) =
        firestore.collection("clientes").document(uid).collection("solicitudes")

    /** Reduce la foto a 1600 px de lado mayor en JPEG 80 (respeta la rotación de la cámara). */
    private fun comprimir(uri: Uri): ByteArray {
        val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val fuente = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(fuente) { decoder, info, _ ->
                val lado = max(info.size.width, info.size.height)
                if (lado > LADO_MAX) {
                    val escala = LADO_MAX.toFloat() / lado
                    decoder.setTargetSize((info.size.width * escala).toInt(), (info.size.height * escala).toInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
            var muestra = 1
            while (max(limites.outWidth, limites.outHeight) / (muestra * 2) >= LADO_MAX) muestra *= 2
            val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
                ?: throw IllegalStateException("No se pudo leer una de las fotos.")
        }
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapear(id: String, d: Map<String, Any?>) = SolicitudPostventa(
        id = id,
        numeroSolicitud = d["numeroSolicitud"] as? String ?: id,
        pedidoId = d["pedidoId"] as? String ?: "",
        numeroPedido = d["numeroPedido"] as? String ?: "",
        prendaCodigo = d["prendaCodigo"] as? String ?: "",
        prendaNombre = d["prendaNombre"] as? String ?: "",
        prendaFotoUrl = d["prendaFotoUrl"] as? String ?: "",
        talla = d["talla"] as? String ?: "",
        tipo = TipoSolicitud.desdeCodigo(d["tipo"] as? String),
        motivo = d["motivo"] as? String ?: "",
        detalleOtro = d["detalleOtro"] as? String,
        fotosUrls = (d["fotosUrls"] as? List<String>).orEmpty(),
        tuvoPruebaVirtual = d["tuvoPruebaVirtual"] as? Boolean ?: false,
        estado = EstadoSolicitud.desdeCodigo(d["estado"] as? String),
        resolucion = d["resolucion"] as? String,
        creadaEnMillis = (d["creadaEnMillis"] as? Number)?.toLong() ?: 0L,
        resueltaEnMillis = (d["resueltaEnMillis"] as? Number)?.toLong(),
    )

    private companion object {
        const val LADO_MAX = 1600
    }
}
