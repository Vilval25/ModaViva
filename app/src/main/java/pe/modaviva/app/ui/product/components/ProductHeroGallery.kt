package pe.modaviva.app.ui.product.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import pe.modaviva.app.R
import pe.modaviva.app.ui.favorites.LocalFavoritos

/** Galería hero de la prenda con indicadores de página. */
@Composable
fun ProductHeroGallery(
    nombrePrenda: String,
    fotos: List<String>,
    onPhotoClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(440.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        val pagerState = rememberPagerState(pageCount = { fotos.size })

        if (fotos.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { pagina ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(fotos[pagina])
                        .build(),
                    contentDescription = "$nombrePrenda, foto ${pagina + 1}",
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onPhotoClick(pagina) },
                    contentScale = ContentScale.Crop,
                )
            }

            // Degradado inferior para mantener contraste en las líneas indicadoras
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f)),
                        ),
                    ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(fotos.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .height(3.dp)
                                .width(if (isSelected) 28.dp else 16.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                ),
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.55f),
                    contentColor = Color.White,
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${fotos.size}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

/** Barra superior flotante y fija con las acciones de volver, favorito y carrito. */
@Composable
fun ProductHeroTopBar(
    codigoPrenda: String,
    cartItemCount: Int,
    onBack: () -> Unit,
    onCartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val overlayBg = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.94f)
        val overlayFg = MaterialTheme.colorScheme.onSurface

        HeroCircleButton(onClick = onBack, backgroundColor = overlayBg) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                tint = overlayFg,
                modifier = Modifier.size(20.dp),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val favoritos = LocalFavoritos.current
            if (favoritos != null) {
                val esFavorita = favoritos.contiene(codigoPrenda)
                HeroCircleButton(
                    onClick = { favoritos.alternar(codigoPrenda) },
                    backgroundColor = overlayBg,
                ) {
                    Icon(
                        imageVector = if (esFavorita) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(
                            if (esFavorita) R.string.cd_favorite_remove else R.string.cd_favorite_add,
                        ),
                        tint = if (esFavorita) MaterialTheme.colorScheme.error else overlayFg,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            HeroCircleButton(onClick = onCartClick, backgroundColor = overlayBg) {
                BadgedBox(
                    badge = {
                        if (cartItemCount > 0) {
                            Badge {
                                Text(text = if (cartItemCount > 99) "99+" else cartItemCount.toString())
                            }
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = stringResource(R.string.cd_cart),
                        tint = overlayFg,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCircleButton(
    onClick: () -> Unit,
    backgroundColor: Color,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = backgroundColor,
        shadowElevation = 4.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center, content = content)
    }
}

