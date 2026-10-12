package pe.modaviva.app.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.modaviva.app.domain.model.Favorito
import pe.modaviva.app.domain.model.PrendaRetirada
import pe.modaviva.app.domain.repository.CatalogoRepository
import pe.modaviva.app.domain.repository.FavoritosRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/** Estado de la pantalla Favoritos. */
data class FavoritosUiState(
    val cargando: Boolean = true,
    val favoritos: List<FavoritoUi> = emptyList(),
)

/** Pantalla Favoritos (HU-08 CA-02 a CA-05 y CA-07). */
@HiltViewModel
class FavoritosViewModel @Inject constructor(
    private val repositorio: FavoritosRepository,
    private val catalogo: CatalogoRepository,
    private val reloj: Clock,
) : ViewModel() {

    private val retiradas = MutableStateFlow<Map<String, PrendaRetirada>>(emptyMap())
    private val buscadas = mutableSetOf<String>()

    init {
        // Mantiene al día precio, stock y publicación mientras la pantalla
        // está abierta (CA-03). Sin conexión se queda con la copia local.
        viewModelScope.launch { catalogo.sincronizarEnTiempoReal().catch { }.collect() }
    }

    val uiState: StateFlow<FavoritosUiState> = combine(
        repositorio.favoritos,
        catalogo.prendas,
        catalogo.ultimaActualizacion,
        retiradas,
    ) { favoritos, prendas, ultimaActualizacion, retiradas ->
        // Sin ninguna copia del catálogo aún no se sabe qué sigue publicado.
        if (favoritos.isNotEmpty() && ultimaActualizacion == null) {
            return@combine FavoritosUiState(cargando = true)
        }
        val lista = armarFavoritos(favoritos, prendas, retiradas, Instant.now(reloj))
        buscarRetiradas(lista)
        FavoritosUiState(cargando = false, favoritos = lista)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritosUiState())

    /** Quita la prenda; la pantalla ofrece deshacerlo (CA-05). */
    fun quitar(favorito: Favorito) {
        viewModelScope.launch { repositorio.quitar(favorito.prendaId) }
    }

    fun deshacer(favorito: Favorito) {
        viewModelScope.launch { repositorio.restaurar(favorito) }
    }

    /** Pide una sola vez el nombre y la foto de cada favorita retirada. */
    private fun buscarRetiradas(lista: List<FavoritoUi>) {
        lista.filter { !it.disponible && buscadas.add(it.codigo) }.forEach { item ->
            viewModelScope.launch {
                val retirada = repositorio.buscarRetirada(item.codigo) ?: return@launch
                retiradas.update { it + (item.codigo to retirada) }
            }
        }
    }
}
