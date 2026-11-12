package pe.modaviva.app.domain.model

import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Estados oficiales del pedido según el caso de estudio ModaViva (HU-21, HU-22, HU-23).
 */
enum class EstadoPedido(val codigo: String, val etiqueta: String) {
    PENDIENTE("PENDIENTE", "Pendiente de pago"),
    APROBADO("APROBADO", "Aprobado"),
    EN_PREPARACION("EN_PREPARACION", "En preparación"),
    DESPACHADO("DESPACHADO", "Despachado"),
    ENTREGADO("ENTREGADO", "Entregado"),
    ANULADO("ANULADO", "Anulado");

    companion object {
        fun desdeCodigo(codigo: String?): EstadoPedido {
            if (codigo.isNullOrBlank()) return PENDIENTE
            val limpio = codigo.trim().uppercase()
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U")
                .replace(" ", "_")
            return entries.firstOrNull {
                it.codigo.equals(limpio, ignoreCase = true) ||
                        it.etiqueta.equals(codigo.trim(), ignoreCase = true)
            } ?: PENDIENTE
        }
    }
}

/**
 * Hito dentro de la línea de tiempo vertical de un pedido (HU-23 CA-04).
 */
data class HitoPedido(
    val estado: EstadoPedido,
    val titulo: String,
    val descripcion: String,
    val fechaEpochMillis: Long,
)

/**
 * Prenda incluida dentro de un pedido confirmado o pendiente.
 */
data class ItemPedido(
    val prendaId: String,
    val codigo: String,
    val nombre: String,
    val fotoUrl: String,
    val talla: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double,
    val usoProbadorVirtual: Boolean = false,
)

/**
 * Solicitud de cambio o devolución asociada a un pedido entregado (HU-24).
 */
data class SolicitudPostventa(
    val id: String,
    val numeroSolicitud: String,
    val pedidoId: String,
    val prendaCodigo: String,
    val prendaNombre: String,
    val talla: String,
    val tipo: String, // "CAMBIO" | "DEVOLUCION"
    val motivo: String,
    val detalleOtro: String? = null,
    val fotosUrls: List<String> = emptyList(),
    val tuvoPruebaVirtual: Boolean = false,
    val estado: String = "EN_EVALUACION", // "EN_EVALUACION" | "APROBADA" | "RECHAZADA"
    val resolucion: String? = null,
    val creadaEnMillis: Long = System.currentTimeMillis(),
)

/**
 * Modelo principal de un Pedido de ModaViva (HU-21, HU-22, HU-23, HU-24, HU-25).
 */
data class Pedido(
    val id: String,
    val numeroPedido: String,
    val uidCliente: String,
    val estado: EstadoPedido,
    val tipoEntrega: String, // "delivery" | "recojo"
    val direccionEntrega: String = "",
    val referenciaEntrega: String? = null,
    val distrito: String = "",
    val distritoSlug: String = "",
    val tiendaRecojoId: String? = null,
    val tiendaRecojoNombre: String? = null,
    val tiendaRecojoDireccion: String? = null,
    val tiendaRecojoHorario: String? = null,
    val destinatario: String = "",
    val telefonoContacto: String = "",
    val medioPago: TipoMedioPago = TipoMedioPago.TARJETA_CREDITO,
    val resumenMedioPago: String = "",
    val items: List<ItemPedido> = emptyList(),
    val subtotal: Double = 0.0,
    val descuentoTotal: Double = 0.0,
    val costoEnvio: Double = 0.0,
    val total: Double = 0.0,
    val cuponAplicado: String? = null,
    val creadoEnMillis: Long = System.currentTimeMillis(),
    val actualizadoEnMillis: Long = System.currentTimeMillis(),
    val venceEnMillis: Long? = null,
    val entregadoEnMillis: Long? = null,
    val empresaTransporte: String? = null,
    val numeroSeguimiento: String? = null,
    val listoParaRecoger: Boolean = false,
    val hitos: List<HitoPedido> = emptyList(),
    val solicitudPostventa: SolicitudPostventa? = null,
) {
    val totalPrendas: Int
        get() = items.sumOf { it.cantidad }

    val esRecojoEnTienda: Boolean
        get() = tipoEntrega.equals("recojo", ignoreCase = true)

    /**
     * Segundos restantes para que venza la reserva de 30 minutos de un pedido PENDIENTE (HU-22 CA-01, CA-02).
     */
    fun segundosRestantesPago(ahoraMillis: Long = System.currentTimeMillis()): Long {
        if (estado != EstadoPedido.PENDIENTE) return 0L
        val limite = venceEnMillis ?: (creadoEnMillis + DURACION_RESERVA_MS)
        return ((limite - ahoraMillis) / 1000L).coerceAtLeast(0L)
    }

    /**
     * Indica si el pedido pendiente ya superó los 30 minutos sin pago (HU-22 CA-04).
     */
    fun haVencidoReserva(ahoraMillis: Long = System.currentTimeMillis()): Boolean {
        return estado == EstadoPedido.PENDIENTE && segundosRestantesPago(ahoraMillis) <= 0L
    }

    /**
     * HU-23 CA-08 y HU-24 CA-01:
     * Solo los pedidos en estado ENTREGADO dentro de los últimos 7 días calendario
     * pueden iniciar una solicitud de cambio o devolución.
     */
    fun puedeSolicitarCambioODevolucion(ahoraMillis: Long = System.currentTimeMillis()): Boolean {
        if (estado != EstadoPedido.ENTREGADO) return false
        val fechaEntrega = entregadoEnMillis ?: actualizadoEnMillis
        val diasTranscurridos = ChronoUnit.DAYS.between(
            Instant.ofEpochMilli(fechaEntrega),
            Instant.ofEpochMilli(ahoraMillis),
        )
        return diasTranscurridos in 0..VENTANA_POSTVENTA_DIAS
    }

    companion object {
        const val DURACION_RESERVA_MS: Long = 30L * 60L * 1000L // 30 minutos (HU-22)
        const val VENTANA_POSTVENTA_DIAS: Long = 7L // 7 días calendario (HU-24)
    }
}

