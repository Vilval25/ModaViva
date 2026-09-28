package pe.modaviva.app.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.modaviva.app.domain.model.AuthFormField
import pe.modaviva.app.domain.exception.AuthException
import pe.modaviva.app.domain.repository.AuthRepository
import pe.modaviva.app.domain.repository.SessionRepository
import pe.modaviva.app.domain.usecase.ValidateLoginFormUseCase
import pe.modaviva.app.ui.auth.toUserMessage
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val validateLoginForm: ValidateLoginFormUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = updateValue { it.copy(email = value) }
    fun onPasswordChange(value: String) = updateValue { it.copy(password = value) }

    fun onTogglePasswordVisibility() =
        _uiState.update { it.copy(showPassword = !it.showPassword) }

    fun onFieldTouched(field: AuthFormField) {
        _uiState.update { it.copy(touched = it.touched + field) }
    }

    fun onSubmit(onSuccess: (LoginResult.Success) -> Unit) {
        val current = _uiState.value
        val errors = validate(current)
        _uiState.update {
            it.copy(
                errors = errors,
                touched = AuthFormField.entries.toSet(),
                globalError = null,
                globalMessage = null,
            )
        }
        if (errors.isNotEmpty()) return

        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            authRepository.login(current.email, current.password)
                .onSuccess { profile ->
                    sessionRepository.signIn(profile)
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            password = "",
                            globalMessage = "Sesión iniciada: ${profile.nombres} " +
                                profile.apellidos,
                        )
                    }
                    onSuccess(LoginResult.Success(profile))
                }
                .onFailure { error ->
                    val message = (error as? AuthException)
                        ?.error
                        ?.toUserMessage()
                        ?: "No se pudo iniciar sesión"
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            globalError = message,
                        )
                    }
                }
        }
    }

    fun onDismissMessage() =
        _uiState.update { it.copy(globalError = null, globalMessage = null) }

    private fun updateValue(transform: (LoginUiState) -> LoginUiState) {
        _uiState.update { state ->
            val updated = transform(state)
            updated.copy(errors = validate(updated))
        }
    }

    private fun validate(state: LoginUiState): Map<AuthFormField, String> =
        validateLoginForm(state.email, state.password)
}
