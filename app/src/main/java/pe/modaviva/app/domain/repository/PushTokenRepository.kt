package pe.modaviva.app.domain.repository

/**
 * Token de Firebase Cloud Messaging del dispositivo, guardado en el perfil del
 * cliente para que la Cloud Function `enviarPush` sepa a dónde enviar (HU-18 CA-05).
 */
interface PushTokenRepository {

    /** Guarda el token en el perfil del cliente con sesión (si no hay sesión, no hace nada). */
    fun registrar(token: String)

    /** Quita el token de este dispositivo del perfil. Se llama antes de cerrar sesión. */
    suspend fun desregistrar()
}
