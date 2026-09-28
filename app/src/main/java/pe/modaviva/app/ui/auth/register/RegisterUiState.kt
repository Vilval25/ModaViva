package pe.modaviva.app.ui.auth.register

import pe.modaviva.app.domain.model.AuthFormField
import pe.modaviva.app.domain.model.UserProfile

data class RegisterUiState(
    val nombres: String = "",
    val apellidos: String = "",
    val documento: String = "",
    val telefono: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val touched: Set<AuthFormField> = emptySet(),
    val errors: Map<AuthFormField, String> = emptyMap(),
    val showPassword: Boolean = false,
    val isSubmitting: Boolean = false,
    val isCheckingEmail: Boolean = false,
    val isCheckingDocumento: Boolean = false,
    val emailTaken: Boolean = false,
    val documentoTaken: Boolean = false,
    val globalError: String? = null,
) {
    fun errorFor(field: AuthFormField): String? =
        when {
            field == AuthFormField.EMAIL && emailTaken ->
                "Este correo ya está registrado"
            field == AuthFormField.DOCUMENTO && documentoTaken ->
                "Este documento ya tiene una cuenta"
            field in touched -> errors[field]
            else -> null
        }

    val hasBlockingUniqueness: Boolean
        get() = emailTaken || documentoTaken

    val canSubmit: Boolean
        get() = errors.isEmpty() && !isSubmitting && !isCheckingEmail &&
            !isCheckingDocumento && !hasBlockingUniqueness
}

sealed interface RegisterResult {
    data class Success(val profile: UserProfile) : RegisterResult
}
