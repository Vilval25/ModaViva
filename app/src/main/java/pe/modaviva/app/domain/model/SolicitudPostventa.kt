package pe.modaviva.app.domain.model

/** Estados de una solicitud de cambio o devolución. Solo el back-office la aprueba o rechaza (HU-24 CA-07). */
enum class EstadoSolicitud(val codigo: String, val etiqueta: String) {
    EN_EVALUACION("EN_EVALUACION", "En evaluación"),
    APROBADA("APROBADA", "Aprobada"),
    RECHAZADA("RECHAZADA", "Rechazada");

    companion object {
        fun desdeCodigo(codigo: String?): EstadoSolicitud =
            entries.firstOrNull { it.codigo.equals(codigo?.trim(), ignoreCase = true) } ?: EN_EVALUACION
    }
}

enum class TipoSolicitud(val codigo: String, val etiqueta: String) {
    CAMBIO("CAMBIO", "Cambio"),
    DEVOLUCION("DEVOLUCION", "Devolución");

    companion object {
        fun desdeCodigo(codigo: String?): TipoSolicitud =
            entries.firstOrNull { it.codigo.equals(codigo?.trim(), ignoreCase = true) } ?: CAMBIO
    }
}

/**
 * Solicitud de cambio o devolución de una prenda de un pedido entregado (HU-24).
 * Se guarda en clientes/{uid}/solicitudes/{id}; un pedido puede tener varias.
 */
data class SolicitudPostventa(
    val id: String,
    val numeroSolicitud: String,
    val pedidoId: String,
    val numeroPedido: String,
    val prendaCodigo: String,
    val prendaNombre: String,
    val prendaFotoUrl: String = "",
    val talla: String,
    val tipo: TipoSolicitud,
    val motivo: String,
    val detalleOtro: String? = null,
    val fotosUrls: List<String> = emptyList(),
    val tuvoPruebaVirtual: Boolean = false,
    val estado: EstadoSolicitud = EstadoSolicitud.EN_EVALUACION,
    val resolucion: String? = null,
    val creadaEnMillis: Long = 0L,
    val resueltaEnMillis: Long? = null,
) {
    val motivoTexto: String
        get() = if (motivo == MOTIVO_OTRO && !detalleOtro.isNullOrBlank()) "Otro: $detalleOtro" else motivo

    companion object {
        const val MOTIVO_OTRO = "Otro"
        const val MAX_FOTOS = 3
        const val MIN_DETALLE_OTRO = 5

        /** Lista de motivos predefinidos (HU-24 CA-02). */
        val MOTIVOS = listOf(
            "La talla no me quedó",
            "El corte o la caída no me favorece",
            "El color es diferente al esperado",
            "Falla de costura o de tela",
            "Llegó una prenda distinta a la pedida",
            MOTIVO_OTRO,
        )
    }
}

/** Datos que el cliente completa en la pantalla de solicitud. */
data class NuevaSolicitud(
    val pedido: Pedido,
    val item: ItemPedido,
    val tipo: TipoSolicitud,
    val motivo: String,
    val detalleOtro: String?,
)
