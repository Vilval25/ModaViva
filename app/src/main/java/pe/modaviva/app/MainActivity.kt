package pe.modaviva.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import pe.modaviva.app.push.AvisoPush
import pe.modaviva.app.ui.navigation.AppNavHost

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Aviso del push que abrió la app (HU-18 CA-05), pendiente de navegar. */
    private var avisoPush by mutableStateOf<AvisoPush?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        avisoPush = AvisoPush.desdeIntent(intent)
        setContent {
            AppNavHost(
                avisoPush = avisoPush,
                onAvisoPushAtendido = {
                    avisoPush = null
                    AvisoPush.limpiar(intent)
                },
            )
        }
    }

    /** La app ya estaba abierta y se tocó un push (launchMode singleTop). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        avisoPush = AvisoPush.desdeIntent(intent)
    }
}
