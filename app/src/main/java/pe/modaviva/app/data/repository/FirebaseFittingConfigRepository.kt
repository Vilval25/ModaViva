package pe.modaviva.app.data.repository

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.domain.repository.FittingConfigRepository
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseFittingConfigRepository @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
) : FittingConfigRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _disabledCategories = MutableStateFlow(DEFAULT_DISABLED_CATEGORIES)
    override val disabledCategories: StateFlow<Set<String>> = _disabledCategories.asStateFlow()

    init {
        initRemoteConfig()
    }

    private fun initRemoteConfig() {
        try {
            // CA-03: Intervalo de fetch de máximo 15 minutos (900 segundos)
            val settings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(FETCH_INTERVAL_SECONDS)
                .build()
            remoteConfig.setConfigSettingsAsync(settings)

            val defaults = mapOf<String, Any>(
                KEY_DISABLED_CATEGORIES to DEFAULT_DISABLED_STRING,
            )
            remoteConfig.setDefaultsAsync(defaults)

            // Cargar valores activos de inmediato
            actualizarCategoriasDesdeConfig()

            // Disparar fetchAndActivate asíncrono
            scope.launch {
                fetchAndActivate()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error al inicializar FirebaseRemoteConfig para probador", e)
        }
    }

    override suspend fun fetchAndActivate(): Boolean {
        return try {
            val updated = remoteConfig.fetchAndActivate().await()
            actualizarCategoriasDesdeConfig()
            updated
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo actualizar Remote Config: ${e.message}")
            actualizarCategoriasDesdeConfig()
            false
        }
    }

    private fun actualizarCategoriasDesdeConfig() {
        val configStr = remoteConfig.getString(KEY_DISABLED_CATEGORIES).ifBlank {
            DEFAULT_DISABLED_STRING
        }
        val parsed = parseCategories(configStr)
        _disabledCategories.value = parsed
    }

    override fun isPrendaHabilitadaParaProbador(prenda: Prenda): Boolean {
        return isCategoriaHabilitada(prenda.categoria, prenda.subcategoria)
    }

    override fun isCategoriaHabilitada(categoria: String, subcategoria: String?): Boolean {
        val catNorm = normalizar(categoria)
        val subNorm = subcategoria?.let { normalizar(it) }

        val set = _disabledCategories.value
        if (set.contains(catNorm)) return false
        if (subNorm != null && set.contains(subNorm)) return false

        // Validación parcial por palabras clave (ej. si contiene "interior", "lenceria", "bano")
        return !set.any { dis ->
            catNorm.contains(dis) || (subNorm != null && subNorm.contains(dis))
        }
    }

    companion object {
        private const val TAG = "FittingConfig"
        const val KEY_DISABLED_CATEGORIES = "probador_categorias_deshabilitadas"
        const val FETCH_INTERVAL_SECONDS = 900L // 15 min (CA-03)

        val DEFAULT_DISABLED_STRING = "ropa-interior,lenceria,trajes-de-bano,banadores,bikinis,ropa-de-bano,accesorios,calzado"

        val DEFAULT_DISABLED_CATEGORIES: Set<String> = parseCategories(DEFAULT_DISABLED_STRING)

        fun parseCategories(source: String): Set<String> {
            return source
                .split(",", ";", "\n")
                .map { normalizar(it) }
                .filter { it.isNotBlank() }
                .toSet()
        }

        fun normalizar(input: String): String {
            val sinAcentos = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            return sinAcentos
                .lowercase()
                .trim()
                .replace(" ", "-")
                .replace("_", "-")
        }
    }
}
