package pe.modaviva.app.ui.checkout

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
import pe.modaviva.app.domain.model.EstadoPedido
import pe.modaviva.app.domain.model.ItemPedido
import pe.modaviva.app.domain.model.Pedido
import pe.modaviva.app.domain.model.TarjetaGuardada
import pe.modaviva.app.domain.model.TipoMedioPago
import pe.modaviva.app.domain.repository.CatalogoRepository
import pe.modaviva.app.domain.repository.OrderRepository
import pe.modaviva.app.domain.repository.SessionRepository
import pe.modaviva.app.domain.repository.TiendaRepository
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
    private val tiendaRepository: TiendaRepository,
    private val catalogoRepository: CatalogoRepository,
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

    fun procesarPago() {
        val s = _uiState.value
        if (s.procesandoPago) return

        val medio = s.medioSeleccionado
        var resumenMedio = medio.titulo
        var tarjetaParaGuardar: TarjetaGuardada? = null

        if (medio.esTarjeta) {
            if (s.usarNuevaTarjeta || s.tarjetasGuardadas.isEmpty()) {
                val resultadoToken = TarjetaGuardada.validarYTokenizarSandbox(
                    numeroTarjeta = s.numeroTarjeta,
                    vencimiento = s.vencimiento,
                    cvv = s.cvv,
                    titular = s.titular,
                    tipo = if (medio == TipoMedioPago.TARJETA_CREDITO) "CREDITO" else "DEBITO",
                )

                if (resultadoToken.isFailure) {
                    _uiState.update {
                        it.copy(errorPago = resultadoToken.exceptionOrNull()?.message ?: "Datos de tarjeta no válidos.")
                    }
                    return
                }

                val tarjetaNueva = resultadoToken.getOrNull()!!
                resumenMedio = tarjetaNueva.etiquetaResumen
                if (s.guardarTarjetaParaProximas) {
                    tarjetaParaGuardar = tarjetaNueva
                }
            } else {
                val tarjetaGuardada = s.tarjetasGuardadas.firstOrNull { it.id == s.tarjetaSeleccionadaId }
                if (tarjetaGuardada == null) {
                    _uiState.update { it.copy(errorPago = "Selecciona una tarjeta para pagar.") }
                    return
                }
                resumenMedio = tarjetaGuardada.etiquetaResumen
            }
        } else if (medio == TipoMedioPago.BILLETERA_ELECTRONICA) {
            resumenMedio = "Billetera electrónica (Yape / Plin)"
        } else if (medio == TipoMedioPago.CONTRA_ENTREGA) {
            resumenMedio = "Pago contra entrega / Reserva 30 min"
        }

        _uiState.update { it.copy(procesandoPago = true, errorPago = null) }

        viewModelScope.launch {
            try {
                // Guardar tarjeta si correspondía
                tarjetaParaGuardar?.let {
                    orderRepository.guardarTarjeta(it)
                }

                val tipoEntrega = checkoutSessionRepository.tipoEntrega.value
                val destino = checkoutSessionRepository.destinoActual.value
                val tiendaId = checkoutSessionRepository.tiendaRecojoId.value
                val profile = sessionRepository.currentUser.value
                val uid = profile?.uid ?: "uid_guest"

                val tiendas = tiendaRepository.observarTiendas().first()
                val tiendaSeleccionada = tiendas.firstOrNull { it.id == tiendaId }

                val itemsCalculados = checkoutSessionRepository.itemsCalculados.value
                val itemsPedido = itemsCalculados.map {
                    ItemPedido(
                        prendaId = it.prendaId,
                        codigo = it.codigo,
                        nombre = it.nombre,
                        fotoUrl = it.fotoUrl,
                        talla = it.talla,
                        cantidad = it.cantidadAprobada,
                        precioUnitario = it.precioUnitario,
                        subtotal = it.subtotal,
                        usoProbadorVirtual = false,
                    )
                }

                val estadoInicial = if (medio == TipoMedioPago.CONTRA_ENTREGA) {
                    EstadoPedido.PENDIENTE
                } else {
                    EstadoPedido.APROBADO
                }

                val destinatario = checkoutSessionRepository.destinatario.value.ifBlank {
                    profile?.let { "${it.nombres} ${it.apellidos}".trim() }.orEmpty()
                }
                val telefono = checkoutSessionRepository.telefonoContacto.value.ifBlank {
                    profile?.telefono.orEmpty()
                }

                val nuevoPedido = Pedido(
                    id = "",
                    numeroPedido = "",
                    uidCliente = uid,
                    estado = estadoInicial,
                    tipoEntrega = tipoEntrega,
                    direccionEntrega = destino?.direccion ?: tiendaSeleccionada?.direccion.orEmpty(),
                    referenciaEntrega = destino?.referencia,
                    distrito = destino?.distrito ?: tiendaSeleccionada?.distrito.orEmpty(),
                    distritoSlug = destino?.distritoSlug.orEmpty(),
                    tiendaRecojoId = tiendaId,
                    tiendaRecojoNombre = tiendaSeleccionada?.nombre,
                    tiendaRecojoDireccion = tiendaSeleccionada?.direccion,
                    tiendaRecojoHorario = tiendaSeleccionada?.horario,
                    destinatario = destinatario,
                    telefonoContacto = telefono,
                    medioPago = medio,
                    resumenMedioPago = resumenMedio,
                    items = itemsPedido,
                    subtotal = s.subtotal,
                    descuentoTotal = s.descuentoTotal,
                    costoEnvio = s.costoEnvio,
                    total = s.total,
                    cuponAplicado = s.cupon,
                )

                val resultadoPedido = orderRepository.crearPedido(nuevoPedido)
                resultadoPedido.onSuccess { creado ->
                    checkoutSessionRepository.limpiarDestino()
                    _uiState.update {
                        it.copy(
                            procesandoPago = false,
                            pedidoConfirmadoId = creado.id,
                        )
                    }
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(
                            procesandoPago = false,
                            errorPago = err.message ?: "Ocurrió un error al confirmar tu pedido.",
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        procesandoPago = false,
                        errorPago = e.message ?: "Error inesperado al conectar con la pasarela.",
                    )
                }
            }
        }
    }
}

