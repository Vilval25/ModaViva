package pe.modaviva.app.data.repository

import com.google.firebase.functions.FirebaseFunctions
import pe.modaviva.app.data.remote.await
import pe.modaviva.app.domain.model.MensajeChat
import pe.modaviva.app.domain.model.PrendaSugeridaChat
import pe.modaviva.app.domain.model.RespuestaChatAsistente
import pe.modaviva.app.domain.repository.ChatAssistantRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseChatAssistantRepository @Inject constructor(
    private val functions: FirebaseFunctions,
) : ChatAssistantRepository {

    override suspend fun enviarMensaje(
        mensaje: String,
        historial: List<MensajeChat>,
        prendaCodigo: String?,
    ): Result<RespuestaChatAsistente> = runCatching {
        val payload = hashMapOf<String, Any>(
            "mensaje" to mensaje,
            "historial" to historial.takeLast(8).map {
                mapOf(
                    "role" to if (it.esUsuario) "user" else "assistant",
                    "content" to it.texto,
                )
            },
        )
        if (!prendaCodigo.isNullOrBlank()) {
            payload["prendaCodigo"] = prendaCodigo
        }

        val res = functions
            .getHttpsCallable(CHAT_ASISTENTE)
            .withTimeout(45, TimeUnit.SECONDS)
            .call(payload)
            .await()
            .getData() as? Map<*, *>
            ?: throw IllegalStateException("Respuesta vacía del asistente.")

        val respuesta = res["respuesta"] as? String
            ?: throw IllegalStateException("El asistente no devolvió un mensaje.")

        val rawPrendas = res["prendasSugeridas"] as? List<*> ?: emptyList<Any>()
        val sugeridas = rawPrendas.mapNotNull { entry ->
            val m = entry as? Map<*, *> ?: return@mapNotNull null
            val codigo = m["codigo"] as? String ?: return@mapNotNull null
            PrendaSugeridaChat(
                codigo = codigo,
                nombre = m["nombre"] as? String ?: codigo,
                precio = (m["precio"] as? Number)?.toDouble() ?: 0.0,
                fotoUrl = m["fotoUrl"] as? String ?: "",
            )
        }

        RespuestaChatAsistente(
            respuesta = respuesta,
            prendasSugeridas = sugeridas,
        )
    }

    private companion object {
        const val CHAT_ASISTENTE = "chatAsistente"
    }
}

