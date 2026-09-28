package pe.modaviva.app.ui.auth

import pe.modaviva.app.domain.exception.AuthError

/**
 * Traduce el vocabulario de errores del dominio al texto que ve el cliente.
 * Vive en la capa de interfaz porque es una decisión de presentación.
 */
fun AuthError.toUserMessage(): String = when (this) {
    AuthError.INVALID_CREDENTIALS -> "Correo o contraseña incorrectos"
    AuthError.EMAIL_ALREADY_IN_USE -> "Este correo ya está registrado"
    AuthError.WEAK_PASSWORD -> "La contraseña es demasiado débil"
    AuthError.INVALID_EMAIL -> "Ingresa un correo válido"
    AuthError.TOO_MANY_REQUESTS -> "Demasiados intentos. Espera un momento"
    AuthError.NETWORK -> "Sin conexión a internet. Revisa tu red"
    AuthError.PROFILE_NOT_FOUND ->
        "Tu cuenta no tiene un perfil asociado. Contacta al administrador"
    AuthError.DOCUMENT_TAKEN -> "Este documento ya tiene una cuenta"
    AuthError.UNKNOWN -> "Ocurrió un error inesperado. Inténtalo de nuevo"
}
