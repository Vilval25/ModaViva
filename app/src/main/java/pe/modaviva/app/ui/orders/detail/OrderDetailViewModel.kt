package pe.modaviva.app.ui.orders.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.modaviva.app.domain.model.EstadoPedido
import pe.modaviva.app.domain.model.Pedido
import pe.modaviva.app.domain.model.SolicitudPostventa
import pe.modaviva.app.domain.model.TipoMedioPago
import pe.modaviva.app.domain.repository.OrderRepository
import pe.modaviva.app.domain.repository.SolicitudRepository
import javax.inject.Inject

data class OrderDetailUiState(
    val pedido: Pedido? = null,
    val cargando: Boolean = true,
    val segundosRestantes: Long = 0L,
    val actualizando: Boolean = false,
    val mensaje: String? = null,
    /** HU-24: solicitudes de cambio o devolución de este pedido. */
    val solicitudes: List<SolicitudPostventa> = emptyList(),
)

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orderRepository: OrderRepository,
    solicitudRepository: SolicitudRepository,
) : ViewModel() {

    private val pedidoId: String = checkNotNull(savedStateHandle["pedidoId"])
    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            orderRepository.observarPedidoPorId(pedidoId).collect { p ->
                _uiState.update {
                    it.copy(
                        pedido = p,
                        cargando = false,
                        segundosRestantes = p?.segundosRestantesPago() ?: 0L,
                    )
                }
            }
        }

        viewModelScope.launch {
            solicitudRepository.observarSolicitudesDePedido(pedidoId).collect { lista ->
                _uiState.update { it.copy(solicitudes = lista) }
            }
        }

        // Ticker de cuenta regresiva para pedidos PENDIENTE (HU-22 CA-02)
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                val p = _uiState.value.pedido
                if (p != null && p.estado == EstadoPedido.PENDIENTE) {
                    val segs = p.segundosRestantesPago()
                    _uiState.update { it.copy(segundosRestantes = segs) }
                    if (segs <= 0L) {
                        anularPedidoVencido()
                        break
                    }
                }
            }
        }
    }

    fun confirmarPagoPendiente(medio: TipoMedioPago) {
        _uiState.update { it.copy(actualizando = true) }
        viewModelScope.launch {
            val res = orderRepository.confirmarPagoPedidoPendiente(
                pedidoId = pedidoId,
                medioPago = medio,
                resumenMedioPago = medio.titulo,
            )
            res.onSuccess {
                _uiState.update {
                    it.copy(
                        actualizando = false,
                        mensaje = "¡Pago confirmado! Tu pedido ha pasado a estado Aprobado.",
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        actualizando = false,
                        mensaje = "Error al confirmar pago: ${err.message}",
                    )
                }
            }
        }
    }

    fun anularPedidoVencido() {
        _uiState.update { it.copy(actualizando = true) }
        viewModelScope.launch {
            val res = orderRepository.anularPedidoVencido(pedidoId)
            res.onSuccess {
                _uiState.update {
                    it.copy(
                        actualizando = false,
                        mensaje = "Reserva anulada por tiempo límite. Las prendas con stock han retornado a tu carrito.",
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(actualizando = false, mensaje = err.message)
                }
            }
        }
    }

    fun cambiarEstadoDemo(nuevoEstado: EstadoPedido) {
        _uiState.update { it.copy(actualizando = true) }
        viewModelScope.launch {
            val res = orderRepository.avanzarEstadoDemo(pedidoId, nuevoEstado)
            res.onSuccess {
                _uiState.update {
                    it.copy(
                        actualizando = false,
                        mensaje = "Estado actualizado a: ${nuevoEstado.etiqueta}",
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(actualizando = false, mensaje = err.message)
                }
            }
        }
    }

    fun limpiarMensaje() {
        _uiState.update { it.copy(mensaje = null) }
    }
}

