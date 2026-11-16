package pe.modaviva.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.busquedasDataStore by preferencesDataStore(name = "busquedas")

/**
 * HU-06 — Búsquedas recientes guardadas en el teléfono con DataStore. Se muestran como
 * sugerencias en Buscar; la más reciente va primero y no se repiten.
 */
@Singleton
class BusquedasRecientesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val recientes: Flow<List<String>> = context.busquedasDataStore.data.map { prefs ->
        prefs[CLAVE]?.split(SEPARADOR)?.filter { it.isNotBlank() }.orEmpty()
    }

    suspend fun agregar(texto: String) {
        val limpio = texto.trim().replace(SEPARADOR, " ")
        if (limpio.length < MINIMO) return
        context.busquedasDataStore.edit { prefs ->
            val actuales = prefs[CLAVE]?.split(SEPARADOR)?.filter { it.isNotBlank() }.orEmpty()
            val nuevas = (listOf(limpio) + actuales.filterNot { it.equals(limpio, ignoreCase = true) })
                .take(MAXIMO)
            prefs[CLAVE] = nuevas.joinToString(SEPARADOR)
        }
    }

    suspend fun borrar() {
        context.busquedasDataStore.edit { it.remove(CLAVE) }
    }

    private companion object {
        val CLAVE = stringPreferencesKey("recientes")
        const val SEPARADOR = "\u001F"
        const val MAXIMO = 8
        const val MINIMO = 2
    }
}
