package pe.modaviva.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import pe.modaviva.app.domain.model.ResultadoPruebaVirtual
import pe.modaviva.app.domain.model.TipoNotificacion
import pe.modaviva.app.domain.repository.NotificacionRepository
import pe.modaviva.app.domain.repository.TryOnRepository

/**
 * HU-12 CA-04 — Genera la prueba virtual con WorkManager para que siga aunque el cliente salga
 * de la pantalla o cierre la app. Si al terminar el cliente ya no está mirando esa prenda, deja
 * un aviso en Notificaciones (y llega como push) que abre el resultado.
 */
class TryOnWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencias {
        fun tryOnRepository(): TryOnRepository
        fun notificaciones(): NotificacionRepository
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Dependencias::class.java)
        val prendaId = inputData.getString(PRENDA_ID) ?: return Result.failure()
        val nombre = inputData.getString(PRENDA_NOMBRE) ?: prendaId
        val talla = inputData.getString(TALLA).orEmpty()

        return deps.tryOnRepository().probarPrenda(
            prendaId = prendaId,
            talla = talla,
            color = inputData.getString(COLOR).orEmpty(),
            forzarRegeneracion = inputData.getBoolean(FORZAR, false),
        ).fold(
            onSuccess = { r ->
                if (!r.cached && PruebaEnPantalla.prendaVisible != prendaId) {
                    deps.notificaciones().crear(
                        tipo = TipoNotificacion.PRUEBA_VIRTUAL,
                        titulo = "Tu prueba virtual está lista",
                        texto = "Mira cómo te queda: $nombre${if (talla.isNotBlank()) " (talla $talla)" else ""}.",
                        refId = prendaId,
                    )
                }
                Result.success(aDatos(r))
            },
            // Sin reintento automático: el cliente decide si reintenta (el intento no se descontó).
            onFailure = { e ->
                Result.failure(workDataOf(ERROR to (e.message ?: "No se pudo generar la prueba virtual.")))
            },
        )
    }

    companion object {
        private const val PRENDA_ID = "prendaId"
        private const val PRENDA_NOMBRE = "prendaNombre"
        private const val TALLA = "talla"
        private const val COLOR = "color"
        private const val FORZAR = "forzar"
        private const val ERROR = "error"

        fun nombreUnico(prendaId: String) = "probador_$prendaId"

        fun solicitud(prendaId: String, nombre: String, talla: String, color: String, forzar: Boolean) =
            OneTimeWorkRequestBuilder<TryOnWorker>()
                .setInputData(
                    workDataOf(
                        PRENDA_ID to prendaId,
                        PRENDA_NOMBRE to nombre,
                        TALLA to talla,
                        COLOR to color,
                        FORZAR to forzar,
                    ),
                )
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()

        fun error(info: WorkInfo): String =
            info.outputData.getString(ERROR) ?: "No se pudo generar la prueba virtual. Inténtalo nuevamente."

        private fun aDatos(r: ResultadoPruebaVirtual): Data = Data.Builder()
            .putString("resultadoUrl", r.resultadoUrl)
            .putString("fotoOriginalUrl", r.fotoOriginalUrl)
            .putString("generationId", r.generationId)
            .putBoolean("cached", r.cached)
            .putBoolean("isMock", r.isMock)
            .putString("mensaje", r.mensaje)
            .putString("talla", r.talla)
            .putString("color", r.color)
            .putBoolean("guardada", r.guardada)
            .putInt("restantesHoy", r.restantesHoy ?: -1)
            .putInt("limiteDiario", r.limiteDiario ?: -1)
            .putLong("duracionMs", r.duracionMs ?: -1L)
            .build()

        fun resultado(info: WorkInfo): ResultadoPruebaVirtual {
            val d = info.outputData
            return ResultadoPruebaVirtual(
                resultadoUrl = d.getString("resultadoUrl").orEmpty(),
                fotoOriginalUrl = d.getString("fotoOriginalUrl").orEmpty(),
                generationId = d.getString("generationId").orEmpty(),
                cached = d.getBoolean("cached", false),
                isMock = d.getBoolean("isMock", false),
                mensaje = d.getString("mensaje").orEmpty(),
                talla = d.getString("talla").orEmpty(),
                color = d.getString("color").orEmpty(),
                guardada = d.getBoolean("guardada", false),
                restantesHoy = d.getInt("restantesHoy", -1).takeIf { it >= 0 },
                limiteDiario = d.getInt("limiteDiario", -1).takeIf { it >= 0 },
                duracionMs = d.getLong("duracionMs", -1L).takeIf { it >= 0 },
            )
        }
    }
}

/**
 * Prenda cuya pantalla está a la vista. Si la prueba termina mientras el cliente la mira, se
 * muestra el resultado en pantalla y no hace falta un aviso (HU-12 CA-04).
 */
object PruebaEnPantalla {
    @Volatile
    var prendaVisible: String? = null
}
