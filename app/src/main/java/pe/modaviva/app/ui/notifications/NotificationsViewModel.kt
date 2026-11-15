package pe.modaviva.app.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.modaviva.app.domain.model.Notificacion
import pe.modaviva.app.domain.repository.NotificacionRepository
import javax.inject.Inject

sealed interface NotificationsUiState {
    data object Cargando : NotificationsUiState
    data class Listo(val avisos: List<Notificacion>) : NotificationsUiState {
        val hayNoLeidos: Boolean get() = avisos.any { !it.leida }
    }
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificacionRepository,
) : ViewModel() {

    val uiState: StateFlow<NotificationsUiState> = repository.notificaciones
        .map<List<Notificacion>, NotificationsUiState> { NotificationsUiState.Listo(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationsUiState.Cargando)

    /** CA-02: al abrir un aviso se marca como leído. */
    fun abrir(aviso: Notificacion) {
        if (aviso.leida) return
        viewModelScope.launch { repository.marcarLeida(aviso.id) }
    }

    /** CA-03. */
    fun marcarTodoLeido() {
        viewModelScope.launch { repository.marcarTodasLeidas() }
    }
}
