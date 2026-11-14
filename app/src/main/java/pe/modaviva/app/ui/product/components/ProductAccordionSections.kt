package pe.modaviva.app.ui.product.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import pe.modaviva.app.domain.model.DestinoEntrega
import pe.modaviva.app.domain.model.EstimacionEntrega
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.domain.model.Stock
import pe.modaviva.app.domain.model.Tienda
import pe.modaviva.app.domain.model.TipoDestino
import pe.modaviva.app.domain.usecase.ResultadoTiendaCercana
import pe.modaviva.app.ui.stock.StockAvailability

/**
 * Secciones desplegables de información de la prenda (Detalles, Material y cuidado, Envío y devoluciones).
 * Inician todas colapsadas por defecto para mantener la vista limpia y evitar redundancia superior.
 */
@Composable
fun ProductAccordionSections(
    prenda: Prenda,
    destinoEntrega: DestinoEntrega?,
    estimacionEntrega: EstimacionEntrega?,
    resultadoTiendaCercana: ResultadoTiendaCercana,
    stock: Stock?,
    tiendas: List<Tienda>,
    onOpenDestinationSelector: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var detailsExpanded by remember { mutableStateOf(false) }
    var fabricExpanded by remember { mutableStateOf(false) }
    var deliveryExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        AccordionItem(
            title = "Detalles",
            isExpanded = detailsExpanded,
            onToggle = { detailsExpanded = !detailsExpanded },
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 14.dp),
            ) {
                if (prenda.descripcion.isNotBlank()) {
                    Text(
                        text = prenda.descripcion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                DetailMetaRow(
                    label = "Categoría",
                    value = "${prenda.categoria} / ${prenda.subcategoria}",
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Color:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val swatchColor = remember(prenda.color.hex) {
                        runCatching { Color(prenda.color.hex.toColorInt()) }.getOrNull()
                    }
                    if (swatchColor != null) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        )
                    }
                    Text(
                        text = prenda.color.nombre,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                DetailMetaRow(
                    label = "Código de producto",
                    value = prenda.codigo,
                )
            }
        }

        AccordionItem(
            title = "Material y cuidado",
            isExpanded = fabricExpanded,
            onToggle = { fabricExpanded = !fabricExpanded },
        ) {
            Text(
                text = "Confección de alta calidad. Lavado en seco recomendado. Planchar a baja temperatura con paño protector.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 14.dp),
            )
        }

        AccordionItem(
            title = "Envío y devoluciones",
            isExpanded = deliveryExpanded,
            onToggle = { deliveryExpanded = !deliveryExpanded },
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 14.dp),
            ) {
                DeliveryDestinationCard(
                    destinoEntrega = destinoEntrega,
                    onClick = onOpenDestinationSelector,
                )

                estimacionEntrega?.let { DeliveryEstimateCard(estimacion = it) }

                resultadoTiendaCercana.tiendaRecomendada?.let { recomendada ->
                    NearestStoreCard(
                        nombre = recomendada.tienda.nombre,
                        direccion = recomendada.tienda.direccion,
                        distrito = recomendada.tienda.distrito,
                        distanciaKm = recomendada.distanciaKm,
                    )
                }

                stock?.let {
                    StockAvailability(
                        stockByStore = it.porTienda,
                        tiendas = tiendas,
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun DetailMetaRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DeliveryDestinationCard(
    destinoEntrega: DestinoEntrega?,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = "Destino de entrega", style = MaterialTheme.typography.titleSmall)
            val completo = destinoEntrega != null && destinoEntrega.estaCompleto
            val textoDestino = if (completo) {
                val tipoStr = when (destinoEntrega.tipo) {
                    TipoDestino.DIRECCION_REGISTRADA -> "Dirección registrada"
                    TipoDestino.UBICACION_ACTUAL_GPS -> "Ubicación GPS"
                    TipoDestino.OTRA_DIRECCION -> "Ingreso manual"
                }
                val dir = if (destinoEntrega.direccion.isNotBlank()) "${destinoEntrega.direccion}, " else ""
                "$tipoStr: $dir${destinoEntrega.distrito}"
            } else {
                "Selecciona tu distrito o dirección de entrega"
            }
            Text(
                text = textoDestino,
                style = MaterialTheme.typography.bodyMedium,
                color = if (completo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DeliveryEstimateCard(estimacion: EstimacionEntrega) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (estimacion.tieneCobertura && estimacion.tarifaRegistrada) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            },
        ),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = "Estimación de entrega", style = MaterialTheme.typography.titleSmall)
            when {
                !estimacion.tarifaRegistrada -> {
                    Text(
                        text = estimacion.mensajeInformativo ?: "Sin tarifa registrada para este distrito",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                !estimacion.tieneCobertura -> {
                    Text(
                        text = estimacion.mensajeInformativo ?: "Sin cobertura de entrega a domicilio",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                else -> {
                    Text(
                        text = "Costo de envío: S/ ${"%.2f".format(estimacion.costoEnvio)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Plazo estimado: ${estimacion.rangoDiasTexto}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun NearestStoreCard(
    nombre: String,
    direccion: String,
    distrito: String,
    distanciaKm: Double?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Tienda más cercana con stock",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = nombre,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (direccion.isNotBlank()) {
                Text(
                    text = "$direccion, $distrito",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            distanciaKm?.let { km ->
                Text(
                    text = "Distancia: ${"%.1f".format(km)} km",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun AccordionItem(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.Remove else Icons.Default.Add,
                contentDescription = if (isExpanded) "Colapsar $title" else "Expandir $title",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            content()
        }
    }
}

