package pe.modaviva.app.navigation

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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pe.modaviva.app.ui.screens.register.RegisterScreen
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
                startDestination = Routes.REGISTER,
            ) {
                composable(Routes.REGISTER) {
                    RegisterScreen(
                        onRegistered = { navController.navigate(Routes.EMAIL_VERIFY) },
                    )
                }
                composable(Routes.LOGIN) {
                    PlaceholderScreen(
                        title = "Iniciar sesión",
                        description = "Pantalla de HU-02 (Vila).",
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.EMAIL_VERIFY) {
                    PlaceholderScreen(
                        title = "Verifica tu correo",
                        description = "HU-01: se implementa al integrar Firebase Auth.",
                        onBack = { navController.navigate(Routes.REGISTER) },
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
