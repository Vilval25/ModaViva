package pe.modaviva.app.ui.screens.register

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
import pe.modaviva.app.data.model.RegisterRequest
import pe.modaviva.app.data.model.Validation
import pe.modaviva.app.data.repository.AuthRepository
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private var emailCheckJob: Job? = null
    private var documentoCheckJob: Job? = null

    fun onNombresChange(value: String) = updateValue(value) { it.copy(nombres = value) }
    fun onApellidosChange(value: String) = updateValue(value) { it.copy(apellidos = value) }
    fun onDocumentoChange(value: String) = updateValue(value) { it.copy(documento = value) }
    fun onTelefonoChange(value: String) = updateValue(value) { it.copy(telefono = value) }
    fun onEmailChange(value: String) = updateValue(value) { it.copy(email = value) }
    fun onPasswordChange(value: String) = updateValue(value) { it.copy(password = value) }
    fun onConfirmPasswordChange(value: String) =
        updateValue(value) { it.copy(confirmPassword = value) }

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

    private fun updateValue(
        value: String,
        transform: (RegisterUiState) -> RegisterUiState,
    ) {
        _uiState.update { state ->
            val updated = transform(state)
            updated.copy(errors = validate(updated))
        }
    }

    private fun checkEmailUniqueness() {
        val email = _uiState.value.email.trim()
        emailCheckJob?.cancel()
        if (!Validation.isValidEmail(email)) {
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
        if (!Validation.isValidDocumento(documento)) {
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
        if (!Validation.isValidDocumento(state.documento)) {
            put(RegisterField.DOCUMENTO, "El documento debe tener 8 dígitos")
        }
        if (!Validation.isValidPhone(state.telefono)) {
            put(RegisterField.TELEFONO, "Ingresa un celular válido de 9 dígitos")
        }
        if (!Validation.isValidEmail(state.email)) {
            put(RegisterField.EMAIL, "Ingresa un correo válido")
        }
        Validation.passwordError(state.password)?.let {
            put(RegisterField.PASSWORD, it)
        }
        Validation.confirmPasswordError(state.confirmPassword, state.password)?.let {
            put(RegisterField.CONFIRM_PASSWORD, it)
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 400L
    }
}
