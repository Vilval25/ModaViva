package pe.modaviva.app.ui.checkout

import pe.modaviva.app.domain.model.CuponAplicado
import pe.modaviva.app.domain.model.ItemPedidoCalculado
import pe.modaviva.app.domain.model.TarifaEnvio

enum class TipoEntregaUi {
    RECOJO,
    DELIVERY,
}

data class CheckoutSummaryUiState(
    val cargando: Boolean = true,
    val calculando: Boolean = false,
    val items: List<ItemPedidoCalculado> = emptyList(),
    val tipoEntrega: TipoEntregaUi = TipoEntregaUi.DELIVERY,
    val distritoSeleccionado: TarifaEnvio? = null,
    val distritosDisponibles: List<TarifaEnvio> = emptyList(),
    val cuponInput: String = "",
    val cuponesAplicados: List<CuponAplicado> = emptyList(),
    val mensajeErrorCupon: String? = null,
    val mensajeExitoCupon: String? = null,
    val subtotal: Double = 0.0,
    val descuentoTotal: Double = 0.0,
    val costoEnvio: Double = 0.0,
    val total: Double = 0.0,
    val alertas: List<String> = emptyList(),
    val errorGeneral: String? = null,
    val totalPrendas: Int = 0,
)
