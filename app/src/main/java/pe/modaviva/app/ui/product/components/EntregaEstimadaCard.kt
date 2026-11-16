package pe.modaviva.app.ui.product.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.modaviva.app.domain.model.Stock
import pe.modaviva.app.domain.model.Tienda

/** Plazo de envío a domicilio ya calculado para el cliente (HU-07 CA-05). */
data class EnvioEstimado(
    val titulo: String,
    val detalle: String,
)

/**
 * HU-07 CA-05 y HU-05 CA-04 — Al elegir una talla disponible se ve el plazo estimado de entrega y
 * las tiendas con stock para recojo.
 */
@Composable
fun EntregaEstimadaCard(
    talla: String,
    stock: Stock,
    tiendas: List<Tienda>,
    envio: EnvioEstimado?,
    modifier: Modifier = Modifier,
) {
    val conStock = tiendas.filter { (stock.porTienda[it.id] ?: 0) > 0 }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Entrega estimada para la talla $talla",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            envio?.let { Linea(Icons.Outlined.LocalShipping, it.titulo, it.detalle) }
            Linea(
                icono = Icons.Outlined.Storefront,
                titulo = if (conStock.isEmpty()) {
                    "Recojo en tienda: sin stock en tiendas"
                } else {
                    "Recojo gratis en tienda: listo en 24 a 48 h"
                },
                detalle = if (conStock.isEmpty()) {
                    "Esta talla solo está disponible para envío desde el almacén."
                } else {
                    "Disponible en ${conStock.size} de ${tiendas.size} tiendas: " +
                        conStock.joinToString { it.nombre }
                },
            )
        }
    }
}

@Composable
private fun Linea(icono: ImageVector, titulo: String, detalle: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(titulo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
