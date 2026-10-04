package pe.modaviva.app

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pe.modaviva.app.domain.repository.BackendRepository
import javax.inject.Inject

@HiltAndroidApp
class ModaVivaApp : Application() {

    @Inject lateinit var backendRepository: BackendRepository

    private val appScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) checkBackend()
    }

    /**
     * Prueba de humo del backend (HT-02 CA-05), solo en builds de debug: llama
     * a la Cloud Function `ping` al abrir la app y deja el resultado en Logcat
     * con la etiqueta [TAG]. No afecta a la app si falla.
     */
    private fun checkBackend() {
        appScope.launch {
            backendRepository.ping()
                .onSuccess { Log.i(TAG, "Backend OK: ${it.message} (${it.serverTime})") }
                .onFailure { Log.w(TAG, "Backend no responde", it) }
        }
    }

    private companion object {
        const val TAG = "ModaVivaBackend"
    }
}
