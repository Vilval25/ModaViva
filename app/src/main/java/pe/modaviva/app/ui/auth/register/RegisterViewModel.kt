package pe.modaviva.app.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.modaviva.app.domain.model.AuthFormField
import pe.modaviva.app.domain.model.RegisterRequest
import pe.modaviva.app.domain.repository.AuthRepository

import pe.modaviva.app.domain.usecase.ValidateRegisterFormUseCase
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val validateRegisterForm: ValidateRegisterFormUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private var emailCheckJob: Job? = null
    private var documentoCheckJob: Job? = null

    fun onNombresChange(value: String) = updateValue { it.copy(nombres = value) }
    fun onApellidosChange(value: String) = updateValue { it.copy(apellidos = value) }
    fun onDocumentoChange(value: String) = updateValue { it.copy(documento = value) }
    fun onTelefonoChange(value: String) = updateValue { it.copy(telefono = value) }
    fun onEmailChange(value: String) = updateValue { it.copy(email = value) }
    fun onPasswordChange(value: String) = updateValue { it.copy(password = value) }
    fun onConfirmPasswordChange(value: String) =
        updateValue { it.copy(confirmPassword = value) }

    fun onTogglePasswordVisibility() =
        _uiState.update { it.copy(showPassword = !it.showPassword) }

    fun onFieldTouched(field: AuthFormField) {
        _uiState.update { it.copy(touched = it.touched + field) }
        when (field) {
            AuthFormField.EMAIL -> checkEmailUniqueness()
            AuthFormField.DOCUMENTO -> checkDocumentoUniqueness()
            else -> Unit
        }
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
        if (errors.isNotEmpty() || _uiState.value.hasBlockingUniqueness) return

        _uiState.update { it.copy(isSubmitting = true, globalError = null) }
        viewModelScope.launch {
            authRepository.register(current.toRegisterRequest())
                .onSuccess { profile ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    onSuccess(RegisterResult.Success(profile))
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            globalError = error.message
                                ?: "No se pudo completar el registro",
                        )
                    }
                }
        }
    }

    fun onDismissGlobalError() = _uiState.update { it.copy(globalError = null) }

    private fun updateValue(transform: (RegisterUiState) -> RegisterUiState) {
        _uiState.update { state ->
            val updated = transform(state)
            updated.copy(errors = validate(updated))
        }
    }

    private fun checkEmailUniqueness() {
        val email = _uiState.value.email.trim()
        emailCheckJob?.cancel()
        if (validateRegisterForm.isEmailValid(email).not()) {
            _uiState.update { it.copy(isCheckingEmail = false, emailTaken = false) }
            return
        }
        _uiState.update { it.copy(isCheckingEmail = true) }
        emailCheckJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            val taken = authRepository.isEmailTaken(email)
            _uiState.update { state ->
                if (state.email.trim().equals(email, ignoreCase = true)) {
                    state.copy(isCheckingEmail = false, emailTaken = taken)
                } else {
                    state.copy(isCheckingEmail = false)
                }
            }
        }
    }

    private fun checkDocumentoUniqueness() {
        val documento = _uiState.value.documento.trim()
        documentoCheckJob?.cancel()
        if (validateRegisterForm.isDocumentoValid(documento).not()) {
            _uiState.update { it.copy(isCheckingDocumento = false, documentoTaken = false) }
            return
        }
        _uiState.update { it.copy(isCheckingDocumento = true) }
        documentoCheckJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            val taken = authRepository.isDocumentoTaken(documento)
            _uiState.update { state ->
                if (state.documento.trim() == documento) {
                    state.copy(isCheckingDocumento = false, documentoTaken = taken)
                } else {
                    state.copy(isCheckingDocumento = false)
                }
            }
        }
    }

    private fun validate(state: RegisterUiState): Map<AuthFormField, String> =
        validateRegisterForm(state.toRegisterRequest(), state.confirmPassword)

    private fun RegisterUiState.toRegisterRequest() = RegisterRequest(
        nombres = nombres.trim(),
        apellidos = apellidos.trim(),
        documento = documento.trim(),
        telefono = telefono.trim(),
        email = email.trim(),
        password = password,
    )

    private companion object {
        const val DEBOUNCE_MS = 400L
    }
}
