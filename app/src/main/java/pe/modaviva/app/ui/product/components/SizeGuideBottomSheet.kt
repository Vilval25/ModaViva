package pe.modaviva.app.ui.product.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.modaviva.app.domain.model.MedidasPrenda

private data class ColumnaMedida(
    val titulo: String,
    val ancho: Dp,
    val obtenerValor: (MedidasPrenda) -> Double,
)

private val COLUMNAS_MEDIDAS = listOf(
    ColumnaMedida("Pecho", 80.dp) { it.pecho },
    ColumnaMedida("Cintura", 80.dp) { it.cintura },
    ColumnaMedida("Cadera", 80.dp) { it.cadera },
    ColumnaMedida("Hombro", 80.dp) { it.hombro },
    ColumnaMedida("Largo", 80.dp) { it.largo },
    ColumnaMedida("Manga", 80.dp) { it.manga },
)

/** Hoja inferior con la tabla de medidas por talla de la prenda (solo columnas con datos > 0). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SizeGuideBottomSheet(
    medidas: Map<String, MedidasPrenda>,
    tallasOrdenadas: List<String> = emptyList(),
    onDismiss: () -> Unit,
) {
    val columnasActivas = COLUMNAS_MEDIDAS.filter { col ->
        medidas.values.any { col.obtenerValor(it) > 0.0 }
    }
    val filasOrdenadas = ordenarFilasMedidas(medidas, tallasOrdenadas)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Tabla de medidas",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "Medidas en centímetros (cm)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (filasOrdenadas.isEmpty() || columnasActivas.isEmpty()) {
                Text(
                    text = "Esta prenda aún no cuenta con tabla de medidas detallada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                    ) {
                        Row(modifier = Modifier.padding(vertical = 12.dp)) {
                            Text(
                                text = "Talla",
                                modifier = Modifier.width(70.dp),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            columnasActivas.forEach { columna ->
                                Text(
                                    text = columna.titulo,
                                    modifier = Modifier.width(columna.ancho),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        HorizontalDivider()

                        filasOrdenadas.forEach { (talla, medidasPrenda) ->
                            Row(modifier = Modifier.padding(vertical = 10.dp)) {
                                Text(
                                    text = talla,
                                    modifier = Modifier.width(70.dp),
                                    fontWeight = FontWeight.SemiBold,
                                )
                                columnasActivas.forEach { columna ->
                                    Text(
                                        text = formatearMedidaCm(columna.obtenerValor(medidasPrenda)),
                                        modifier = Modifier.width(columna.ancho),
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

private fun ordenarFilasMedidas(
    medidas: Map<String, MedidasPrenda>,
    tallasOrdenadas: List<String>,
): List<Map.Entry<String, MedidasPrenda>> {
    if (tallasOrdenadas.isEmpty()) return medidas.entries.toList()
    val ordenIndice = tallasOrdenadas.withIndex().associate { it.value to it.index }
    return medidas.entries.sortedBy { ordenIndice[it.key] ?: Int.MAX_VALUE }
}

private fun formatearMedidaCm(valor: Double): String {
    if (valor <= 0.0) return "—"
    return if (valor % 1.0 == 0.0) "${valor.toInt()}" else "%.1f".format(valor)
}

