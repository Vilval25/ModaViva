package pe.modaviva.app.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import pe.modaviva.app.domain.repository.StockRepository
import java.util.concurrent.TimeUnit

/**
 * HU-05 CA-03 — Refresca la copia local del stock cada 15 minutos (el mínimo de WorkManager),
 * trayendo solo los documentos que cambiaron desde la última vez.
 */
class StockSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencias {
        fun stockRepository(): StockRepository
    }

    override suspend fun doWork(): Result {
        val repo = EntryPointAccessors.fromApplication(applicationContext, Dependencias::class.java).stockRepository()
        return repo.sincronizar().fold(
            onSuccess = { cambios ->
                Log.i(TAG, "Stock sincronizado: $cambios variantes actualizadas")
                Result.success()
            },
            onFailure = {
                Log.w(TAG, "No se pudo sincronizar el stock", it)
                Result.retry()
            },
        )
    }

    companion object {
        private const val TAG = "StockSync"
        private const val PERIODICO = "stock_sync_periodico"
        private const val AL_ABRIR = "stock_sync_al_abrir"

        /** Programa la sincronización periódica y una inmediata al abrir la app. */
        fun programar(context: Context) {
            val conRed = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val wm = WorkManager.getInstance(context)
            wm.enqueueUniquePeriodicWork(
                PERIODICO,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<StockSyncWorker>(15, TimeUnit.MINUTES).setConstraints(conRed).build(),
            )
            wm.enqueueUniqueWork(
                AL_ABRIR,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<StockSyncWorker>().setConstraints(conRed).build(),
            )
        }
    }
}
