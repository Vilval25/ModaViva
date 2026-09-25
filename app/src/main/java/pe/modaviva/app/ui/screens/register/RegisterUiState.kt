package pe.modaviva.app.ui.screens.register

import pe.modaviva.app.data.model.UserProfile

enum class RegisterField {
    NOMBRES,
    APELLIDOS,
    DOCUMENTO,
    TELEFONO,
    EMAIL,
    PASSWORD,
    CONFIRM_PASSWORD,
}

data class RegisterUiState(
    val nombres: String = "",
    val apellidos: String = "",
    val documento: String = "",
    val telefono: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val touched: Set<RegisterField> = emptySet(),
    val errors: Map<RegisterField, String> = emptyMap(),
    val showPassword: Boolean = false,
    val isSubmitting: Boolean = false,
    val isCheckingEmail: Boolean = false,
    val isCheckingDocumento: Boolean = false,
    val emailTaken: Boolean = false,
    val documentoTaken: Boolean = false,
    val globalError: String? = null,
) {
    fun errorFor(field: RegisterField): String? =
        when {
            field == RegisterField.EMAIL && emailTaken ->
                "Este correo ya está registrado"
            field == RegisterField.DOCUMENTO && documentoTaken ->
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
