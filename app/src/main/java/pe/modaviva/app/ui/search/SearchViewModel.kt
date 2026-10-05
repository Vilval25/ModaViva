package pe.modaviva.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.domain.repository.CatalogoRepository

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val catalogo: CatalogoRepository,
) : ViewModel() {

    private val _textoBusqueda = MutableStateFlow("")

    val textoBusqueda: StateFlow<String> = _textoBusqueda

    val resultados: StateFlow<List<Prenda>> = combine(
        catalogo.prendas,
        _textoBusqueda,
    ) { prendas, texto ->
        val consulta = texto.trim()

        if (consulta.isBlank()) {
            prendas
        } else {
            prendas.filter { prenda ->
                prenda.nombre.contains(consulta, ignoreCase = true) ||
                        prenda.marca.contains(consulta, ignoreCase = true)
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    fun actualizarBusqueda(texto: String) {
        _textoBusqueda.value = texto
    }
}