package pe.modaviva.app.domain.repository

import pe.modaviva.app.domain.model.MensajeChat
import pe.modaviva.app.domain.model.RespuestaChatAsistente

interface ChatAssistantRepository {
    suspend fun enviarMensaje(
        mensaje: String,
        historial: List<MensajeChat>,
        prendaCodigo: String? = null,
    ): Result<RespuestaChatAsistente>
}

