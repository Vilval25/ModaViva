package pe.modaviva.app.ui.addresses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.modaviva.app.domain.model.DireccionEntrega
import pe.modaviva.app.domain.model.TarifaEnvio
import pe.modaviva.app.domain.repository.BackendRepository
import pe.modaviva.app.domain.repository.DireccionRepository
import javax.inject.Inject

data class AddressesUiState(
    val cargando: Boolean = false,
    val direcciones: List<DireccionEntrega> = emptyList(),
    val tarifasDisponibles: List<TarifaEnvio> = emptyList(),
    val error: String? = null,
    val mensajeExito: String? = null,
)

@HiltViewModel
class AddressesViewModel @Inject constructor(
    private val direccionRepository: DireccionRepository,
    private val backendRepository: BackendRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddressesUiState(cargando = true))
    val uiState: StateFlow<AddressesUiState> = _uiState.asStateFlow()

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            val tarifasRes = backendRepository.obtenerTarifasEnvio()
            val tarifas = tarifasRes.getOrDefault(emptyList())

            direccionRepository.observarDirecciones().collect { lista ->
                _uiState.update {
                    it.copy(
                        cargando = false,
                        direcciones = lista,
                        tarifasDisponibles = tarifas,
                    )
                }
            }
        }
    }

    fun guardarDireccion(
        direccion: DireccionEntrega,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            val result = direccionRepository.guardarDireccion(direccion)
            result.onSuccess {
                _uiState.update { it.copy(cargando = false, mensajeExito = "Dirección guardada con éxito") }
                onSuccess()
            }.onFailure { e ->
                _uiState.update { it.copy(cargando = false, error = e.localizedMessage ?: "No se pudo guardar la dirección") }
            }
        }
    }

    fun eliminarDireccion(direccionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            val result = direccionRepository.eliminarDireccion(direccionId)
            result.onSuccess {
                _uiState.update { it.copy(cargando = false, mensajeExito = "Dirección eliminada") }
            }.onFailure { e ->
                _uiState.update { it.copy(cargando = false, error = e.localizedMessage ?: "No se pudo eliminar la dirección") }
            }
        }
    }

    fun establecerPrincipal(direccionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            val result = direccionRepository.establecerPrincipal(direccionId)
            result.onSuccess {
                _uiState.update { it.copy(cargando = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(cargando = false, error = e.localizedMessage ?: "No se pudo establecer como principal") }
            }
        }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(error = null, mensajeExito = null) }
    }
}
