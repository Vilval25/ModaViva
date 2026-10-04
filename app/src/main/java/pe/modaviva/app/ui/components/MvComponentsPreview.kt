package pe.modaviva.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.modaviva.app.ui.theme.ModaVivaTheme

/*
 * Catálogo visual del diseño base para revisarlo en claro y oscuro desde el
 * panel de Preview de Android Studio (HT-01 CA-01 y CA-02).
 */

@Preview(name = "Claro", showBackground = true, widthDp = 360)
@Preview(
    name = "Oscuro",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ComponentsPreview() {
    ModaVivaTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MvPrimaryButton(text = "Agregar al carrito", onClick = {}, Modifier.fillMaxWidth())
                MvPrimaryButton(text = "", onClick = {}, Modifier.fillMaxWidth(), loading = true)
                MvSecondaryButton(text = "Seguir comprando", onClick = {}, Modifier.fillMaxWidth())
                MvDestructiveButton(text = "Eliminar foto", onClick = {}, Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MvChip(label = "S", selected = false, onClick = {})
                    MvChip(label = "M", selected = true, onClick = {})
                    MvChip(label = "L", selected = false, onClick = {}, enabled = false)
                    MvRemovableChip(label = "Blusas", onRemove = {})
                }
                MvTextField(value = "maria@correo", onValueChange = {}, label = "Correo")
                MvTextField(
                    value = "maria@",
                    onValueChange = {},
                    label = "Correo",
                    errorMessage = "Ingresa un correo válido",
                )
                MvOfflineBanner(message = "Sin conexión – datos del 04/10 18:30")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MvGarmentCard(
                        name = "Blusa manga larga de lino",
                        price = "S/ 89.90",
                        onClick = {},
                        modifier = Modifier.width(150.dp),
                        isFavorite = true,
                    )
                    MvGarmentCard(
                        name = "Jean slim tiro alto",
                        price = "S/ 99.90",
                        originalPrice = "S/ 129.90",
                        onClick = {},
                        modifier = Modifier.width(150.dp),
                        isSoldOut = true,
                        isFavorite = false,
                    )
                }
            }
        }
    }
}

@Preview(name = "Estados – claro", showBackground = true, widthDp = 360, heightDp = 640)
@Preview(
    name = "Estados – oscuro",
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun StatesPreview() {
    ModaVivaTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                MvLoadingState(Modifier.height(140.dp))
                MvEmptyState(
                    icon = Icons.Outlined.History,
                    title = "Aún no tienes pedidos",
                    message = "Cuando hagas tu primera compra, la verás aquí.",
                    modifier = Modifier.height(170.dp),
                )
                MvErrorState(onRetry = {}, modifier = Modifier.height(170.dp))
                MvOfflineState(onRetry = null, modifier = Modifier.height(160.dp))
            }
        }
    }
}
