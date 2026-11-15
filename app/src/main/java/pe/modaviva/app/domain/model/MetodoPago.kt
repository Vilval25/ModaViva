package pe.modaviva.app.domain.model

/**
 * Los 4 medios de pago definidos en el Caso de Estudio ModaViva (HU-21 CA-01).
 */
enum class TipoMedioPago(
    val codigo: String,
    val titulo: String,
    val descripcion: String,
) {
    TARJETA_CREDITO(
        codigo = "TARJETA_CREDITO",
        titulo = "Tarjeta de crédito",
        descripcion = "Visa, Mastercard, American Express o Diners",
    ),
    TARJETA_DEBITO(
        codigo = "TARJETA_DEBITO",
        titulo = "Tarjeta de débito",
        descripcion = "Débito en soles de cualquier banco nacional",
    ),
    BILLETERA_ELECTRONICA(
        codigo = "BILLETERA_ELECTRONICA",
        titulo = "Billetera electrónica (Yape / Plin)",
        descripcion = "Pago inmediato con código de aprobación",
    ),
    CONTRA_ENTREGA(
        codigo = "CONTRA_ENTREGA",
        titulo = "Pago contra entrega / Reserva 30 min",
        descripcion = "Paga al recibir o completa tu pago dentro de 30 minutos",
    );

    val esTarjeta: Boolean
        get() = this == TARJETA_CREDITO || this == TARJETA_DEBITO

    companion object {
        fun desdeCodigo(codigo: String?): TipoMedioPago {
            if (codigo.isNullOrBlank()) return TARJETA_CREDITO
            return entries.firstOrNull { it.codigo.equals(codigo.trim(), ignoreCase = true) }
                ?: TARJETA_CREDITO
        }
    }
}

/**
 * Tarjeta tokenizada guardada en Perfil > Métodos de pago (HU-21 CA-02, CA-03, CA-04).
 * Por seguridad PCI-DSS, en la app y en Firestore NUNCA se guardan el número completo (PAN) ni el CVV:
 * únicamente marca, últimos 4 dígitos, vencimiento y token de la pasarela sandbox. La tarjeta se
 * valida y tokeniza en el servidor (Cloud Functions `crearPedido` y `registrarTarjeta`).
 */
data class TarjetaGuardada(
    val id: String = "",
    val marca: String = "Visa", // "Visa" | "Mastercard" | "Amex"
    val ultimos4: String = "",
    val vencimiento: String = "", // "MM/AA"
    val titular: String = "",
    val tipo: String = "CREDITO", // "CREDITO" | "DEBITO"
    val tokenPasarela: String = "",
    val esPredeterminada: Boolean = false,
) {
    val etiquetaResumen: String
        get() = "$marca •••• $ultimos4"
}

/** Datos de una tarjeta nueva. Solo viajan a la Cloud Function; nunca se guardan en el celular. */
data class DatosTarjetaNueva(
    val numero: String,
    val vencimiento: String,
    val cvv: String,
    val titular: String,
    val tipo: String = "CREDITO", // "CREDITO" | "DEBITO"
)

/** Respuesta de la pasarela al registrar una tarjeta desde Perfil > Métodos de pago (CA-04, CA-05). */
sealed interface ResultadoRegistroTarjeta {
    data class Guardada(val tarjetaId: String, val etiqueta: String) : ResultadoRegistroTarjeta
    data class Rechazada(val motivo: String) : ResultadoRegistroTarjeta
}

/** Solicitud de pago que la app envía a la Cloud Function `crearPedido` (HU-21). */
data class SolicitudPago(
    val idempotencyKey: String,
    val items: List<ItemCalculoRequest>,
    val tipoEntrega: String,
    val distritoSlug: String?,
    val cupones: List<String>,
    val direccion: String?,
    val referencia: String?,
    val distrito: String?,
    val tiendaRecojoId: String?,
    val destinatario: String,
    val telefono: String,
    val medioPago: TipoMedioPago,
    val tarjetaGuardadaId: String? = null,
    val tarjetaNueva: DatosTarjetaNueva? = null,
    val guardarTarjeta: Boolean = false,
    val prendasProbadas: List<String> = emptyList(),
    /** Total mostrado en 'Pagar S/ X'; el servidor no cobra si su cálculo es distinto. */
    val totalEsperado: Double? = null,
)

/** Respuesta de la pasarela y del servidor al intentar pagar. */
sealed interface ResultadoPago {
    data class Aprobado(val pedidoId: String, val numeroPedido: String) : ResultadoPago
    data class Pendiente(val pedidoId: String, val numeroPedido: String) : ResultadoPago
    data class Rechazado(val motivo: String) : ResultadoPago
}
