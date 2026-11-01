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
import pe.modaviva.app.domain.model.CuponAplicado
import pe.modaviva.app.domain.model.ItemCalculoRequest
import pe.modaviva.app.domain.model.ItemPedidoCalculado
import pe.modaviva.app.domain.model.ResultadoCalculoPedido
import pe.modaviva.app.domain.model.SolicitudCalculoPedido
import pe.modaviva.app.domain.model.TarifaEnvio
import pe.modaviva.app.domain.repository.BackendRepository
import pe.modaviva.app.domain.repository.CartRepository
import pe.modaviva.app.domain.repository.CatalogoRepository
import pe.modaviva.app.domain.repository.StockRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@HiltViewModel
class CheckoutSummaryViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val catalogoRepository: CatalogoRepository,
    private val stockRepository: StockRepository,
    private val backendRepository: BackendRepository,
    private val reloj: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutSummaryUiState())
    val uiState: StateFlow<CheckoutSummaryUiState> = _uiState.asStateFlow()

    init {
        inicializarCheckout()
    }

    fun inicializarCheckout() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, errorGeneral = null) }

            // 1. Obtener tarifas de distritos disponibles
            val tarifas = backendRepository.obtenerTarifasEnvio().getOrDefault(emptyList())
            val distritoPorDefecto = tarifas.firstOrNull { it.cobertura } ?: tarifas.firstOrNull()

            _uiState.update {
                it.copy(
                    distritosDisponibles = tarifas,
                    distritoSeleccionado = distritoPorDefecto,
                )
            }

            // 2. Revalidar y calcular pedido
            recalcular()
            _uiState.update { it.copy(cargando = false) }
        }
    }

    fun onTipoEntregaChanged(tipo: TipoEntregaUi) {
        if (_uiState.value.tipoEntrega == tipo) return
        _uiState.update { it.copy(tipoEntrega = tipo) }
        recalcular()
    }

    fun onDistritoChanged(tarifa: TarifaEnvio) {
        if (_uiState.value.distritoSeleccionado?.id == tarifa.id) return
        _uiState.update { it.copy(distritoSeleccionado = tarifa) }
        if (_uiState.value.tipoEntrega == TipoEntregaUi.DELIVERY) {
            recalcular()
        }
    }

    fun onCuponInputChanged(input: String) {
        _uiState.update {
            it.copy(
                cuponInput = input,
                mensajeErrorCupon = null,
                mensajeExitoCupon = null,
            )
        }
    }

    fun aplicarCupon() {
        val codigo = _uiState.value.cuponInput.trim().uppercase()
        if (codigo.isBlank()) {
            _uiState.update { it.copy(mensajeErrorCupon = "Ingresa un código de cupón.") }
            return
        }

        val actuales = _uiState.value.cuponesAplicados.map { it.codigo }
        if (actuales.contains(codigo)) {
            _uiState.update { it.copy(mensajeErrorCupon = "El cupón ya se encuentra aplicado.") }
            return
        }

        val nuevosCupones = actuales + codigo
        recalcularConCupones(nuevosCupones, codigoRecienAgregado = codigo)
    }

    fun removerCupon(codigo: String) {
        val nuevos = _uiState.value.cuponesAplicados
            .filter { it.codigo != codigo }
            .map { it.codigo }

        _uiState.update {
            it.copy(
                mensajeErrorCupon = null,
                mensajeExitoCupon = null,
            )
        }
        recalcularConCupones(nuevos)
    }

    fun descartarAlerta(index: Int) {
        _uiState.update {
            val lista = it.alertas.toMutableList()
            if (index in lista.indices) lista.removeAt(index)
            it.copy(alertas = lista)
        }
    }

    private fun recalcular() {
        val cupones = _uiState.value.cuponesAplicados.map { it.codigo }
        recalcularConCupones(cupones)
    }

    private fun recalcularConCupones(cupones: List<String>, codigoRecienAgregado: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(calculando = true, errorGeneral = null) }

            val cartItems = cartRepository.items.first()
            if (cartItems.isEmpty()) {
                _uiState.update {
                    it.copy(
                        calculando = false,
                        items = emptyList(),
                        subtotal = 0.0,
                        descuentoTotal = 0.0,
                        costoEnvio = 0.0,
                        total = 0.0,
                        totalPrendas = 0,
                    )
                }
                return@launch
            }

            val itemsSolicitud = cartItems.map {
                ItemCalculoRequest(
                    prendaId = it.prendaId,
                    talla = it.talla,
                    cantidad = it.cantidad,
                    precioVisto = if (it.precioAlAgregar > 0.0) it.precioAlAgregar else null,
                )
            }

            val tipoEntregaStr = if (_uiState.value.tipoEntrega == TipoEntregaUi.RECOJO) "recojo" else "delivery"
            val distritoSlug = _uiState.value.distritoSeleccionado?.id

            val solicitud = SolicitudCalculoPedido(
                items = itemsSolicitud,
                tipoEntrega = tipoEntregaStr,
                distritoSlug = distritoSlug,
                cupones = cupones,
            )

            val resultado = backendRepository.calcularPedido(solicitud)

            resultado.onSuccess { res ->
                val errorCupon = res.erroresCupon.firstOrNull()
                val exitoCupon = if (codigoRecienAgregado != null && res.cuponesAplicados.any { it.codigo == codigoRecienAgregado }) {
                    "¡Cupón $codigoRecienAgregado aplicado con éxito!"
                } else null

                _uiState.update {
                    it.copy(
                        calculando = false,
                        items = res.items,
                        subtotal = res.subtotal,
                        descuentoTotal = res.descuentoTotal,
                        costoEnvio = res.costoEnvio,
                        total = res.total,
                        totalPrendas = res.totalPrendasAprobadas,
                        cuponesAplicados = res.cuponesAplicados,
                        cuponInput = if (exitoCupon != null) "" else it.cuponInput,
                        mensajeErrorCupon = errorCupon,
                        mensajeExitoCupon = exitoCupon,
                        alertas = res.alertas,
                    )
                }
            }.onFailure {
                // Fallback de cálculo local cuando no hay conexión a Cloud Functions
                calcularLocalmente(cartItems, cupones, codigoRecienAgregado)
            }
        }
    }

    /**
     * Fallback de cálculo local para desarrollo y pruebas sin conexión directa
     * a las Cloud Functions, cumpliendo las reglas del negocio de CA-01 a CA-06.
     */
    private suspend fun calcularLocalmente(
        cartItems: List<pe.modaviva.app.domain.model.CartItem>,
        cupones: List<String>,
        codigoRecienAgregado: String?,
    ) {
        val ahora = Instant.now(reloj)
        val prendas = catalogoRepository.prendas.first().associateBy { it.codigo }
        val stocks = stockRepository.observeStocks().first()

        val itemsCalculados = mutableListOf<ItemPedidoCalculado>()
        val alertas = mutableListOf<String>()
        var subtotal = 0.0

        for (item in cartItems) {
            val prenda = prendas[item.prendaId]
            val nombre = prenda?.nombre ?: "Prenda ${item.prendaId}"
            val foto = prenda?.fotos?.firstOrNull().orEmpty()
            val precioActual = prenda?.precioVigente(ahora) ?: 0.0
            val maxStock = stocks[item.varianteId]?.total ?: prenda?.stockTotal ?: 0
            val estaAgotado = (maxStock <= 0) || (prenda != null && prenda.agotada)

            var cantidadAprobada = item.cantidad
            if (estaAgotado) {
                cantidadAprobada = 0
                alertas.add("'$nombre' (talla ${item.talla}) está agotado y se retiró del total.")
            } else if (maxStock < item.cantidad) {
                cantidadAprobada = maxStock
                alertas.add("Solo quedan $maxStock unidad(es) de '$nombre'. Se ajustó la cantidad.")
            }

            var cambioPrecio = false
            var precioAnterior: Double? = null
            if (item.precioAlAgregar > 0.0 && kotlin.math.abs(item.precioAlAgregar - precioActual) > 0.01) {
                cambioPrecio = true
                precioAnterior = item.precioAlAgregar
                alertas.add("El precio de '$nombre' cambió de S/ ${"%.2f".format(precioAnterior)} a S/ ${"%.2f".format(precioActual)}.")
            }

            val subtotalLinea = redondear2(precioActual * cantidadAprobada)
            if (!estaAgotado && cantidadAprobada > 0) {
                subtotal = redondear2(subtotal + subtotalLinea)
            }

            itemsCalculados.add(
                ItemPedidoCalculado(
                    prendaId = item.prendaId,
                    codigo = item.prendaId,
                    nombre = nombre,
                    fotoUrl = foto,
                    talla = item.talla,
                    cantidadOriginal = item.cantidad,
                    cantidadAprobada = cantidadAprobada,
                    precioUnitario = precioActual,
                    subtotal = subtotalLinea,
                    agotado = estaAgotado,
                    cambioPrecio = cambioPrecio,
                    precioAnterior = precioAnterior,
                )
            )
        }

        // Validación de cupones local (CA-04, CA-05)
        var descuentoTotal = 0.0
        val cuponesValidos = mutableListOf<CuponAplicado>()
        var errorCupon: String? = null
        var exitoCupon: String? = null

        val cuponesConocidos = mapOf(
            "MODA10" to Pair("porcentaje", 10.0),
            "BIENVENIDA" to Pair("monto", 15.0),
            "VERANO20" to Pair("porcentaje", 20.0),
        )

        for (c in cupones) {
            val datos = cuponesConocidos[c]
            if (datos == null) {
                errorCupon = "El cupón '$c' no existe o ha vencido."
                continue
            }
            if (subtotal < 50.0) {
                errorCupon = "El cupón '$c' requiere una compra mínima de S/ 50.00."
                continue
            }
            if (cuponesValidos.isNotEmpty()) {
                errorCupon = "No es posible combinar estos cupones en una sola compra."
                continue
            }

            val (tipo, valor) = datos
            val desc = if (tipo == "porcentaje") {
                redondear2(subtotal * (valor / 100.0))
            } else {
                redondear2(min(subtotal, valor))
            }
            descuentoTotal = redondear2(descuentoTotal + desc)
            cuponesValidos.add(CuponAplicado(c, tipo, valor, desc))
            if (c == codigoRecienAgregado) {
                exitoCupon = "¡Cupón $c aplicado con éxito!"
            }
        }

        // Costo de envío (CA-03)
        val costoEnvio = if (_uiState.value.tipoEntrega == TipoEntregaUi.RECOJO) {
            0.0
        } else {
            _uiState.value.distritoSeleccionado?.costo ?: 8.0
        }

        // Total final (CA-02: total = subtotal - descuento + envio)
        val total = redondear2(max(0.0, subtotal - descuentoTotal + costoEnvio))
        val totalPrendas = itemsCalculados.sumOf { it.cantidadAprobada }

        _uiState.update {
            it.copy(
                calculando = false,
                items = itemsCalculados,
                subtotal = subtotal,
                descuentoTotal = descuentoTotal,
                costoEnvio = costoEnvio,
                total = total,
                totalPrendas = totalPrendas,
                cuponesAplicados = cuponesValidos,
                cuponInput = if (exitoCupon != null) "" else it.cuponInput,
                mensajeErrorCupon = errorCupon,
                mensajeExitoCupon = exitoCupon,
                alertas = alertas,
            )
        }
    }

    private fun redondear2(valor: Double): Double = (valor * 100.0).roundToInt() / 100.0
}
