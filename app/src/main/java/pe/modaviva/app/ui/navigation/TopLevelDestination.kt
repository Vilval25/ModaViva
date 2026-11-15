package pe.modaviva.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector
import pe.modaviva.app.R

/**
 * Pestañas de la barra inferior: Inicio, Buscar (explorar + categorías),
 * Probador (al centro), Pedidos y Perfil. El ícono relleno marca la pestaña
 * actual y el delineado las demás.
 */
enum class TopLevelDestination(
    val route: String,
    @param:StringRes val label: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(
        "home",
        R.string.tab_home,
        Icons.Filled.Home,
        Icons.Outlined.Home,
    ),
    SEARCH(
        "search",
        R.string.tab_search,
        Icons.Filled.Search,
        Icons.Outlined.Search,
    ),
    FITTING(
        "fitting",
        R.string.tab_fitting,
        Icons.Filled.Checkroom,
        Icons.Outlined.Checkroom,
    ),
    ORDERS(
        "orders",
        R.string.tab_orders,
        Icons.AutoMirrored.Filled.ReceiptLong,
        Icons.AutoMirrored.Outlined.ReceiptLong,
    ),
    PROFILE(
        "profile",
        R.string.tab_profile,
        Icons.Filled.Person,
        Icons.Outlined.Person,
    ),
}
