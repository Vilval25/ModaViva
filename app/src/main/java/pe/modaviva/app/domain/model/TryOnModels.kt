package pe.modaviva.app.domain.model

data class ResultadoPruebaVirtual(
    val resultadoUrl: String,
    val fotoOriginalUrl: String,
    val generationId: String = "",
    val cached: Boolean = false,
    val isMock: Boolean = false,
    val mensaje: String = "",
)

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

