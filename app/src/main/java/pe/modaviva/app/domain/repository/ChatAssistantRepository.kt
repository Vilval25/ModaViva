package pe.modaviva.app.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.modaviva.app.domain.model.MensajeChat
import pe.modaviva.app.domain.model.RespuestaChatAsistente

interface ChatAssistantRepository {
    /**
     * Envía la pregunta al asistente. Si responde, el servidor guarda la pregunta y la respuesta
     * en la conversación (HU-27 CA-04); si falla, no se guarda nada y la app ofrece reintentar.
     */
    suspend fun enviarMensaje(
        mensaje: String,
        historial: List<MensajeChat>,
        prendaCodigo: String? = null,
    ): Result<RespuestaChatAsistente>

    /** HU-27 CA-04: conversación guardada (clientes/{uid}/chat), de la más antigua a la más reciente. */
    fun observarConversacion(): Flow<List<MensajeChat>>

    /** Borra la conversación guardada para empezar una nueva. */
    suspend fun borrarConversacion(): Result<Unit>
}
