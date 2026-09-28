package pe.modaviva.app.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pe.modaviva.app.ui.auth.login.LoginScreen
import pe.modaviva.app.ui.auth.register.RegisterScreen
import pe.modaviva.app.ui.auth.verify.EmailVerifyScreen
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
                    PlaceholderScreen(
                        title = "Historial de pedidos",
                        description = "HU-01: se implementa al integrar Cloud Firestore.",
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    description: String,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = description,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onBack) {
            Text(text = "Volver")
        }
    }
}
