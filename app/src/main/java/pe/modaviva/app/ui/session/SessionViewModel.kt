package pe.modaviva.app.ui.session

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
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val currentUser: StateFlow<UserProfile?> = sessionRepository.currentUser

    private val _passwordResetSent = MutableStateFlow(false)
    val passwordResetSent: StateFlow<Boolean> = _passwordResetSent.asStateFlow()

    private val _isSendingPasswordReset = MutableStateFlow(false)
    val isSendingPasswordReset: StateFlow<Boolean> = _isSendingPasswordReset.asStateFlow()

    private val _passwordResetError = MutableStateFlow<String?>(null)
    val passwordResetError: StateFlow<String?> = _passwordResetError.asStateFlow()

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) return
        viewModelScope.launch {
            _isSendingPasswordReset.value = true
            _passwordResetError.value = null
            val result = authRepository.sendPasswordResetEmail(email)
            _isSendingPasswordReset.value = false
            result.onSuccess {
                _passwordResetSent.value = true
            }.onFailure { e ->
                _passwordResetError.value = e.localizedMessage ?: "No se pudo enviar el correo de recuperación."
            }
        }
    }

    fun dismissPasswordResetState() {
        _passwordResetSent.value = false
        _passwordResetError.value = null
    }

    private val _isUpdatingProfile = MutableStateFlow(false)
    val isUpdatingProfile: StateFlow<Boolean> = _isUpdatingProfile.asStateFlow()

    private val _updateProfileError = MutableStateFlow<String?>(null)
    val updateProfileError: StateFlow<String?> = _updateProfileError.asStateFlow()

    fun updateProfile(
        nombres: String,
        apellidos: String,
        telefono: String?,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            _isUpdatingProfile.value = true
            _updateProfileError.value = null
            val result = authRepository.updateProfile(nombres, apellidos, telefono)
            _isUpdatingProfile.value = false
            result.onSuccess { updated ->
                sessionRepository.signIn(updated)
                onSuccess()
            }.onFailure { e ->
                _updateProfileError.value = e.localizedMessage ?: "No se pudieron actualizar los datos."
            }
        }
    }

    fun dismissUpdateProfileError() {
        _updateProfileError.value = null
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            sessionRepository.signOut()
        }
    }
}
