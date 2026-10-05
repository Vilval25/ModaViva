package pe.modaviva.app.ui.auth.register

import pe.modaviva.app.domain.model.AuthFormField
import pe.modaviva.app.domain.model.UserProfile

data class RegisterUiState(
    val nombres: String = "",
    val apellidos: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val aceptaTerminos: Boolean = false,
    val touched: Set<AuthFormField> = emptySet(),
    val errors: Map<AuthFormField, String> = emptyMap(),
    val showPassword: Boolean = false,
    val isSubmitting: Boolean = false,
    val isGoogleSubmitting: Boolean = false,
    val globalError: String? = null,
) {
    fun errorFor(field: AuthFormField): String? =
        if (field in touched) errors[field] else null

    val canSubmit: Boolean
        get() = errors.isEmpty() && !isSubmitting && !isGoogleSubmitting && aceptaTerminos
}

sealed interface RegisterResult {
    data class Success(val profile: UserProfile) : RegisterResult
}
