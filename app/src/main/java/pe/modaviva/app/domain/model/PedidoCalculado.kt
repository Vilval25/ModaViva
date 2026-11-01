package pe.modaviva.app.domain.model

data class ItemCalculoRequest(
    val prendaId: String,
    val talla: String,
    val cantidad: Int,
    val precioVisto: Double? = null,
)

data class SolicitudCalculoPedido(
    val items: List<ItemCalculoRequest>,
    val tipoEntrega: String, // "recojo" | "delivery"
    val distritoSlug: String? = null,
    val cupones: List<String> = emptyList(),
)

data class ItemPedidoCalculado(
    val prendaId: String,
    val codigo: String,
    val nombre: String,
    val fotoUrl: String,
    val talla: String,
    val cantidadOriginal: Int,
    val cantidadAprobada: Int,
    val precioUnitario: Double,
    val subtotal: Double,
    val agotado: Boolean,
    val cambioPrecio: Boolean,
    val precioAnterior: Double? = null,
)

data class CuponAplicado(
    val codigo: String,
    val tipo: String, // "porcentaje" | "monto"
    val valor: Double,
    val descuentoCalculado: Double,
)

data class TarifaEnvio(
    val id: String,
    val distrito: String,
    val cobertura: Boolean,
    val costo: Double,
    val plazoDias: Int,
)

data class ResultadoCalculoPedido(
    val subtotal: Double,
    val descuentoTotal: Double,
    val costoEnvio: Double,
    val total: Double,
    val tipoEntrega: String,
    val distrito: String,
    val plazoDias: Int,
    val items: List<ItemPedidoCalculado>,
    val cuponesAplicados: List<CuponAplicado>,
    val erroresCupon: List<String>,
    val alertas: List<String>,
    val totalPrendasAprobadas: Int,
)
