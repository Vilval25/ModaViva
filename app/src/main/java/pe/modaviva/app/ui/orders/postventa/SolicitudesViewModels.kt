package pe.modaviva.app.ui.orders.postventa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pe.modaviva.app.domain.model.SolicitudPostventa
import pe.modaviva.app.domain.repository.SolicitudRepository
import javax.inject.Inject

/** null mientras carga. */
@HiltViewModel
class MisSolicitudesViewModel @Inject constructor(
    repository: SolicitudRepository,
) : ViewModel() {
    val solicitudes: StateFlow<List<SolicitudPostventa>?> = repository.observarSolicitudes()
        .map<List<SolicitudPostventa>, List<SolicitudPostventa>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@HiltViewModel
class SolicitudDetalleViewModel @Inject constructor(
    savedStateHandle: androidx.lifecycle.SavedStateHandle,
    repository: SolicitudRepository,
) : ViewModel() {
    private val solicitudId: String = checkNotNull(savedStateHandle["solicitudId"])

    /** Primero [Cargando]; luego la solicitud o null si no existe. */
    val estado: StateFlow<DetalleEstado> = repository.observarSolicitud(solicitudId)
        .map { if (it == null) DetalleEstado.NoEncontrada else DetalleEstado.Lista(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleEstado.Cargando)
}

sealed interface DetalleEstado {
    data object Cargando : DetalleEstado
    data object NoEncontrada : DetalleEstado
    data class Lista(val solicitud: SolicitudPostventa) : DetalleEstado
}
