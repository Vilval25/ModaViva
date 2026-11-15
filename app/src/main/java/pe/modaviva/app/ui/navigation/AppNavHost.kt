package pe.modaviva.app.ui.navigation

import pe.modaviva.app.ui.payments.PaymentMethodsScreen
import pe.modaviva.app.push.AvisoPush
import pe.modaviva.app.ui.notifications.NotificationsViewModel
import pe.modaviva.app.ui.notifications.SolicitudPermisoNotificaciones
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import pe.modaviva.app.R
import pe.modaviva.app.ui.addresses.AddressesScreen
import pe.modaviva.app.ui.analytics.CustomerAnalyticsScreen
import pe.modaviva.app.ui.auth.login.LoginScreen
import pe.modaviva.app.ui.auth.register.RegisterScreen
import pe.modaviva.app.ui.auth.verify.EmailVerifyScreen
import pe.modaviva.app.ui.cart.CartScreen
import pe.modaviva.app.ui.chat.ChatAssistantScreen
import pe.modaviva.app.ui.checkout.confirmed.OrderConfirmedScreen
import pe.modaviva.app.ui.checkout.delivery.CheckoutDeliveryScreen
import pe.modaviva.app.ui.checkout.payment.CheckoutPaymentScreen
import pe.modaviva.app.ui.checkout.summary.CheckoutSummaryScreen
import pe.modaviva.app.ui.components.MvAuthPromptBottomSheet
import pe.modaviva.app.ui.favorites.Favoritos
import pe.modaviva.app.ui.favorites.FavoritesScreen
import pe.modaviva.app.ui.favorites.LocalFavoritos
import pe.modaviva.app.ui.favorites.MarcarFavoritoViewModel
import pe.modaviva.app.ui.fitting.FittingTab
import pe.modaviva.app.ui.home.HomeScreen
import pe.modaviva.app.ui.notifications.NotificationsScreen
import pe.modaviva.app.ui.fitting.deletion.ConstanciaEliminacionScreen
import pe.modaviva.app.ui.components.MvScreen
import pe.modaviva.app.domain.model.ConstanciaEliminacionFoto
import pe.modaviva.app.domain.model.TipoNotificacion
import java.time.Instant
import pe.modaviva.app.ui.orders.detail.OrderDetailScreen
import pe.modaviva.app.ui.orders.history.OrderHistoryScreen
import pe.modaviva.app.ui.product.ProductDetailScreen
import pe.modaviva.app.ui.profile.ProfileTab
import pe.modaviva.app.ui.search.SearchScreen
import pe.modaviva.app.ui.search.SearchViewModel
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
    avisoPush: AvisoPush? = null,
    onAvisoPushAtendido: () -> Unit = {},
    sessionViewModel: SessionViewModel = hiltViewModel(),
    marcarFavorito: MarcarFavoritoViewModel = hiltViewModel(),
    notificaciones: NotificationsViewModel = hiltViewModel(),
) {
    val currentUser by sessionViewModel.currentUser.collectAsStateWithLifecycle()
    val codigosFavoritos by marcarFavorito.codigos.collectAsStateWithLifecycle()

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

    // Corazón de tarjetas y fichas (HU-08). Un invitado ve el aviso de
    // iniciar sesión, y la prenda se marca si entra desde ahí (CA-08).
    val favoritos = remember(codigosFavoritos, currentUser) {
        Favoritos(codigosFavoritos) { codigo ->
            if (currentUser == null) {
                marcarFavorito.marcarAlIniciarSesion(codigo)
                authPromptMessage = favoritesPrompt
                showAuthPrompt = true
            } else {
                marcarFavorito.alternar(codigo)
            }
        }
    }

    ModaVivaTheme {
        val navController = rememberNavController()

        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentTab = TopLevelDestination.entries.firstOrNull { tab ->
            backStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
        }

        CompositionLocalProvider(LocalFavoritos provides favoritos) {
            // Franja superior fija que respeta la barra de estado y el recorte de cámara.
            val topSafeInsets = WindowInsets.statusBars
                .union(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

            Scaffold(
                contentWindowInsets = WindowInsets(0),
                topBar = {
                    MvStatusBarSafeBand(topSafeInsets = topSafeInsets)
                },
                bottomBar = {
                    if (currentTab != null) {
                        AppBottomBar(
                            current = currentTab,
                            onSelect = { tab ->
                                when (tab) {
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
                        .consumeWindowInsets(topSafeInsets)
                        .consumeWindowInsets(innerPadding),
                ) {
                    topLevelGraph(
                        navController = navController,
                        requireAuth = requireAuth,
                        notificationsPrompt = notificationsPrompt,
                        favoritesPrompt = favoritesPrompt,
                        cartPrompt = cartPrompt,
                        defaultPrompt = defaultPrompt,
                    )
                    overlayGraph(
                        navController = navController,
                        requireAuth = requireAuth,
                        cartPrompt = cartPrompt,
                        defaultPrompt = defaultPrompt,
                    )
                    authGraph(navController)
                }
            }
        }

        // HU-18 CA-04/CA-05: al tocar un push se abre la pantalla del aviso y se marca como leído.
        LaunchedEffect(avisoPush) {
            val aviso = avisoPush ?: return@LaunchedEffect
            if (currentUser != null) {
                notificaciones.marcarLeidaPorId(aviso.avisoId)
                if (!navController.abrirDestinoDeAviso(aviso.tipo, aviso.refId)) {
                    navController.navigate(Routes.NOTIFICATIONS)
                }
            }
            onAvisoPushAtendido()
        }

        // HU-18 CA-06: explicación previa al permiso de notificaciones (Android 13+).
        SolicitudPermisoNotificaciones(
            habilitado = currentUser != null && currentTab == TopLevelDestination.HOME,
        )

        if (showAuthPrompt) {
            MvAuthPromptBottomSheet(
                onDismiss = {
                    showAuthPrompt = false
                    marcarFavorito.olvidarPendiente()
                },
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
    defaultPrompt: String,
) {
    composable(TopLevelDestination.HOME.route) {
        HomeScreen(
            onNotificationsClick = {
                requireAuth(notificationsPrompt) {
                    navController.navigate(Routes.NOTIFICATIONS)
                }
            },
            onFavoritesClick = {
                requireAuth(favoritesPrompt) {
                    navController.navigate(Routes.FAVORITES)
                }
            },
            onCartClick = {
                navController.navigate(Routes.CART)
            },
            onPrendaClick = { codigo -> navController.navigate(Routes.prenda(codigo)) },
            // La portada abre Buscar filtrada por marca o categoría; Mujer/Hombre/Niños
            // abren Buscar filtrada por ese público.
            onPortadaClick = { portada ->
                val marca = portada.marca
                if (marca != null) {
                    navController.abrirBusqueda(FILTRO_MARCA, marca)
                } else {
                    navController.abrirBusqueda(FILTRO_CATEGORIA, portada.categoria)
                }
            },
            onAudienciaClick = { genero ->
                navController.abrirBusqueda(FILTRO_GENERO, genero)
            },
            onChatClick = {
                requireAuth(defaultPrompt) {
                    navController.navigate(Routes.chat())
                }
            },
        )
    }
    composable(TopLevelDestination.SEARCH.route) { entry ->
        val viewModel: SearchViewModel = hiltViewModel()
        AplicarFiltroInicial(entry, viewModel)
        SearchScreen(
            onPrendaClick = { codigo ->
                navController.navigate(Routes.prenda(codigo))
            },
            viewModel = viewModel,
        )
    }
    composable(TopLevelDestination.PROFILE.route) {
        ProfileTab(
            onLoginClick = { navController.navigate(Routes.LOGIN) },
            onRegisterClick = { navController.navigate(Routes.REGISTER) },
            onNavigateToFitting = { navController.navigate(TopLevelDestination.FITTING.route) },
            onNavigateToAddresses = { navController.navigate(Routes.ADDRESSES) },
            onNavigateToActivity = { navController.navigate(Routes.MY_ACTIVITY) },
            onNavigateToPaymentMethods = { navController.navigate(Routes.PAYMENT_METHODS) },
        )
    }
    composable(Routes.PAYMENT_METHODS) {
        PaymentMethodsScreen(onBack = { navController.popBackStack() })
    }
    composable(TopLevelDestination.FITTING.route) {
        FittingTab(
            onLoginClick = { navController.navigate(Routes.LOGIN) },
            onRegisterClick = { navController.navigate(Routes.REGISTER) },
            onExploreClick = {
                navController.popBackStack(TopLevelDestination.HOME.route, inclusive = false)
            },
        )
    }
    composable(TopLevelDestination.ORDERS.route) {
        OrderHistoryScreen(
            onOrderClick = { pedidoId ->
                navController.navigate(Routes.orderDetail(pedidoId))
            },
            onGoToHome = {
                navController.popBackStack(TopLevelDestination.HOME.route, inclusive = false)
            },
        )
    }
}

/** Pantallas que se abren desde Inicio, con flecha para volver. */
private fun NavGraphBuilder.overlayGraph(
    navController: NavHostController,
    requireAuth: (String, () -> Unit) -> Unit,
    cartPrompt: String,
    defaultPrompt: String,
) {
    composable(Routes.FAVORITES) {
        FavoritesScreen(
            onBack = { navController.popBackStack() },
            onPrendaClick = { codigo -> navController.navigate(Routes.prenda(codigo)) },
            onGoToHome = {
                navController.popBackStack(TopLevelDestination.HOME.route, inclusive = false)
            },
        )
    }
    composable(Routes.ADDRESSES) {
        AddressesScreen(
            onBack = { navController.popBackStack() },
        )
    }
    composable(Routes.MY_ACTIVITY) {
        CustomerAnalyticsScreen(
            onBack = { navController.popBackStack() },
            onExploreClick = {
                navController.popBackStack(TopLevelDestination.HOME.route, inclusive = false)
            },
        )
    }
    composable(Routes.NOTIFICATIONS) {
        NotificationsScreen(
            onBack = { navController.popBackStack() },
            // HU-18 CA-04: cada aviso abre su pantalla relacionada.
            onAvisoClick = { aviso -> navController.abrirDestinoDeAviso(aviso.tipo, aviso.refId) },
        )
    }
    composable(
        route = Routes.CONSTANCIA_FOTO,
        arguments = listOf(navArgument("eliminadaEnMillis") { type = NavType.LongType }),
    ) { entry ->
        val millis = entry.arguments?.getLong("eliminadaEnMillis") ?: 0L
        MvScreen(
            title = "Constancia de eliminación",
            onBack = { navController.popBackStack() },
        ) { padding ->
            ConstanciaEliminacionScreen(
                constancia = ConstanciaEliminacionFoto(eliminadaEn = Instant.ofEpochMilli(millis)),
                onNuevaFotoClick = { navController.navigateToTab(TopLevelDestination.FITTING) },
                onCatalogoClick = {
                    navController.popBackStack(TopLevelDestination.HOME.route, inclusive = false)
                },
                modifier = Modifier.padding(padding),
            )
        }
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
            onFittingClick = { navController.navigate(TopLevelDestination.FITTING.route) },
            onChatClick = { prendaCodigo ->
                requireAuth(defaultPrompt) {
                    navController.navigate(Routes.chat(prendaCodigo))
                }
            },
        )
    }
    composable(
        route = Routes.CHAT,
        arguments = listOf(
            navArgument("prendaCodigo") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
    ) { backStackEntry ->
        val prendaCodigo = backStackEntry.arguments?.getString("prendaCodigo")
        ChatAssistantScreen(
            onBack = { navController.popBackStack() },
            onPrendaClick = { codigo -> navController.navigate(Routes.prenda(codigo)) },
            prendaCodigoContexto = prendaCodigo,
        )
    }
    composable(Routes.CART) {
        CartScreen(
            onBack = { navController.popBackStack() },
            onGoToHome = {
                navController.popBackStack(TopLevelDestination.HOME.route, inclusive = false)
            },
            onCheckout = {
                requireAuth(cartPrompt) {
                    navController.navigate(Routes.CHECKOUT_SUMMARY)
                }
            },
        )
    }
    composable(Routes.CHECKOUT_SUMMARY) {
        CheckoutSummaryScreen(
            onBack = { navController.popBackStack() },
            onContinueToDelivery = {
                navController.navigate(Routes.CHECKOUT_DELIVERY)
            },
        )
    }
    composable(Routes.CHECKOUT_DELIVERY) {
        CheckoutDeliveryScreen(
            onBack = { navController.popBackStack() },
            onContinueToPayment = {
                navController.navigate(Routes.CHECKOUT_PAYMENT)
            },
        )
    }
    composable(Routes.CHECKOUT_PAYMENT) {
        CheckoutPaymentScreen(
            onBack = { navController.popBackStack() },
            onPaymentSuccess = { pedidoId ->
                navController.navigate(Routes.orderConfirmed(pedidoId)) {
                    popUpTo(Routes.CHECKOUT_SUMMARY) { inclusive = true }
                }
            },
        )
    }
    composable(
        route = Routes.ORDER_CONFIRMED,
        arguments = listOf(
            navArgument("pedidoId") {
                type = NavType.StringType
            },
        ),
    ) { backStackEntry ->
        val pedidoId = backStackEntry.arguments?.getString("pedidoId").orEmpty()
        OrderConfirmedScreen(
            pedidoId = pedidoId,
            onGoToHome = {
                navController.popBackStack(TopLevelDestination.HOME.route, inclusive = false)
            },
            onGoToOrders = {
                navController.navigate(TopLevelDestination.ORDERS.route) {
                    popUpTo(TopLevelDestination.HOME.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        )
    }
    composable(
        route = Routes.ORDER_DETAIL,
        arguments = listOf(
            navArgument("pedidoId") {
                type = NavType.StringType
            },
        ),
    ) {
        OrderDetailScreen(
            onBack = { navController.popBackStack() },
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
 * Filtro con el que otra pantalla abre Categorías o la búsqueda (p. ej. Inicio
 * abre Categorías en "Mujer" o la búsqueda en una marca). Viaja en el
 * savedStateHandle de la pantalla de destino; se aplica una vez y se borra,
 * así que no pisa lo que el usuario elija después.
 */
private const val FILTRO_GENERO = "filtroGenero"
private const val FILTRO_MARCA = "filtroMarca"
private const val FILTRO_CATEGORIA = "filtroCategoria"

@Composable
private fun AplicarFiltroInicial(entry: NavBackStackEntry, viewModel: SearchViewModel) {
    val estado = entry.savedStateHandle
    LaunchedEffect(estado, viewModel) {
        val filtros = mapOf<String, (String) -> Unit>(
            FILTRO_GENERO to viewModel::seleccionarGenero,
            FILTRO_MARCA to viewModel::seleccionarMarca,
            FILTRO_CATEGORIA to viewModel::seleccionarCategoria,
        )
        filtros.forEach { (clave, aplicar) ->
            launch {
                estado.getStateFlow<String?>(clave, null).filterNotNull().collect { valor ->
                    aplicar(valor)
                    estado[clave] = null
                }
            }
        }
    }
}

/** Abre la pestaña Buscar con un filtro ya aplicado (ver AplicarFiltroInicial). */
private fun NavHostController.abrirBusqueda(filtro: String, valor: String) {
    navigateToTab(TopLevelDestination.SEARCH)
    getBackStackEntry(TopLevelDestination.SEARCH.route).savedStateHandle[filtro] = valor
}

/**
 * Cambia de pestaña conservando el estado de cada una y sin apilar pestañas:
 * el botón atrás desde cualquier pestaña vuelve a Inicio y luego sale.
 */
/** HU-18 CA-04: pantalla que abre cada tipo de aviso. */
private fun NavHostController.abrirDestinoDeAviso(tipo: TipoNotificacion, refId: String?): Boolean {
    val ref = refId?.takeIf { it.isNotBlank() } ?: return false
    val ruta = when (tipo) {
        TipoNotificacion.PRUEBA_VIRTUAL -> Routes.prenda(ref)
        TipoNotificacion.PEDIDO, TipoNotificacion.SOLICITUD -> Routes.orderDetail(ref)
        TipoNotificacion.ELIMINACION_FOTO -> ref.toLongOrNull()?.let { Routes.constanciaFoto(it) }
        TipoNotificacion.GENERAL -> null
    } ?: return false
    navigate(ruta)
    return true
}

private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Franja superior fija con la altura de la barra de estado y el recorte de
 * cámara (`displayCutout`), evitando que los encabezados toquen la cámara.
 */
@Composable
private fun MvStatusBarSafeBand(
    topSafeInsets: WindowInsets,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(topSafeInsets)
                    .height(4.dp),
            )
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            )
        }
    }
}
