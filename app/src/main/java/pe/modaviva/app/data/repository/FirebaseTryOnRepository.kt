package pe.modaviva.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import pe.modaviva.app.data.remote.await
import pe.modaviva.app.domain.model.ResultadoPruebaVirtual
import pe.modaviva.app.domain.model.UsoProbadorHoy
import pe.modaviva.app.domain.repository.TryOnRepository
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseTryOnRepository @Inject constructor(
    private val functions: FirebaseFunctions,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : TryOnRepository {

    override suspend fun probarPrenda(
        prendaId: String,
        talla: String,
        color: String,
        mock: Boolean,
        forzarRegeneracion: Boolean,
    ): Result<ResultadoPruebaVirtual> = runCatching {
        val payload = hashMapOf<String, Any>(
            "prendaId" to prendaId,
            "talla" to talla,
            "color" to color,
            "mock" to mock,
            "forzarRegeneracion" to forzarRegeneracion,
        )

        val res = try {
            functions
                .getHttpsCallable(PROBAR_PRENDA)
                // El servidor corta la IA a los 60 s (HU-12 CA-07); se deja margen para la respuesta.
                .withTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
                .call(payload)
                .await()
                .getData() as? Map<*, *>
        } catch (e: FirebaseFunctionsException) {
            val mensaje = if (e.code == FirebaseFunctionsException.Code.DEADLINE_EXCEEDED && e.message.isNullOrBlank()) {
                "La IA tardó más de 60 segundos en responder. Este intento no se descontó de tus pruebas de hoy."
            } else {
                e.message ?: "No se pudo generar la prueba virtual."
            }
            throw IllegalStateException(mensaje, e)
        } ?: throw IllegalStateException("Respuesta vacía del servidor del probador virtual.")

        val resultadoUrl = res["resultadoUrl"] as? String
            ?: throw IllegalStateException("No se recibió la imagen generada.")

        ResultadoPruebaVirtual(
            resultadoUrl = resultadoUrl,
            fotoOriginalUrl = res["fotoOriginalUrl"] as? String ?: "",
            generationId = res["generationId"] as? String ?: "",
            cached = res["cached"] as? Boolean ?: false,
            isMock = res["isMock"] as? Boolean ?: false,
            mensaje = res["mensaje"] as? String ?: "",
            talla = res["talla"] as? String ?: talla,
            color = res["color"] as? String ?: color,
            restantesHoy = (res["restantesHoy"] as? Number)?.toInt(),
            limiteDiario = (res["limiteDiario"] as? Number)?.toInt(),
            duracionMs = (res["duracionMs"] as? Number)?.toLong(),
        )
    }

    override fun observarUsoHoy(): Flow<UsoProbadorHoy?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        // Mismo día que usa el servidor: el límite se renueva a medianoche de Lima.
        val hoy = LocalDate.now(ZoneId.of("America/Lima")).toString()
        val registro = firestore.collection("clientes").document(uid)
            .collection("probadorUso").document(hoy)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null || !snap.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val usadas = snap.getLong("usadas")?.toInt() ?: 0
                val limite = snap.getLong("limite")?.toInt() ?: return@addSnapshotListener
                trySend(UsoProbadorHoy(usadas = usadas.coerceAtLeast(0), limite = limite))
            }
        awaitClose { registro.remove() }
    }

    private companion object {
        const val PROBAR_PRENDA = "probarPrenda"
        const val TIMEOUT_SEGUNDOS = 90L
    }
}
