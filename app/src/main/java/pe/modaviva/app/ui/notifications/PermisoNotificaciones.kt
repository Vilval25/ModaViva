package pe.modaviva.app.ui.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * HU-18 CA-06: en Android 13+, antes del permiso del sistema se explica para qué
 * se usan las notificaciones. Se pregunta una sola vez; si el cliente dice
 * "Ahora no" o rechaza el permiso, los avisos igual quedan en la bandeja.
 *
 * @param habilitado true cuando hay sesión y el cliente está en Inicio.
 */
@Composable
fun SolicitudPermisoNotificaciones(habilitado: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var mostrar by remember { mutableStateOf(false) }

    // El resultado no cambia nada en la app: con o sin permiso, la bandeja funciona igual.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(habilitado) {
        mostrar = habilitado && !permisoConcedido(context) && !prefs.getBoolean(KEY_PREGUNTADO, false)
    }

    if (!mostrar) return

    val marcarPreguntado = {
        prefs.edit().putBoolean(KEY_PREGUNTADO, true).apply()
        mostrar = false
    }

    AlertDialog(
        onDismissRequest = marcarPreguntado,
        icon = { Icon(Icons.Outlined.NotificationsActive, contentDescription = null) },
        title = { Text("Activa las notificaciones") },
        text = {
            Text(
                "Te avisaremos cuando tu prueba virtual esté lista y cuando cambie el estado " +
                    "de tus pedidos o de tus solicitudes de cambio o devolución.\n\n" +
                    "Si no las activas, igual verás todos los avisos en Notificaciones.",
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    marcarPreguntado()
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
            ) { Text("Activar") }
        },
        dismissButton = {
            TextButton(onClick = marcarPreguntado) { Text("Ahora no") }
        },
    )
}

private fun permisoConcedido(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private const val PREFS = "modaviva_notificaciones"
private const val KEY_PREGUNTADO = "permiso_preguntado"
