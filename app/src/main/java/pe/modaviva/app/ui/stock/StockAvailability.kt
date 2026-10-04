package pe.modaviva.app.ui.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StockAvailability(
    stockByStore: Map<String, Int>,
    modifier: Modifier = Modifier,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Tienda $storeId",
                    style = MaterialTheme.typography.bodyLarge,
                )

                Text(
                    text = "$quantity disponibles",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}