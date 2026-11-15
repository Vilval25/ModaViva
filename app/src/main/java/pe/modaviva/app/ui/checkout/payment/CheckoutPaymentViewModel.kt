package pe.modaviva.app.ui.checkout.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.modaviva.app.data.repository.CheckoutSessionRepository
import pe.modaviva.app.domain.model.DatosTarjetaNueva
import pe.modaviva.app.domain.model.ItemCalculoRequest
import pe.modaviva.app.domain.model.ResultadoPago
import pe.modaviva.app.domain.model.SolicitudPago
import pe.modaviva.app.domain.model.TarjetaGuardada
import pe.modaviva.app.domain.model.TipoMedioPago
import pe.modaviva.app.domain.repository.AnalyticsRepository
import pe.modaviva.app.domain.repository.OrderRepository
import pe.modaviva.app.domain.repository.SessionRepository
import java.util.UUID
import javax.inject.Inject

data class CheckoutPaymentUiState(
    val medioSeleccionado: TipoMedioPago = TipoMedioPago.TARJETA_CREDITO,
    val tarjetasGuardadas: List<TarjetaGuardada> = emptyList(),
    val tarjetaSeleccionadaId: String? = null,
    val usarNuevaTarjeta: Boolean = false,
    val numeroTarjeta: String = "",
    val vencimiento: String = "",
    val cvv: String = "",
    val titular: String = "",
    val guardarTarjetaParaProximas: Boolean = true,
    val subtotal: Double = 0.0,
    val descuentoTotal: Double = 0.0,
    val costoEnvio: Double = 0.0,
    val total: Double = 0.0,
    val cupon: String? = null,
    val procesandoPago: Boolean = false,
    val errorPago: String? = null,
    val pedidoConfirmadoId: String? = null,
)

