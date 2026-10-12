package pe.modaviva.app.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.modaviva.app.domain.repository.FavoritosRepository
import javax.inject.Inject

/**
 * Corazón de las tarjetas y de la ficha (HU-08 CA-01 y CA-08). Vive a nivel
 * de la navegación para que todas las pantallas muestren las mismas marcas.
 */
@HiltViewModel
class MarcarFavoritoViewModel @Inject constructor(
    private val repositorio: FavoritosRepository,
) : ViewModel() {

    /** Códigos de las prendas favoritas de la cuenta (vacío sin sesión). */
    val codigos: StateFlow<Set<String>> = repositorio.favoritos
        .map { lista -> lista.mapTo(HashSet()) { it.prendaId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** Marca o desmarca la prenda. Solo con sesión: la navegación lo controla. */
    fun alternar(codigo: String) {
        viewModelScope.launch {
            if (codigo in codigos.value) repositorio.quitar(codigo) else repositorio.marcar(codigo)
        }
    }

    /** Un invitado tocó el corazón: se marcará si inicia sesión ahora (CA-08). */
    fun marcarAlIniciarSesion(codigo: String) = repositorio.marcarAlIniciarSesion(codigo)

    /** Cerró el aviso sin iniciar sesión. */
    fun olvidarPendiente() = repositorio.marcarAlIniciarSesion(null)
}
