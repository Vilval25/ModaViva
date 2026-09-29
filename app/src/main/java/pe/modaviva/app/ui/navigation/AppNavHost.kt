package pe.modaviva.app.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pe.modaviva.app.ui.auth.login.LoginScreen
import pe.modaviva.app.ui.auth.register.RegisterScreen
import pe.modaviva.app.ui.auth.verify.EmailVerifyScreen
import pe.modaviva.app.ui.orders.OrderHistoryScreen
import pe.modaviva.app.ui.session.SessionScreen
import pe.modaviva.app.ui.theme.ModaVivaTheme

@Composable
fun AppNavHost() {
    ModaVivaTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = Routes.LOGIN,
            ) {
                composable(Routes.LOGIN) {
                    LoginScreen(
                        onRegisterClick = { navController.navigate(Routes.REGISTER) },
                        onLoggedIn = { navController.navigate(Routes.SESSION) },
                    )
                }
                composable(Routes.SESSION) {
                    SessionScreen(
                        onSignOut = {
                            navController.popBackStack(Routes.LOGIN, inclusive = false)
                        },
                        onOrdersClick = { navController.navigate(Routes.ORDER_HISTORY) },
                    )
                }
                composable(Routes.REGISTER) {
                    RegisterScreen(
                        onRegistered = { success ->
                            val route = Routes.EMAIL_VERIFY.replace(
                                "{email}",
                                Uri.encode(success.profile.email),
                            )
                            navController.navigate(route)
                        },
                        onLoginClick = { navController.popBackStack() },
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
                            navController.popBackStack(Routes.LOGIN, inclusive = false)
                        },
                    )
                }
                composable(Routes.ORDER_HISTORY) {
                    OrderHistoryScreen(
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
