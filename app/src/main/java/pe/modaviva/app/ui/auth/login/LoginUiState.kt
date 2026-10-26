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
    val isGoogleSubmitting: Boolean = false,
    val pendingConsentProfile: UserProfile? = null,
    val isAcceptingTerms: Boolean = false,
    val isSendingReset: Boolean = false,
    val resetSent: Boolean = false,
    val resetError: String? = null,
    val failedAttempts: Int = 0,
    val lockoutUntilMillis: Long = 0L,
    val globalError: String? = null,
) {
    fun errorFor(field: AuthFormField): String? =
        if (field in touched) errors[field] else null

    val isLockedOut: Boolean
        get() = lockoutUntilMillis > System.currentTimeMillis()

    val canSubmit: Boolean
        get() = errors.isEmpty() && !isSubmitting && !isGoogleSubmitting && !isLockedOut
}

sealed interface LoginResult {
    data class Success(val profile: UserProfile) : LoginResult
}
