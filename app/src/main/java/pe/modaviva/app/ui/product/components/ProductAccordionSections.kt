package pe.modaviva.app.ui.product.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.domain.model.Stock
import pe.modaviva.app.domain.model.Tienda
import pe.modaviva.app.ui.stock.StockAvailability
import androidx.compose.ui.res.stringResource
import pe.modaviva.app.R
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

/**
 * Secciones desplegables de información de la prenda (Detalles, Material y cuidado,
 * Disponibilidad y envío, Cambios y devoluciones).
 * Inician todas colapsadas por defecto para mantener la vista limpia y evitar redundancia superior.
 */
@Composable
fun ProductAccordionSections(
    prenda: Prenda,
    tallaSeleccionada: String?,
    stock: Stock?,
    tiendas: List<Tienda>,
    envioEstimado: EnvioEstimado? = null,
    modifier: Modifier = Modifier,
) {
    var detailsExpanded by remember { mutableStateOf(false) }
    var fabricExpanded by remember { mutableStateOf(false) }
    var availabilityExpanded by remember { mutableStateOf(false) }
    var returnsExpanded by remember { mutableStateOf(false) }

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
            title = "Disponibilidad y envío",
            isExpanded = availabilityExpanded,
            onToggle = { availabilityExpanded = !availabilityExpanded },
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 14.dp),
            ) {
                if (tallaSeleccionada != null && stock != null && stock.total > 0) {
                    EntregaEstimadaCard(
                        talla = tallaSeleccionada,
                        stock = stock,
                        tiendas = tiendas,
                        envio = envioEstimado,
                    )
                } else {
                    InfoSummaryCard(
                        titulo = "Opciones de entrega (se eligen al pagar)",
                        lineas = listOf(
                            "• Envío a domicilio en Lima Metropolitana (tarifa y plazo según tu distrito en Checkout).",
                            "• Recojo gratis en tienda: disponible hoy o entre 24 y 48 h según disponibilidad en la sede elegida.",
                        ),
                    )
                }

                when {
                    tallaSeleccionada == null -> {
                        Text(
                            text = "Elige una talla arriba para ver el stock disponible por tienda.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    stock != null -> {
                        StockAvailability(
                            stockByStore = stock.porTienda,
                            tiendas = tiendas,
                        )
                    }

                    else -> {
                        Text(
                            text = "Consultando disponibilidad en tiendas…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        AccordionItem(
            title = "Cambios y devoluciones",
            isExpanded = returnsExpanded,
            onToggle = { returnsExpanded = !returnsExpanded },
        ) {
            InfoSummaryCard(
                titulo = "Cómo funcionan los cambios y devoluciones",
                lineas = listOf(
                    "• Plazo de hasta 7 días después de recibir tu pedido (estado Entregado).",
                    "• Ingresa a Mis pedidos → Detalle del pedido, selecciona la prenda, elige cambio o devolución, indica el motivo y adjunta hasta 3 fotos.",
                    "• La prenda debe encontrarse sin uso y con sus etiquetas originales.",
                ),
                modifier = Modifier.padding(bottom = 14.dp),
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun InfoSummaryCard(
    titulo: String,
    lineas: List<String>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            lineas.forEach { linea ->
                Text(
                    text = linea,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
private fun AccordionItem(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    val estado = stringResource(if (isExpanded) R.string.cd_state_expanded else R.string.cd_state_collapsed)
    val accion = stringResource(if (isExpanded) R.string.cd_action_collapse else R.string.cd_action_expand)
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = accion, role = Role.Button, onClick = onToggle)
                .semantics { stateDescription = estado }
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
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            content()
        }
        Spacer(modifier = Modifier.height(0.dp))
    }
}

