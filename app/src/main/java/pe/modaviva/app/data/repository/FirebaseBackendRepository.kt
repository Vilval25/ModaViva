package pe.modaviva.app.data.repository

import com.google.firebase.functions.FirebaseFunctions
import pe.modaviva.app.data.remote.await
import pe.modaviva.app.domain.model.BackendPing
import pe.modaviva.app.domain.repository.BackendRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Acceso a las Cloud Functions del proyecto (functions/ en la raíz del repo).
 */
@Singleton
class FirebaseBackendRepository @Inject constructor(
    private val functions: FirebaseFunctions,
) : BackendRepository {

    override suspend fun ping(): Result<BackendPing> = runCatching {
        val data = functions.getHttpsCallable(PING).call().await().getData() as? Map<*, *>
        check(data?.get("ok") == true) { "Respuesta inesperada de $PING: $data" }
        BackendPing(
            message = data["message"] as? String ?: "",
            serverTime = data["serverTime"] as? String ?: "",
        )
    }

    private companion object {
        const val PING = "ping"
    }
}
