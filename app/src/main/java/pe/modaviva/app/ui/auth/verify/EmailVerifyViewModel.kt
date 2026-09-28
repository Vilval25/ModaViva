package pe.modaviva.app.ui.auth.verify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pe.modaviva.app.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Repite el chequeo de verificación mientras la pantalla está en primer plano.
 *
 * Firebase Auth marca la cuenta como verificada al pulsar el enlace del correo,
 * pero Firestore no se entera solo. Cada intervalo se pregunta a Auth y, si ya
 * está verificado, se refleja en `clientes/{uid}.emailVerificado`.
 */
@HiltViewModel
class EmailVerifyViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private var pollJob: Job? = null

    fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (authRepository.syncEmailVerification().not()) {
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private companion object {
        const val POLL_INTERVAL_MS = 3_000L
    }
}