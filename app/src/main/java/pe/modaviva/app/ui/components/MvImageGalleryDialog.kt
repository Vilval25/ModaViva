package pe.modaviva.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage

@Composable
fun MvImageGalleryDialog(
    fotos: List<String>,
    initialPage: Int = 0,
    onDismiss: () -> Unit,
) {
    if (fotos.isEmpty()) return

    val paginaInicial = initialPage.coerceIn(0, fotos.lastIndex)

    val pagerState = rememberPagerState(
        initialPage = paginaInicial,
        pageCount = { fotos.size },
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { pagina ->
                var escala by remember(pagina) {
                    mutableFloatStateOf(1f)
                }
                var desplazamientoX by remember(pagina) {
                    mutableFloatStateOf(0f)
                }
                var desplazamientoY by remember(pagina) {
                    mutableFloatStateOf(0f)
                }

                AsyncImage(
                    model = fotos[pagina],
                    contentDescription = "Foto ${pagina + 1} de ${fotos.size}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = escala
                            scaleY = escala
                            translationX = desplazamientoX
                            translationY = desplazamientoY
                        }
                        .pointerInput(pagina) {
                            detectTransformGestures {
                                    _, pan, zoom, _ ->
                                escala = (escala * zoom)
                                    .coerceIn(1f, 4f)

                                desplazamientoX += pan.x
                                desplazamientoY += pan.y

                                if (escala == 1f) {
                                    desplazamientoX = 0f
                                    desplazamientoY = 0f
                                }
                            }
                        },
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(
                            alpha = 0.85f,
                        ),
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar galería",
                )
            }
        }
    }
}
