package pe.modaviva.app.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.modaviva.app.domain.exception.AuthException
import pe.modaviva.app.domain.model.AuthFormField
import pe.modaviva.app.domain.model.RegisterRequest
import pe.modaviva.app.domain.repository.AuthRepository
import pe.modaviva.app.domain.repository.SessionRepository
import pe.modaviva.app.domain.usecase.ValidateRegisterFormUseCase
import pe.modaviva.app.ui.auth.toUserMessage
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val validateRegisterForm: ValidateRegisterFormUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNombresChange(value: String) = updateValue { it.copy(nombres = value) }
    fun onApellidosChange(value: String) = updateValue { it.copy(apellidos = value) }
    fun onEmailChange(value: String) = updateValue { it.copy(email = value) }
    fun onPasswordChange(value: String) = updateValue { it.copy(password = value) }
    fun onConfirmPasswordChange(value: String) =
        updateValue { it.copy(confirmPassword = value) }
    fun onTermsChange(value: Boolean) =
        updateValue { it.copy(aceptaTerminos = value) }

    fun onTogglePasswordVisibility() =
        _uiState.update { it.copy(showPassword = !it.showPassword) }

    fun onFieldTouched(field: AuthFormField) {
        _uiState.update { it.copy(touched = it.touched + field) }
    }

    fun onSubmit(onSuccess: (RegisterResult.Success) -> Unit) {
        val current = _uiState.value
        val errors = validate(current)
        _uiState.update {
            it.copy(
                errors = errors,
                touched = AuthFormField.entries.toSet(),
            )
        }
        if (errors.isNotEmpty()) return

        _uiState.update { it.copy(isSubmitting = true, globalError = null) }
        viewModelScope.launch {
            authRepository.register(current.toRegisterRequest())
                .onSuccess { profile ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    onSuccess(RegisterResult.Success(profile))
                }
                .onFailure { error ->
                    val message = (error as? AuthException)
                        ?.error
                        ?.toUserMessage()
                        ?: "No se pudo completar el registro"
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            globalError = message,
                        )
                    }
                }
        }
    }

    fun onGoogleSignIn(idToken: String, onSuccess: (RegisterResult.Success) -> Unit) {
        _uiState.update { it.copy(isGoogleSubmitting = true, globalError = null) }
        viewModelScope.launch {
            authRepository.signInWithGoogle(idToken)
                .onSuccess { profile ->
                    sessionRepository.signIn(profile)
                    _uiState.update { it.copy(isGoogleSubmitting = false) }
                    onSuccess(RegisterResult.Success(profile))
                }
                .onFailure { error ->
                    val message = (error as? AuthException)
                        ?.error
                        ?.toUserMessage()
                        ?: "No se pudo registrar con Google"
                    _uiState.update {
                        it.copy(
                            isGoogleSubmitting = false,
                            globalError = message,
                        )
                    }
                }
        }
    }

    fun onGoogleSignInError(message: String?) {
        _uiState.update {
            it.copy(
                isGoogleSubmitting = false,
                globalError = message ?: "Error al conectar con Google",
            )
        }
    }

    fun onDismissGlobalError() = _uiState.update { it.copy(globalError = null) }

    private fun updateValue(transform: (RegisterUiState) -> RegisterUiState) {
        _uiState.update { state ->
            val updated = transform(state)
            updated.copy(errors = validate(updated))
        }
    }

    private fun validate(state: RegisterUiState): Map<AuthFormField, String> =
        validateRegisterForm(state.toRegisterRequest(), state.confirmPassword)

    private fun RegisterUiState.toRegisterRequest() = RegisterRequest(
        nombres = nombres.trim(),
        apellidos = apellidos.trim(),
        email = email.trim(),
        password = password,
        aceptaTerminos = aceptaTerminos,
    )
}
