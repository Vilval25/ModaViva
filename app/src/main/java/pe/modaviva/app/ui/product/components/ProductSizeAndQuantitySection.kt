package pe.modaviva.app.ui.product.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.domain.model.RecomendacionTalla
import pe.modaviva.app.domain.model.Stock

/** Sección de recomendación de talla IA, selector de tallas y control de cantidad. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductSizeAndQuantitySection(
    codigoPrenda: String,
    prenda: Prenda,
    recomendacionTalla: RecomendacionTalla,
    tallaSeleccionada: String?,
    cantidad: Int,
    maxStock: Int,
    esCargaPendiente: Boolean,
    estaAgotado: Boolean,
    allStocks: Map<String, Stock>,
    onSelectSize: (String) -> Unit,
    onOpenSizeGuide: () -> Unit,
    onDecreaseQty: () -> Unit,
    onIncreaseQty: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SizeRecommendationBanner(recomendacionTalla = recomendacionTalla)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Elige tu talla",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "Guía de tallas",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable(onClick = onOpenSizeGuide),
            )
        }

        val isSizeSoldOut: (String) -> Boolean = { talla ->
            val stockTalla = allStocks["${codigoPrenda}_$talla"]
            (stockTalla != null && stockTalla.total <= 0) || (stockTalla == null && prenda.stockTotal <= 0)
        }

        if (prenda.tallas.size <= 6) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                prenda.tallas.forEach { talla ->
                    SizePill(
                        talla = talla,
                        isSelected = tallaSeleccionada == talla,
                        esAgotada = isSizeSoldOut(talla),
                        onClick = { onSelectSize(talla) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                prenda.tallas.forEach { talla ->
                    SizePill(
                        talla = talla,
                        isSelected = tallaSeleccionada == talla,
                        esAgotada = isSizeSoldOut(talla),
                        onClick = { onSelectSize(talla) },
                        modifier = Modifier.width(60.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Cantidad",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedIconButton(
                    onClick = onDecreaseQty,
                    enabled = !estaAgotado && !esCargaPendiente && tallaSeleccionada != null && cantidad > 1,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Disminuir cantidad",
                        modifier = Modifier.size(16.dp),
                    )
                }

                Text(
                    text = if (estaAgotado || tallaSeleccionada == null) "0" else cantidad.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                OutlinedIconButton(
                    onClick = onIncreaseQty,
                    enabled = !estaAgotado && !esCargaPendiente && tallaSeleccionada != null && cantidad < maxStock,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Aumentar cantidad",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        if (!estaAgotado && !esCargaPendiente && tallaSeleccionada != null && maxStock > 0 && cantidad >= maxStock) {
            Text(
                text = "Solo quedan $maxStock unidades disponibles",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        } else if (estaAgotado && tallaSeleccionada != null) {
            Text(
                text = "Agotado en esta talla",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SizePill(
    talla: String,
    isSelected: Boolean,
    esAgotada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = { if (!esAgotada) onClick() },
        shape = RoundedCornerShape(14.dp),
        color = when {
            isSelected -> MaterialTheme.colorScheme.primary
            esAgotada -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.surfaceContainerLowest
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            },
        ),
        enabled = !esAgotada,
        modifier = modifier.height(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = talla,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    esAgotada -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.onSurface
                },
                textDecoration = if (esAgotada) TextDecoration.LineThrough else TextDecoration.None,
            )
        }
    }
}

@Composable
private fun SizeRecommendationBanner(recomendacionTalla: RecomendacionTalla) {
    when (recomendacionTalla) {
        is RecomendacionTalla.TallaExacta, is RecomendacionTalla.EntreDosTallas -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Straighten,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = when (recomendacionTalla) {
                                is RecomendacionTalla.TallaExacta -> "Tu talla recomendada: ${recomendacionTalla.talla}"
                                is RecomendacionTalla.EntreDosTallas -> "Tu talla recomendada: Entre ${recomendacionTalla.tallaMenor} y ${recomendacionTalla.tallaMayor}"
                                else -> ""
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = "Calculada por IA según tus medidas registradas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        )
                        // HU-15 CA-04: aviso referencial obligatorio.
                        Text(
                            text = "Recomendación aproximada, no garantiza el ajuste exacto",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }

        is RecomendacionTalla.FotoInvalidaParaMedidas -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "No se pudieron calcular tus medidas con exactitud. Registra una nueva foto en el Probador.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        RecomendacionTalla.SinDatosSuficientes -> Unit
    }
}
