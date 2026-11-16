package pe.modaviva.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : ChatAssistantRepository {

    override fun observarConversacion(): Flow<List<MensajeChat>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registro = coleccion(uid)
            .orderBy("creadoEnMillis", Query.Direction.ASCENDING)
            .limitToLast(MAX_MENSAJES)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                trySend(snap.documents.map { doc -> doc.aMensaje() })
            }
        awaitClose { registro.remove() }
    }

    override suspend fun borrarConversacion(): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching
        val docs = coleccion(uid).get().await().documents
        docs.chunked(400).forEach { grupo ->
            val lote = firestore.batch()
            grupo.forEach { lote.delete(it.reference) }
            lote.commit().await()
        }
    }

    private fun coleccion(uid: String) =
        firestore.collection("clientes").document(uid).collection("chat")

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.aMensaje() = MensajeChat(
        id = id,
        texto = getString("texto").orEmpty(),
        esUsuario = getBoolean("esUsuario") == true,
        timestampMillis = getLong("creadoEnMillis") ?: 0L,
        prendasSugeridas = (get("prendasSugeridas") as? List<Map<String, Any?>>).orEmpty().mapNotNull { m ->
            val codigo = m["codigo"] as? String ?: return@mapNotNull null
            PrendaSugeridaChat(
                codigo = codigo,
                nombre = m["nombre"] as? String ?: codigo,
                precio = (m["precio"] as? Number)?.toDouble() ?: 0.0,
                fotoUrl = m["fotoUrl"] as? String ?: "",
            )
        },
    )

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

        val res = try {
            functions
                .getHttpsCallable(CHAT_ASISTENTE)
                .withTimeout(45, TimeUnit.SECONDS)
                .call(payload)
                .await()
                .getData() as? Map<*, *>
        } catch (e: FirebaseFunctionsException) {
            throw IllegalStateException(e.message ?: "El asistente no está disponible en este momento.", e)
        } ?: throw IllegalStateException("Respuesta vacía del asistente.")

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
        const val MAX_MENSAJES = 60L
    }
}

