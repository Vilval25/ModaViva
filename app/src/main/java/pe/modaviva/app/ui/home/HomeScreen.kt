package pe.modaviva.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pe.modaviva.app.R
import pe.modaviva.app.ui.components.MvEmptyState

/**
 * Pestaña Inicio. Por ahora solo tiene la barra superior; el catálogo lo
 * agrega HU-04 en el cuerpo de la pantalla.
 */
@Composable
fun HomeScreen(
    onNotificationsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onCartClick: () -> Unit,
    modifier: Modifier = Modifier,
    unreadNotifications: Int = 0,
    cartItemCount: Int = 0,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            HomeTopBar(
                onNotificationsClick = onNotificationsClick,
                onSearchClick = onSearchClick,
                onFavoritesClick = onFavoritesClick,
                onCartClick = onCartClick,
                unreadNotifications = unreadNotifications,
                cartItemCount = cartItemCount,
            )
        },
    ) { padding ->
        MvEmptyState(
            icon = Icons.Outlined.Storefront,
            title = stringResource(R.string.home_catalog_soon_title),
            message = stringResource(R.string.home_catalog_soon_body),
            modifier = Modifier.padding(padding),
        )
    }
}

/**
 * Barra superior de Inicio (HT-01 CA-05), de izquierda a derecha:
 * Notificaciones, buscador, Favoritos y Carrito. El buscador es un acceso a la
 * pantalla de búsqueda, no un campo editable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    onNotificationsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onCartClick: () -> Unit,
    unreadNotifications: Int,
    cartItemCount: Int,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            BadgedIconButton(
                icon = Icons.Outlined.Notifications,
                contentDescription = stringResource(R.string.cd_notifications),
                count = unreadNotifications,
                onClick = onNotificationsClick,
            )
        },
        title = { SearchEntry(onClick = onSearchClick) },
        actions = {
            IconButton(onClick = onFavoritesClick) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(R.string.cd_favorites),
                )
            }
            BadgedIconButton(
                icon = Icons.Outlined.ShoppingCart,
                contentDescription = stringResource(R.string.cd_cart),
                count = cartItemCount,
                onClick = onCartClick,
            )
        },
    )
}

/** Ícono con insignia numérica; la insignia se oculta cuando [count] es 0. */
@Composable
private fun BadgedIconButton(
    icon: ImageVector,
    contentDescription: String,
    count: Int,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (count > 0) {
                    Badge {
                        Text(text = if (count > 99) "99+" else count.toString())
                    }
                }
            },
        ) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    }
}

@Composable
private fun SearchEntry(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics { role = Role.Button },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
            Text(
                text = stringResource(R.string.home_search_hint),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
