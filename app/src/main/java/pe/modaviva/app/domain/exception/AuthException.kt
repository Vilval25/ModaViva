package pe.modaviva.app.domain.exception

/**
 * Vocabulario de fallos de autenticación en términos del dominio.
 *
 * La capa de datos traduce las excepciones propias de Firebase a estos
 * códigos, de modo que la interfaz nunca depende del proveedor de
 * autenticación: cambiar Firebase por otro servicio no tocaría las pantallas.
 */
enum class AuthError {
    INVALID_CREDENTIALS,
    EMAIL_ALREADY_IN_USE,
    WEAK_PASSWORD,
    INVALID_EMAIL,
    TOO_MANY_REQUESTS,
    NETWORK,
    PROFILE_NOT_FOUND,
    DOCUMENT_TAKEN,
    UNKNOWN,
}

class AuthException(
    val error: AuthError,
    message: String? = null,
) : Exception(message)