@HiltViewModel
class CheckoutPaymentViewModel @Inject constructor(
    private val checkoutSessionRepository: CheckoutSessionRepository,
    private val orderRepository: OrderRepository,
    private val sessionRepository: SessionRepository,
    private val analyticsRepository: AnalyticsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutPaymentUiState())
    val uiState: StateFlow<CheckoutPaymentUiState> = _uiState.asStateFlow()

    init {
        val subtotal = checkoutSessionRepository.subtotal.value
        val desc = checkoutSessionRepository.descuentoTotal.value
        val envio = checkoutSessionRepository.costoEnvio.value
        val tot = checkoutSessionRepository.total.value
        val cup = checkoutSessionRepository.cuponCodigo.value
        val profile = sessionRepository.currentUser.value
        val titularDefault = profile?.let { "${it.nombres} ${it.apellidos}".trim().uppercase() } ?: ""

        _uiState.update {
            it.copy(
                subtotal = subtotal,
                descuentoTotal = desc,
                costoEnvio = envio,
                total = tot,
                cupon = cup,
                titular = titularDefault,
            )
        }

        viewModelScope.launch {
            orderRepository.observarTarjetasGuardadas().collect { tarjetas ->
                val predeterminada = tarjetas.firstOrNull { it.esPredeterminada } ?: tarjetas.firstOrNull()
                _uiState.update { current ->
                    current.copy(
                        tarjetasGuardadas = tarjetas,
                        tarjetaSeleccionadaId = current.tarjetaSeleccionadaId ?: predeterminada?.id,
                        usarNuevaTarjeta = tarjetas.isEmpty(),
                    )
                }
            }
        }
    }

    fun onMedioPagoChanged(medio: TipoMedioPago) {
        _uiState.update { it.copy(medioSeleccionado = medio, errorPago = null) }
    }

    fun onSeleccionarTarjetaGuardada(tarjetaId: String) {
        _uiState.update {
            it.copy(
                tarjetaSeleccionadaId = tarjetaId,
                usarNuevaTarjeta = false,
                errorPago = null,
            )
        }
    }

    fun onActivarNuevaTarjeta(activar: Boolean) {
        _uiState.update { it.copy(usarNuevaTarjeta = activar, errorPago = null) }
    }

    fun onNumeroTarjetaChanged(v: String) {
        val soloDigitos = v.filter { it.isDigit() }.take(16)
        _uiState.update { it.copy(numeroTarjeta = soloDigitos, errorPago = null) }
    }

    fun onVencimientoChanged(v: String) {
        val limpio = v.filter { it.isDigit() || it == '/' }.take(5)
        _uiState.update { it.copy(vencimiento = limpio, errorPago = null) }
    }

    fun onCvvChanged(v: String) {
        val limpio = v.filter { it.isDigit() }.take(4)
        _uiState.update { it.copy(cvv = limpio, errorPago = null) }
    }

    fun onTitularChanged(v: String) {
        _uiState.update { it.copy(titular = v.uppercase(), errorPago = null) }
    }

    fun onGuardarTarjetaToggle(v: Boolean) {
        _uiState.update { it.copy(guardarTarjetaParaProximas = v) }
    }

    /**
     * Identificador del intento de pago (HU-21 CA-10). Se mantiene mientras el intento siga
     * abierto: un doble toque o un reintento tras un corte de red reusan la misma clave y el
     * servidor devuelve el mismo pedido sin cobrar dos veces. Se renueva tras un rechazo.
     */
    private var idempotencyKey: String = nuevaClave()

    private fun nuevaClave(): String = "pay_" + UUID.randomUUID().toString().replace("-", "")

    fun procesarPago() {
        val s = _uiState.value
        if (s.procesandoPago) return

        val medio = s.medioSeleccionado
        val usaTarjetaNueva = medio.esTarjeta && (s.usarNuevaTarjeta || s.tarjetasGuardadas.isEmpty())
        val tarjetaGuardadaId = if (medio.esTarjeta && !usaTarjetaNueva) s.tarjetaSeleccionadaId else null

        if (medio.esTarjeta && !usaTarjetaNueva && tarjetaGuardadaId == null) {
            _uiState.update { it.copy(errorPago = "Selecciona una tarjeta para pagar.") }
            return
        }

        _uiState.update { it.copy(procesandoPago = true, errorPago = null) }

        viewModelScope.launch {
            val profile = sessionRepository.currentUser.value
            val destino = checkoutSessionRepository.destinoActual.value

            val codigosProbados = runCatching {
                analyticsRepository.observarPruebasVirtuales().first()
                    .map { it.prendaCodigo }
                    .distinct()
            }.getOrDefault(emptyList())

            val items = checkoutSessionRepository.itemsCalculados.value
                .filter { it.cantidadAprobada > 0 }
                .map {
                    ItemCalculoRequest(
                        prendaId = it.prendaId,
                        talla = it.talla,
                        cantidad = it.cantidadAprobada,
                        precioVisto = it.precioUnitario,
                    )
                }

            val solicitud = SolicitudPago(
                idempotencyKey = idempotencyKey,
                items = items,
                tipoEntrega = checkoutSessionRepository.tipoEntrega.value,
                distritoSlug = destino?.distritoSlug?.ifBlank { null },
                cupones = listOfNotNull(s.cupon),
                direccion = destino?.direccion,
                referencia = destino?.referencia,
                distrito = destino?.distrito,
                tiendaRecojoId = checkoutSessionRepository.tiendaRecojoId.value,
                destinatario = checkoutSessionRepository.destinatario.value.ifBlank {
                    profile?.let { "${it.nombres} ${it.apellidos}".trim() }.orEmpty()
                },
                telefono = checkoutSessionRepository.telefonoContacto.value.ifBlank {
                    profile?.telefono.orEmpty()
                },
                medioPago = medio,
                tarjetaGuardadaId = tarjetaGuardadaId,
                tarjetaNueva = if (usaTarjetaNueva) {
                    DatosTarjetaNueva(
                        numero = s.numeroTarjeta,
                        vencimiento = s.vencimiento,
                        cvv = s.cvv,
                        titular = s.titular,
                    )
                } else null,
                guardarTarjeta = usaTarjetaNueva && s.guardarTarjetaParaProximas,
                prendasProbadas = codigosProbados,
                totalEsperado = s.total,
            )

            orderRepository.pagarPedido(solicitud)
                .onSuccess { resultado ->
                    when (resultado) {
                        is ResultadoPago.Aprobado, is ResultadoPago.Pendiente -> {
                            val pedidoId = (resultado as? ResultadoPago.Aprobado)?.pedidoId
                                ?: (resultado as ResultadoPago.Pendiente).pedidoId
                            checkoutSessionRepository.limpiarDestino()
                            _uiState.update {
                                it.copy(procesandoPago = false, pedidoConfirmadoId = pedidoId, cvv = "")
                            }
                        }
                        is ResultadoPago.Rechazado -> {
                            // CA-09: nuevo intento con otro medio; el carrito se conserva.
                            idempotencyKey = nuevaClave()
                            _uiState.update {
                                it.copy(procesandoPago = false, errorPago = resultado.motivo, cvv = "")
                            }
                        }
                    }
                }
                .onFailure { err ->
                    // Error de red o de validación: se conserva la clave para que el reintento
                    // no genere un segundo cobro si el servidor alcanzó a procesar el pago.
                    _uiState.update {
                        it.copy(
                            procesandoPago = false,
                            errorPago = err.message ?: "No se pudo conectar con la pasarela. Intenta de nuevo.",
                        )
                    }
                }
        }
    }
}
