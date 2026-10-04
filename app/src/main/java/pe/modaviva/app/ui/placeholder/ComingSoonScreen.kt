package pe.modaviva.app.ui.placeholder

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import pe.modaviva.app.R
import pe.modaviva.app.ui.components.MvEmptyState
import pe.modaviva.app.ui.components.MvScreen

/**
 * Pantalla provisional para los destinos que todavía no tienen historia
 * implementada. Permite que la navegación completa sea recorrible desde ya
 * (HT-01 CA-06); cada historia reemplaza su uso en AppNavHost.
 */
@Composable
fun ComingSoonScreen(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    MvScreen(title = title, modifier = modifier, onBack = onBack) { padding ->
        MvEmptyState(
            icon = icon,
            title = stringResource(R.string.coming_soon_title),
            message = stringResource(R.string.coming_soon_body),
            modifier = Modifier.padding(padding),
        )
    }
}
