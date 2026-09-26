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
import pe.modaviva.app.domain.model.RegisterRequest
import pe.modaviva.app.domain.repository.AuthRepository
import pe.modaviva.app.domain.usecase.ValidateConfirmPasswordUseCase
import pe.modaviva.app.domain.usecase.ValidateDocumentoUseCase
import pe.modaviva.app.domain.usecase.ValidateEmailUseCase
import pe.modaviva.app.domain.usecase.ValidatePasswordUseCase
import pe.modaviva.app.domain.usecase.ValidatePhoneUseCase
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val validateEmail: ValidateEmailUseCase,
    private val validateDocumento: ValidateDocumentoUseCase,
    private val validatePhone: ValidatePhoneUseCase,
    private val validatePassword: ValidatePasswordUseCase,
    private val validateConfirmPassword: ValidateConfirmPasswordUseCase,
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

    fun onFieldTouched(field: RegisterField) {
        _uiState.update { it.copy(touched = it.touched + field) }
        when (field) {
            RegisterField.EMAIL -> checkEmailUniqueness()
            RegisterField.DOCUMENTO -> checkDocumentoUniqueness()
            else -> Unit
        }
    }

    fun onSubmit(onSuccess: (RegisterResult.Success) -> Unit) {
        val current = _uiState.value
        val errors = validate(current)
        _uiState.update {
            it.copy(
                errors = errors,
                touched = RegisterField.entries.toSet(),
            )
        }
        if (errors.isNotEmpty() || _uiState.value.hasBlockingUniqueness) return

        _uiState.update { it.copy(isSubmitting = true, globalError = null) }
        viewModelScope.launch {
            val request = RegisterRequest(
                nombres = current.nombres.trim(),
                apellidos = current.apellidos.trim(),
                documento = current.documento.trim(),
                telefono = current.telefono.trim(),
                email = current.email.trim(),
                password = current.password,
            )
            authRepository.register(request)
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
        if (validateEmail.isValid(email).not()) {
            _uiState.update { it.copy(isCheckingEmail = false, emailTaken = false) }
            return
        }
        _uiState.update { it.copy(isCheckingEmail = true) }
        emailCheckJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            val taken = authRepository.isEmailTaken(email)
            _uiState.update { it.copy(isCheckingEmail = false, emailTaken = taken) }
        }
    }

    private fun checkDocumentoUniqueness() {
        val documento = _uiState.value.documento.trim()
        documentoCheckJob?.cancel()
        if (validateDocumento.isValid(documento).not()) {
            _uiState.update { it.copy(isCheckingDocumento = false, documentoTaken = false) }
            return
        }
        _uiState.update { it.copy(isCheckingDocumento = true) }
        documentoCheckJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            val taken = authRepository.isDocumentoTaken(documento)
            _uiState.update { it.copy(isCheckingDocumento = false, documentoTaken = taken) }
        }
    }

    private fun validate(state: RegisterUiState): Map<RegisterField, String> = buildMap {
        if (state.nombres.isBlank()) put(RegisterField.NOMBRES, "Ingresa tus nombres")
        if (state.apellidos.isBlank()) put(RegisterField.APELLIDOS, "Ingresa tus apellidos")
        validateDocumento(state.documento)?.let { put(RegisterField.DOCUMENTO, it) }
        validatePhone(state.telefono)?.let { put(RegisterField.TELEFONO, it) }
        validateEmail(state.email)?.let { put(RegisterField.EMAIL, it) }
        validatePassword(state.password)?.let { put(RegisterField.PASSWORD, it) }
        validateConfirmPassword(state.confirmPassword, state.password)?.let {
            put(RegisterField.CONFIRM_PASSWORD, it)
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 400L
    }
}
