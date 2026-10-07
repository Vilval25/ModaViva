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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import pe.modaviva.app.ui.cart.CartScreen
import pe.modaviva.app.ui.components.MvAuthPromptBottomSheet
import pe.modaviva.app.ui.home.HomeScreen
import pe.modaviva.app.ui.orders.OrderHistoryScreen
import pe.modaviva.app.ui.placeholder.ComingSoonScreen
import pe.modaviva.app.ui.product.ProductDetailScreen
import pe.modaviva.app.ui.profile.ProfileTab
import pe.modaviva.app.ui.search.SearchScreen
import pe.modaviva.app.ui.session.SessionViewModel
import pe.modaviva.app.ui.stock.StockTestScreen
import pe.modaviva.app.ui.theme.ModaVivaTheme

/**
 * Navegación de la app. Se entra directo a Inicio, sin pedir sesión (HT-01 CA-03).
 * La barra inferior solo se muestra en las pestañas; el resto de pantallas se abre encima de ellas.
 *
 * Las acciones que requieren cuenta (usar el probador, abrir o marcar favoritos, abrir notificaciones,
 * abrir Mis pedidos o pagar) interceptan a los invitados mostrando la hoja modal 'Inicia sesión para continuar'
 * (HU-02 CA-02). Si se descarta el aviso, el usuario sigue navegando como invitado.
 */
@Composable
fun AppNavHost(
    sessionViewModel: SessionViewModel = hiltViewModel(),
) {
    val currentUser by sessionViewModel.currentUser.collectAsStateWithLifecycle()

    var showAuthPrompt by remember { mutableStateOf(false) }
    var authPromptMessage by remember { mutableStateOf("") }

    val defaultPrompt = stringResource(R.string.auth_prompt_body_default)
    val fittingPrompt = stringResource(R.string.auth_prompt_body_fitting)
    val favoritesPrompt = stringResource(R.string.auth_prompt_body_favorites)
    val notificationsPrompt = stringResource(R.string.auth_prompt_body_notifications)
    val ordersPrompt = stringResource(R.string.auth_prompt_body_orders)
    val cartPrompt = stringResource(R.string.auth_prompt_body_cart)

    val requireAuth: (String, () -> Unit) -> Unit = { message, onAuthenticated ->
        if (currentUser == null) {
            authPromptMessage = message
            showAuthPrompt = true
        } else {
            onAuthenticated()
        }
    }

    ModaVivaTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentTab = TopLevelDestination.entries.firstOrNull { tab ->
            backStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                if (currentTab != null) {
                    AppBottomBar(
                        current = currentTab,
                        onSelect = { tab ->
                            when (tab) {
                                TopLevelDestination.FITTING -> {
                                    requireAuth(fittingPrompt) {
                                        navController.navigateToTab(tab)
                                    }
                                }
                                TopLevelDestination.ORDERS -> {
                                    requireAuth(ordersPrompt) {
                                        navController.navigateToTab(tab)
                                    }
                                }
                                else -> navController.navigateToTab(tab)
                            }
                        },
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
                topLevelGraph(
                    navController = navController,
                    requireAuth = requireAuth,
                    notificationsPrompt = notificationsPrompt,
                    favoritesPrompt = favoritesPrompt,
                    cartPrompt = cartPrompt,
                )
                overlayGraph(
                    navController = navController,
                    requireAuth = requireAuth,
                    cartPrompt = cartPrompt,
                )
                authGraph(navController)
            }
        }

        if (showAuthPrompt) {
            MvAuthPromptBottomSheet(
                onDismiss = { showAuthPrompt = false },
                onLoginClick = {
                    showAuthPrompt = false
                    navController.navigate(Routes.LOGIN)
                },
                onRegisterClick = {
                    showAuthPrompt = false
                    navController.navigate(Routes.REGISTER)
                },
                message = authPromptMessage.ifBlank { defaultPrompt },
            )
        }
    }
}

private fun NavGraphBuilder.topLevelGraph(
    navController: NavHostController,
    requireAuth: (String, () -> Unit) -> Unit,
    notificationsPrompt: String,
    favoritesPrompt: String,
    cartPrompt: String,
) {
    composable(TopLevelDestination.HOME.route) {
        HomeScreen(
            onNotificationsClick = {
                requireAuth(notificationsPrompt) {
                    navController.navigate(Routes.NOTIFICATIONS)
                }
            },
            onSearchClick = { navController.navigate(Routes.SEARCH) },
            onFavoritesClick = {
                requireAuth(favoritesPrompt) {
                    navController.navigate(Routes.FAVORITES)
                }
            },
            onCartClick = {
                navController.navigate(Routes.CART)
            },
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
    requireAuth: (String, () -> Unit) -> Unit,
    cartPrompt: String,
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
            onCartClick = { navController.navigate(Routes.CART) },
        )
    }
    composable(Routes.CART) {
        CartScreen(
            onBack = { navController.popBackStack() },
            onGoToHome = {
                navController.popBackStack(TopLevelDestination.HOME.route, inclusive = false)
            },
            onCheckout = {
                // Proceder al pago
            },
        )
    }
    composable("stock-test") {
        StockTestScreen()
    }
}

/**
 * Acceso y registro. Al autenticarse con éxito, vuelve a la pantalla desde donde se solicitó
 * o a Perfil si se abrió desde allí (HU-01 y HU-02).
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
            onLoggedIn = {
                if (!navController.popBackStack()) {
                    backToProfile()
                }
            },
            modifier = Modifier.safeDrawingPadding(),
        )
    }
    composable(Routes.REGISTER) {
        RegisterScreen(
            onRegistered = { success ->
                if (success.profile.emailVerificado) {
                    if (!navController.popBackStack()) {
                        backToProfile()
                    }
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
            onBack = {
                if (!navController.popBackStack()) {
                    backToProfile()
                }
            },
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
