package pe.modaviva.app.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import pe.modaviva.app.domain.repository.PushTokenRepository
import javax.inject.Inject

/**
 * Recibe los avisos push (HU-18 CA-05). La Cloud Function `enviarPush` manda
 * mensajes solo de datos, así que este servicio arma la notificación tanto con
 * la app abierta como en segundo plano.
 */
@AndroidEntryPoint
class ModaVivaMessagingService : FirebaseMessagingService() {

    @Inject lateinit var pushTokens: PushTokenRepository

    override fun onNewToken(token: String) {
        pushTokens.registrar(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val datos = message.data
        val aviso = AvisoPush.desdeDatos(datos) ?: return
        NotificacionesSistema.mostrar(
            context = this,
            aviso = aviso,
            titulo = datos[AvisoPush.CLAVE_TITULO]?.takeIf { it.isNotBlank() } ?: "ModaViva",
            texto = datos[AvisoPush.CLAVE_TEXTO].orEmpty(),
        )
    }
}
