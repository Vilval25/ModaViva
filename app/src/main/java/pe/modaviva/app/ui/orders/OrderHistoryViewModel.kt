package pe.modaviva.app.ui.orders

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
import pe.modaviva.app.domain.model.Pedido
import pe.modaviva.app.domain.repository.OrderRepository
import pe.modaviva.app.domain.repository.SessionRepository
import javax.inject.Inject

data class OrderHistoryUiState(
    val pedidos: List<Pedido> = emptyList(),
    val cargando: Boolean = true,
    val sembrando: Boolean = false,
    val mensaje: String? = null,
)

@HiltViewModel
class OrderHistoryViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderHistoryUiState())
    val uiState: StateFlow<OrderHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            orderRepository.observarPedidos().collect { lista ->
                _uiState.update {
                    it.copy(
                        pedidos = lista,
                        cargando = false,
                    )
                }
            }
        }
    }

    fun sembrarDatosDemo() {
        if (_uiState.value.sembrando) return
        _uiState.update { it.copy(sembrando = true, mensaje = null) }
        viewModelScope.launch {
            val res = orderRepository.cargarDatosSemilla()
            res.onSuccess { cant ->
                _uiState.update {
                    it.copy(
                        sembrando = false,
                        mensaje = "¡Se cargaron $cant pedidos de prueba exitosamente!",
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        sembrando = false,
                        mensaje = "Error al cargar datos demo: ${err.message}",
                    )
                }
            }
        }
    }

    fun limpiarMensaje() {
        _uiState.update { it.copy(mensaje = null) }
    }
}

