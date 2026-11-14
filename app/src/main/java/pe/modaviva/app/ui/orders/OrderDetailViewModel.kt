package pe.modaviva.app.ui.orders

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
import javax.inject.Inject

data class OrderDetailUiState(
    val pedido: Pedido? = null,
    val cargando: Boolean = true,
    val segundosRestantes: Long = 0L,
    val actualizando: Boolean = false,
    val mensaje: String? = null,
    val mostrarDialogoPostventa: Boolean = false,
)

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orderRepository: OrderRepository,
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

    fun registrarPostventa(
        prendaCodigo: String,
        prendaNombre: String,
        talla: String,
        tipo: String,
        motivo: String,
        detalleOtro: String?,
    ) {
        _uiState.update { it.copy(actualizando = true) }
        viewModelScope.launch {
            val s = SolicitudPostventa(
                id = "",
                numeroSolicitud = "",
                pedidoId = pedidoId,
                prendaCodigo = prendaCodigo,
                prendaNombre = prendaNombre,
                talla = talla,
                tipo = tipo,
                motivo = motivo,
                detalleOtro = detalleOtro,
                tuvoPruebaVirtual = _uiState.value.pedido?.items?.firstOrNull { it.codigo == prendaCodigo }?.usoProbadorVirtual == true,
            )
            val res = orderRepository.registrarSolicitudPostventa(pedidoId, s)
            res.onSuccess { sol ->
                _uiState.update {
                    it.copy(
                        actualizando = false,
                        mostrarDialogoPostventa = false,
                        mensaje = "Solicitud ${sol.numeroSolicitud} registrada con éxito (En evaluación).",
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(actualizando = false, mensaje = err.message)
                }
            }
        }
    }

    fun setMostrarDialogoPostventa(mostrar: Boolean) {
        _uiState.update { it.copy(mostrarDialogoPostventa = mostrar) }
    }

    fun limpiarMensaje() {
        _uiState.update { it.copy(mensaje = null) }
    }
}

