package pe.modaviva.app.ui.fitting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.modaviva.app.domain.model.UserProfile
import pe.modaviva.app.domain.repository.AuthRepository
import pe.modaviva.app.domain.repository.SessionRepository
import javax.inject.Inject

@HiltViewModel
class FittingViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val currentUser: StateFlow<UserProfile?> = sessionRepository.currentUser

    private val _isSubmittingConsent = MutableStateFlow(false)
    val isSubmittingConsent: StateFlow<Boolean> = _isSubmittingConsent.asStateFlow()

    private val _consentDeclinedTemporarily = MutableStateFlow(false)
    val consentDeclinedTemporarily: StateFlow<Boolean> = _consentDeclinedTemporarily.asStateFlow()

    private val _consentError = MutableStateFlow<String?>(null)
    val consentError: StateFlow<String?> = _consentError.asStateFlow()

    fun acceptConsent(onSuccess: () -> Unit = {}) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            _isSubmittingConsent.value = true
            _consentError.value = null
            val result = authRepository.acceptProbadorConsent(UserProfile.PROBADOR_AVISO_VERSION_ACTUAL)
            _isSubmittingConsent.value = false
            result.onSuccess {
                val updated = user.copy(
                    consentimientoProbadorAceptado = true,
                    consentimientoProbadorVersion = UserProfile.PROBADOR_AVISO_VERSION_ACTUAL,
                )
                sessionRepository.signIn(updated)
                _consentDeclinedTemporarily.value = false
                onSuccess()
            }.onFailure { e ->
                _consentError.value = e.localizedMessage ?: "No se pudo registrar el consentimiento."
            }
        }
    }

    fun declineConsent() {
        // CA-05: El usuario elige 'Ahora no'
        _consentDeclinedTemporarily.value = true
    }

    fun requestConsentAgain() {
        _consentDeclinedTemporarily.value = false
    }

    fun dismissError() {
        _consentError.value = null
    }
}
