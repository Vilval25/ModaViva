package pe.modaviva.app.ui.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.modaviva.app.domain.model.Tienda

@Composable
fun StockAvailability(
    stockByStore: Map<String, Int>,
    modifier: Modifier = Modifier,
    tiendas: List<Tienda> = emptyList(),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Disponibilidad para recojo",
            style = MaterialTheme.typography.titleMedium,
        )

        stockByStore.forEach { (storeId, quantity) ->
            val tienda = tiendas.firstOrNull { it.id == storeId }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = tienda?.nombre?.ifBlank { null } ?: "Tienda $storeId",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )

                        Text(
                            text = if (quantity > 0) "$quantity disponible${if (quantity > 1) "s" else ""}" else "Sin stock",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (quantity > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    if (tienda != null) {
                        if (tienda.direccion.isNotBlank()) {
                            Text(
                                text = if (tienda.distrito.isNotBlank()) "${tienda.direccion}, ${tienda.distrito}" else tienda.direccion,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (tienda.horario.isNotBlank()) {
                            Text(
                                text = "Horario: ${tienda.horario}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (tienda.telefono.isNotBlank()) {
                            Text(
                                text = "Tel: ${tienda.telefono}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
