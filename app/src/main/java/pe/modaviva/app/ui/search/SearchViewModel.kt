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

    private val _categoriaSeleccionada = MutableStateFlow<String?>(null)

    val categoriaSeleccionada: StateFlow<String?> =
        _categoriaSeleccionada

    val categorias: StateFlow<List<String>> = catalogo.prendas
        .combine(_categoriaSeleccionada) { prendas, _ ->
            prendas
                .map { it.categoria }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    val resultados: StateFlow<List<Prenda>> = combine(
        catalogo.prendas,
        _textoBusqueda,
        _categoriaSeleccionada,
    ) { prendas, texto, categoria ->

        val consulta = texto.trim()

        prendas.filter { prenda ->

            val coincideTexto =
                consulta.isBlank() ||
                        prenda.nombre.contains(consulta, ignoreCase = true) ||
                        prenda.marca.contains(consulta, ignoreCase = true)

            val coincideCategoria =
                categoria == null ||
                        prenda.categoria.equals(categoria, ignoreCase = true)

            coincideTexto && coincideCategoria
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    fun actualizarBusqueda(texto: String) {
        _textoBusqueda.value = texto
    }

    fun seleccionarCategoria(categoria: String?) {
        _categoriaSeleccionada.value = categoria
    }
}