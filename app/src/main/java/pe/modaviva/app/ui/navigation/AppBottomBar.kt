package pe.modaviva.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Barra de navegación inferior con las cinco pestañas principales. Material 3
 * admite como máximo 5 destinos; por eso Carrito está en la barra superior de
 * Inicio y no aquí.
 *
 * Estilo editorial, a juego con Inicio: mismo fondo que la página con una
 * línea fina arriba, íconos de trazo y etiquetas en mayúsculas pequeñas. La
 * pestaña actual va en el color del texto con una raya debajo (HT-01 CA-04);
 * las demás, en gris.
 */
@Composable
fun AppBottomBar(
    current: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(NavigationBarDefaults.windowInsets),
    ) {
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .selectableGroup(),
        ) {
            TopLevelDestination.entries.forEach { destination ->
                Pestana(
                    destination = destination,
                    selected = destination == current,
                    onClick = { onSelect(destination) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.Pestana(
    destination: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .selectable(selected = selected, onClick = onClick, role = Role.Tab),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            // Siempre el ícono delineado: la pestaña activa se marca con el
            // color y la raya, no con un ícono relleno.
            imageVector = destination.unselectedIcon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = stringResource(destination.label).uppercase(),
            // Tamaño y espaciado justos para que "CATEGORÍAS" quepa en un quinto del ancho.
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.6.sp),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .width(16.dp)
                .height(1.5.dp)
                .background(if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent),
        )
    }
}
