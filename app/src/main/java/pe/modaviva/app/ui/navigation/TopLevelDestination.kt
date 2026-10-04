package pe.modaviva.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import pe.modaviva.app.R

/**
 * Pestañas de la barra inferior, en el orden que exige HT-01 CA-04. El ícono
 * relleno marca la pestaña actual y el delineado las demás.
 */
enum class TopLevelDestination(
    val route: String,
    @param:StringRes val label: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME("home", R.string.tab_home, Icons.Filled.Home, Icons.Outlined.Home),
    CATEGORIES(
        "categories",
        R.string.tab_categories,
        Icons.Filled.Category,
        Icons.Outlined.Category,
    ),
    PROFILE("profile", R.string.tab_profile, Icons.Filled.Person, Icons.Outlined.Person),
    FITTING("fitting", R.string.tab_fitting, Icons.Filled.Checkroom, Icons.Outlined.Checkroom),
    ORDERS(
        "orders",
        R.string.tab_orders,
        Icons.AutoMirrored.Filled.ReceiptLong,
        Icons.AutoMirrored.Outlined.ReceiptLong,
    ),
}
