package pe.modaviva.app.ui.orders

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import pe.modaviva.app.R
import pe.modaviva.app.ui.components.MvEmptyState
import pe.modaviva.app.ui.components.MvScreen

/** Pestaña Mis pedidos. Muestra el estado vacío hasta que HU-23 cargue los pedidos. */
@Composable
fun OrderHistoryScreen(
    modifier: Modifier = Modifier,
) {
    MvScreen(title = stringResource(R.string.orders_title), modifier = modifier) { padding ->
        MvEmptyState(
            icon = Icons.Outlined.History,
            title = stringResource(R.string.orders_empty_title),
            message = stringResource(R.string.orders_empty_body),
            modifier = Modifier.padding(padding),
        )
    }
}
