package pe.modaviva.app.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.modaviva.app.domain.model.DestinoEntrega
import pe.modaviva.app.domain.model.ItemPedidoCalculado
import pe.modaviva.app.domain.model.TarjetaGuardada
import pe.modaviva.app.domain.model.TipoMedioPago
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckoutSessionRepository @Inject constructor() {

    private val _destinoActual = MutableStateFlow<DestinoEntrega?>(null)
    val destinoActual: StateFlow<DestinoEntrega?> = _destinoActual.asStateFlow()

    private val _tiendaRecojoId = MutableStateFlow<String?>(null)
    val tiendaRecojoId: StateFlow<String?> = _tiendaRecojoId.asStateFlow()

    private val _tipoEntrega = MutableStateFlow("delivery") // "delivery" | "recojo"
    val tipoEntrega: StateFlow<String> = _tipoEntrega.asStateFlow()

    private val _subtotal = MutableStateFlow(0.0)
    val subtotal: StateFlow<Double> = _subtotal.asStateFlow()

    private val _descuentoTotal = MutableStateFlow(0.0)
    val descuentoTotal: StateFlow<Double> = _descuentoTotal.asStateFlow()

    private val _costoEnvio = MutableStateFlow(0.0)
    val costoEnvio: StateFlow<Double> = _costoEnvio.asStateFlow()

    private val _total = MutableStateFlow(0.0)
    val total: StateFlow<Double> = _total.asStateFlow()

    private val _cuponCodigo = MutableStateFlow<String?>(null)
    val cuponCodigo: StateFlow<String?> = _cuponCodigo.asStateFlow()

    private val _itemsCalculados = MutableStateFlow<List<ItemPedidoCalculado>>(emptyList())
    val itemsCalculados: StateFlow<List<ItemPedidoCalculado>> = _itemsCalculados.asStateFlow()

    private val _tarjetaSeleccionada = MutableStateFlow<TarjetaGuardada?>(null)
    val tarjetaSeleccionada: StateFlow<TarjetaGuardada?> = _tarjetaSeleccionada.asStateFlow()

    private val _medioPagoSeleccionado = MutableStateFlow(TipoMedioPago.TARJETA_CREDITO)
    val medioPagoSeleccionado: StateFlow<TipoMedioPago> = _medioPagoSeleccionado.asStateFlow()

    fun actualizarDestino(destino: DestinoEntrega) {
        _destinoActual.value = destino
    }

    fun actualizarTiendaRecojo(tiendaId: String?) {
        _tiendaRecojoId.value = tiendaId
    }

    fun guardarResumenCalculo(
        tipoEntrega: String,
        subtotal: Double,
        descuentoTotal: Double,
        costoEnvio: Double,
        total: Double,
        cupon: String?,
        items: List<ItemPedidoCalculado>,
    ) {
        _tipoEntrega.value = tipoEntrega
        _subtotal.value = subtotal
        _descuentoTotal.value = descuentoTotal
        _costoEnvio.value = costoEnvio
        _total.value = total
        _cuponCodigo.value = cupon
        _itemsCalculados.value = items
    }

    fun seleccionarMedioPago(medio: TipoMedioPago, tarjeta: TarjetaGuardada? = null) {
        _medioPagoSeleccionado.value = medio
        _tarjetaSeleccionada.value = tarjeta
    }

    fun limpiarDestino() {
        _destinoActual.value = null
        _tiendaRecojoId.value = null
        _subtotal.value = 0.0
        _descuentoTotal.value = 0.0
        _costoEnvio.value = 0.0
        _total.value = 0.0
        _cuponCodigo.value = null
        _itemsCalculados.value = emptyList()
        _tarjetaSeleccionada.value = null
    }
}
