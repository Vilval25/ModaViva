package pe.modaviva.app.domain.model

data class ResultadoPruebaVirtual(
    val resultadoUrl: String,
    val fotoOriginalUrl: String,
    val generationId: String = "",
    val cached: Boolean = false,
    val isMock: Boolean = false,
    val mensaje: String = "",
    /** HU-12 CA-02: talla y color con los que se generó la prueba. */
    val talla: String = "",
    val color: String = "",
    /** HU-12 CA-06: true si el cliente ya eligió Guardar (o es una prueba anterior a esa opción). */
    val guardada: Boolean = false,
    /** HU-12 CA-07: pruebas que quedan hoy (null si la respuesta vino del historial). */
    val restantesHoy: Int? = null,
    val limiteDiario: Int? = null,
    /** Tiempo que tardó la IA, para medir la meta de menos de 30 s (CA-03). */
    val duracionMs: Long? = null,
)

/** Pruebas virtuales usadas hoy (HU-12 CA-07). */
data class UsoProbadorHoy(
    val usadas: Int,
    val limite: Int,
) {
    val restantes: Int get() = (limite - usadas).coerceAtLeast(0)
    val agotado: Boolean get() = restantes == 0
}

data class PrendaSugeridaChat(
    val codigo: String,
    val nombre: String,
    val precio: Double,
    val fotoUrl: String = "",
)

data class MensajeChat(
    val id: String,
    val texto: String,
    val esUsuario: Boolean,
    val timestampMillis: Long = System.currentTimeMillis(),
    val prendasSugeridas: List<PrendaSugeridaChat> = emptyList(),
)

data class RespuestaChatAsistente(
    val respuesta: String,
    val prendasSugeridas: List<PrendaSugeridaChat> = emptyList(),
)

