package pe.modaviva.app.ui.auth.login

import pe.modaviva.app.domain.model.AuthFormField
import pe.modaviva.app.domain.model.UserProfile

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val touched: Set<AuthFormField> = emptySet(),
    val errors: Map<AuthFormField, String> = emptyMap(),
    val showPassword: Boolean = false,
    val isSubmitting: Boolean = false,
    val globalError: String? = null,
    val globalMessage: String? = null,
) {
    fun errorFor(field: AuthFormField): String? =
        if (field in touched) errors[field] else null

    val canSubmit: Boolean
        get() = errors.isEmpty() && !isSubmitting
}

sealed interface LoginResult {
    data class Success(val profile: UserProfile) : LoginResult
}
