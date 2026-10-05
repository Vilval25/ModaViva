package pe.modaviva.app.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pe.modaviva.app.R
import pe.modaviva.app.ui.auth.login.LoginScreen
import pe.modaviva.app.ui.auth.register.RegisterScreen
import pe.modaviva.app.ui.auth.verify.EmailVerifyScreen
import pe.modaviva.app.ui.home.HomeScreen
import pe.modaviva.app.ui.orders.OrderHistoryScreen
import pe.modaviva.app.ui.placeholder.ComingSoonScreen
import pe.modaviva.app.ui.profile.ProfileTab
import pe.modaviva.app.ui.theme.ModaVivaTheme
import pe.modaviva.app.ui.stock.StockTestScreen
import pe.modaviva.app.ui.search.SearchScreen
import pe.modaviva.app.ui.product.ProductDetailScreen

/**
 * Navegación de la app. Se entra directo a Inicio, sin pedir sesión
 * (HT-01 CA-03). La barra inferior solo se muestra en las pestañas; el resto
 * de pantallas se abre encima de ellas.
 */
@Composable
fun AppNavHost() {
    ModaVivaTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentTab = TopLevelDestination.entries.firstOrNull { tab ->
            backStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
        }

        Scaffold(
            // Cada pantalla gestiona sus propios márgenes del sistema; aquí
            // solo se reserva el espacio de la barra inferior.
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                if (currentTab != null) {
                    AppBottomBar(
                        current = currentTab,
                        onSelect = navController::navigateToTab,
                    )
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = TopLevelDestination.HOME.route,
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            ) {
                topLevelGraph(navController)
                overlayGraph(navController)
                authGraph(navController)
            }
        }
    }
}

private fun NavGraphBuilder.topLevelGraph(
    navController: NavHostController,
) {
    composable(TopLevelDestination.HOME.route) {
        HomeScreen(
            onNotificationsClick = { navController.navigate(Routes.NOTIFICATIONS) },
            onSearchClick = { navController.navigate(Routes.SEARCH) },
            onFavoritesClick = { navController.navigate(Routes.FAVORITES) },
            onCartClick = { navController.navigate(Routes.CART) },
            onPrendaClick = { codigo -> navController.navigate(Routes.prenda(codigo)) },
        )
    }
    composable(TopLevelDestination.CATEGORIES.route) {
        ComingSoonScreen(
            title = stringResource(R.string.tab_categories),
            icon = Icons.Outlined.Category,
        )
    }
    composable(TopLevelDestination.PROFILE.route) {
        ProfileTab(
            onLoginClick = { navController.navigate(Routes.LOGIN) },
            onRegisterClick = { navController.navigate(Routes.REGISTER) },
        )
    }
    composable(TopLevelDestination.FITTING.route) {
        ComingSoonScreen(
            title = stringResource(R.string.fitting_title),
            icon = Icons.Outlined.Checkroom,
        )
    }
    composable(TopLevelDestination.ORDERS.route) {
        OrderHistoryScreen()
    }
}

/** Pantallas que se abren desde Inicio, con flecha para volver. */
private fun NavGraphBuilder.overlayGraph(
    navController: NavHostController,
) {
    composable(Routes.SEARCH) {
        SearchScreen(
            onBack = { navController.popBackStack() },
            onPrendaClick = { codigo ->
                navController.navigate(Routes.prenda(codigo))
            },
        )
    }
    composable(Routes.FAVORITES) {
        ComingSoonScreen(
            title = stringResource(R.string.favorites_title),
            icon = Icons.Outlined.FavoriteBorder,
            onBack = { navController.popBackStack() },
        )
    }
    composable(Routes.NOTIFICATIONS) {
        ComingSoonScreen(
            title = stringResource(R.string.notifications_title),
            icon = Icons.Outlined.Notifications,
            onBack = { navController.popBackStack() },
        )
    }
    // Ficha de la prenda: la construye HU-07.
    composable(
        route = Routes.PRENDA,
        arguments = listOf(
            navArgument("codigo") {
                type = NavType.StringType
            },
        ),
    ) { backStackEntry ->

        val codigo = backStackEntry.arguments?.getString("codigo").orEmpty()

        ProductDetailScreen(
            codigo = codigo,
            onBack = { navController.popBackStack() },
        )
    }
    composable(Routes.CART) {
        ComingSoonScreen(
            title = stringResource(R.string.cart_title),
            icon = Icons.Outlined.ShoppingCart,
            onBack = { navController.popBackStack() },
        )
    }
    composable("stock-test") {
        StockTestScreen()
    }

}

/**
 * Acceso y registro, abiertos desde Perfil. Al terminar se vuelve a Perfil,
 * que muestra la sesión. HU-02 cambiará esto para volver a la pantalla desde
 * donde se pidió iniciar sesión.
 */
private fun NavGraphBuilder.authGraph(
    navController: NavHostController,
) {
    val backToProfile: () -> Unit = {
        navController.popBackStack(TopLevelDestination.PROFILE.route, inclusive = false)
    }

    composable(Routes.LOGIN) {
        LoginScreen(
            onRegisterClick = {
                navController.navigate(Routes.REGISTER) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            },
            onBackClick = {
                navController.popBackStack()
            },
            onLoggedIn = { backToProfile() },
            modifier = Modifier.safeDrawingPadding(),
        )
    }
    composable(Routes.REGISTER) {
        RegisterScreen(
            onRegistered = { success ->
                if (success.profile.emailVerificado) {
                    backToProfile()
                } else {
                    val route = Routes.EMAIL_VERIFY.replace(
                        "{email}",
                        Uri.encode(success.profile.email),
                    )
                    navController.navigate(route) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                }
            },
            onLoginClick = {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(Routes.REGISTER) { inclusive = true }
                }
            },
            onBackClick = {
                navController.popBackStack()
            },
            modifier = Modifier.safeDrawingPadding(),
        )
    }
    composable(
        route = Routes.EMAIL_VERIFY,
        arguments = listOf(
            navArgument("email") {
                type = NavType.StringType
                defaultValue = ""
            },
        ),
    ) { backStackEntry ->
        EmailVerifyScreen(
            email = backStackEntry.arguments?.getString("email").orEmpty(),
            onBack = backToProfile,
            modifier = Modifier.safeDrawingPadding(),
        )
    }
}

/**
 * Cambia de pestaña conservando el estado de cada una y sin apilar pestañas:
 * el botón atrás desde cualquier pestaña vuelve a Inicio y luego sale.
 */
private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
