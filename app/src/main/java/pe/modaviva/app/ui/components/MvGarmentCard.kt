package pe.modaviva.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pe.modaviva.app.R

/**
 * Tarjeta de prenda para el catálogo, las búsquedas y Favoritos.
 *
 * La imagen llega como contenido ([image]) para que la tarjeta no dependa de
 * cómo se cargan las fotos; quien la usa pone ahí su AsyncImage. Los precios
 * llegan ya formateados ("S/ 89.90").
 *
 * @param brand marca, sobre el nombre; null la oculta.
 * @param originalPrice precio anterior, solo si hay promoción; se muestra tachado.
 * @param isFavorite null oculta el corazón (p. ej. donde no aplica).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MvGarmentCard(
    name: String,
    price: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    brand: String? = null,
    originalPrice: String? = null,
    isSoldOut: Boolean = false,
    isFavorite: Boolean? = null,
    onFavoriteClick: () -> Unit = {},
    image: @Composable BoxScope.() -> Unit = { GarmentImagePlaceholder() },
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f),
        ) {
            image()
            if (isSoldOut) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                ) {
                    Text(
                        text = stringResource(R.string.garment_sold_out),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            if (isFavorite != null) {
                FilledTonalIconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                ) {
                    Icon(
                        imageVector = if (isFavorite) {
                            Icons.Filled.Favorite
                        } else {
                            Icons.Outlined.FavoriteBorder
                        },
                        contentDescription = stringResource(
                            if (isFavorite) R.string.cd_favorite_remove else R.string.cd_favorite_add,
                        ),
                    )
                }
            }
        }
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (brand != null) {
                Text(
                    text = brand.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            // FlowRow: si no caben juntos, el precio tachado baja de línea entero.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = price,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (originalPrice != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    softWrap = false,
                )
                if (originalPrice != null) {
                    Text(
                        text = originalPrice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

/** Fondo neutro mientras la foto carga o si la prenda no tiene foto. */
@Composable
fun GarmentImagePlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Checkroom,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
    }
}

/**
 * Tarjeta vacía con la misma forma que [MvGarmentCard], para mostrar mientras
 * carga el catálogo por primera vez.
 */
@Composable
fun MvGarmentCardSkeleton(modifier: Modifier = Modifier) {
    val relleno = MaterialTheme.colorScheme.surfaceContainerHigh
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .background(relleno),
        )
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.4f)
                    .height(10.dp)
                    .background(relleno, MaterialTheme.shapes.extraSmall),
            )
            Box(
                Modifier
                    .fillMaxWidth(0.8f)
                    .height(14.dp)
                    .background(relleno, MaterialTheme.shapes.extraSmall),
            )
            Box(
                Modifier
                    .fillMaxWidth(0.3f)
                    .height(14.dp)
                    .background(relleno, MaterialTheme.shapes.extraSmall),
            )
        }
    }
}
