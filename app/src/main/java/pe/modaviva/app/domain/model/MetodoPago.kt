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
 * únicamente marca, últimos 4 dígitos, vencimiento y token de la pasarela sandbox.
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

    companion object {
        /**
         * Detecta la marca de la tarjeta según el BIN (primeros dígitos).
         */
        fun detectarMarca(numeroLimpio: String): String {
            return when {
                numeroLimpio.startsWith("4") -> "Visa"
                numeroLimpio.startsWith("5") || numeroLimpio.startsWith("2") -> "Mastercard"
                numeroLimpio.startsWith("34") || numeroLimpio.startsWith("37") -> "Amex"
                numeroLimpio.startsWith("36") || numeroLimpio.startsWith("38") -> "Diners"
                else -> "Visa"
            }
        }

        /**
         * Valida y simula la tokenización en la pasarela sandbox (HU-21 CA-03, CA-04, CA-05).
         * - Si la tarjeta termina en "0000" o el CVV es "000", simula un rechazo de la pasarela
         *   para poder demostrar en vivo los criterios CA-05 y CA-09.
         */
        fun validarYTokenizarSandbox(
            numeroTarjeta: String,
            vencimiento: String,
            cvv: String,
            titular: String,
            tipo: String = "CREDITO",
        ): Result<TarjetaGuardada> {
            val soloDigitos = numeroTarjeta.filter { it.isDigit() }
            if (soloDigitos.length !in 15..16) {
                return Result.failure(IllegalArgumentException("Ingresa un número de tarjeta válido de 15 o 16 dígitos."))
            }
            if (!Regex("""^(0[1-9]|1[0-2])/\d{2}$""").matches(vencimiento.trim())) {
                return Result.failure(IllegalArgumentException("Ingresa la fecha de vencimiento en formato MM/AA (ej. 08/28)."))
            }
            if (cvv.filter { it.isDigit() }.length !in 3..4) {
                return Result.failure(IllegalArgumentException("Ingresa un código CVV válido de 3 o 4 dígitos."))
            }
            if (titular.trim().length < 3) {
                return Result.failure(IllegalArgumentException("Ingresa el nombre del titular de la tarjeta."))
            }

            // Simulación de rechazo de pasarela sandbox (CA-05 y CA-09)
            if (soloDigitos.endsWith("0000") || cvv.trim() == "000") {
                return Result.failure(
                    IllegalStateException("Pasarela Sandbox: Tarjeta rechazada por el banco emisor (fondos insuficientes o tarjeta restringida).")
                )
            }

            val ultimos4 = soloDigitos.takeLast(4)
            val marca = detectarMarca(soloDigitos)
            val token = "tok_sandbox_${marca.lowercase()}_${ultimos4}_${System.currentTimeMillis() % 100000}"

            return Result.success(
                TarjetaGuardada(
                    id = "card_${marca.lowercase()}_$ultimos4",
                    marca = marca,
                    ultimos4 = ultimos4,
                    vencimiento = vencimiento.trim(),
                    titular = titular.trim().uppercase(),
                    tipo = tipo,
                    tokenPasarela = token,
                )
            )
        }
    }
}

