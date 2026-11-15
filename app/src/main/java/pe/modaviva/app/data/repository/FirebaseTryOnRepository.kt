package pe.modaviva.app.data.repository

import com.google.firebase.functions.FirebaseFunctions
import pe.modaviva.app.data.remote.await
import pe.modaviva.app.domain.model.ResultadoPruebaVirtual
import pe.modaviva.app.domain.repository.TryOnRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseTryOnRepository @Inject constructor(
    private val functions: FirebaseFunctions,
) : TryOnRepository {

    override suspend fun probarPrenda(
        prendaId: String,
        mock: Boolean,
        forzarRegeneracion: Boolean,
    ): Result<ResultadoPruebaVirtual> = runCatching {
        val payload = hashMapOf<String, Any>(
            "prendaId" to prendaId,
            "mock" to mock,
            "forzarRegeneracion" to forzarRegeneracion,
        )

        val res = functions
            .getHttpsCallable(PROBAR_PRENDA)
            .withTimeout(120, TimeUnit.SECONDS)
            .call(payload)
            .await()
            .getData() as? Map<*, *>
            ?: throw IllegalStateException("Respuesta vacía del servidor del probador virtual.")

        val resultadoUrl = res["resultadoUrl"] as? String
            ?: throw IllegalStateException("No se recibió la imagen generada.")

        ResultadoPruebaVirtual(
            resultadoUrl = resultadoUrl,
            fotoOriginalUrl = res["fotoOriginalUrl"] as? String ?: "",
            generationId = res["generationId"] as? String ?: "",
            cached = res["cached"] as? Boolean ?: false,
            isMock = res["isMock"] as? Boolean ?: false,
            mensaje = res["mensaje"] as? String ?: "",
        )
    }

    private companion object {
        const val PROBAR_PRENDA = "probarPrenda"
    }
}

