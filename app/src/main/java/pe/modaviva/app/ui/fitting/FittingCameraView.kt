package pe.modaviva.app.ui.fitting

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Vista de cámara nativa con CameraX y silueta guía para cuerpo completo (HU-11 CA-01).
 */
@Composable
fun FittingCameraView(
    onPhotoCaptured: (Bitmap) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            imageCapture = capture

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    capture,
                )
            } catch (_: Exception) {
                // Si la cámara seleccionada no está disponible
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Vista previa de CameraX
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )

        // Overlay con silueta guía para cuerpo completo (HU-11 CA-01)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Guía de cuerpo: cabeza y cuerpo estilizado centrado
            val centerX = canvasWidth / 2f
            val topY = canvasHeight * 0.12f
            val bottomY = canvasHeight * 0.82f
            val bodyHeight = bottomY - topY

            val stroke = Stroke(
                width = 3.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)
            )
            val guideColor = Color.White.copy(alpha = 0.75f)

            // Cabeza (óvalo)
            val headRadiusX = bodyHeight * 0.08f
            val headRadiusY = bodyHeight * 0.10f
            val headCenterY = topY + headRadiusY
            drawOval(
                color = guideColor,
                topLeft = Offset(centerX - headRadiusX, headCenterY - headRadiusY),
                size = Size(headRadiusX * 2, headRadiusY * 2),
                style = stroke,
            )

            // Contorno de cuerpo/hombros/piernas
            val bodyPath = Path().apply {
                val shouldersY = headCenterY + headRadiusY + (bodyHeight * 0.04f)
                val shoulderWidth = bodyHeight * 0.22f

                // Hombros
                moveTo(centerX - shoulderWidth, shouldersY + (bodyHeight * 0.06f))
                lineTo(centerX - (shoulderWidth * 0.6f), shouldersY)
                lineTo(centerX + (shoulderWidth * 0.6f), shouldersY)
                lineTo(centerX + shoulderWidth, shouldersY + (bodyHeight * 0.06f))

                // Tronco hacia caderas
                val hipY = topY + (bodyHeight * 0.50f)
                val hipWidth = shoulderWidth * 0.85f
                lineTo(centerX + hipWidth, hipY)
                lineTo(centerX - hipWidth, hipY)
                close()

                // Pierna izquierda
                moveTo(centerX - (hipWidth * 0.7f), hipY)
                lineTo(centerX - (hipWidth * 0.5f), bottomY)

                // Pierna derecha
                moveTo(centerX + (hipWidth * 0.7f), hipY)
                lineTo(centerX + (hipWidth * 0.5f), bottomY)
            }
            drawPath(path = bodyPath, color = guideColor, style = stroke)
        }

        // Barra superior con botón de cerrar y texto guía
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = Color.White,
                )
            }

            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Alinea tu cuerpo completo",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Botón para alternar cámara frontal / trasera
            IconButton(
                onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Default.FlipCameraAndroid,
                    contentDescription = "Cambiar cámara",
                    tint = Color.White,
                )
            }
        }

        // Barra inferior con botón disparador
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
            contentAlignment = Alignment.Center,
        ) {
            FloatingActionButton(
                onClick = {
                    if (isCapturing) return@FloatingActionButton
                    val capture = imageCapture ?: return@FloatingActionButton
                    isCapturing = true

                    val executor = ContextCompat.getMainExecutor(context)
                    capture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            try {
                                val rotation = image.imageInfo.rotationDegrees
                                val rawBitmap = image.toBitmap()
                                val matrix = Matrix().apply {
                                    postRotate(rotation.toFloat())
                                    if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                                        postScale(-1f, 1f) // Espejo frontal
                                    }
                                }
                                val finalBitmap = Bitmap.createBitmap(
                                    rawBitmap, 0, 0,
                                    rawBitmap.width, rawBitmap.height,
                                    matrix, true
                                )
                                onPhotoCaptured(finalBitmap)
                            } finally {
                                image.close()
                                isCapturing = false
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            isCapturing = false
                        }
                    })
                },
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = "Tomar foto",
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}

