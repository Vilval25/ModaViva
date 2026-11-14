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
import androidx.compose.ui.unit.dp
import pe.modaviva.app.domain.model.MedidasPrenda

/** Hoja inferior con la tabla de medidas por talla de la prenda. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SizeGuideBottomSheet(
    medidas: Map<String, MedidasPrenda>,
    onDismiss: () -> Unit,
) {
    val columnas = listOf(
        "Talla" to 70.dp,
        "Pecho" to 80.dp,
        "Cintura" to 80.dp,
        "Cadera" to 80.dp,
        "Hombro" to 80.dp,
        "Largo" to 80.dp,
        "Manga" to 80.dp,
    )

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
                        columnas.forEach { (titulo, ancho) ->
                            Text(
                                text = titulo,
                                modifier = Modifier.width(ancho),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    HorizontalDivider()

                    medidas.forEach { (talla, medidasPrenda) ->
                        val valores = listOf(
                            talla,
                            "${medidasPrenda.pecho}",
                            "${medidasPrenda.cintura}",
                            "${medidasPrenda.cadera}",
                            "${medidasPrenda.hombro}",
                            "${medidasPrenda.largo}",
                            "${medidasPrenda.manga}",
                        )
                        Row(modifier = Modifier.padding(vertical = 10.dp)) {
                            valores.forEachIndexed { index, valor ->
                                Text(
                                    text = valor,
                                    modifier = Modifier.width(columnas[index].second),
                                    fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal,
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

