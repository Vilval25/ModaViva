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
import kotlinx.coroutines.flow.map

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val catalogo: CatalogoRepository,
) : ViewModel() {

    private val _textoBusqueda = MutableStateFlow("")

    val textoBusqueda: StateFlow<String> = _textoBusqueda

    private val _categoriaSeleccionada = MutableStateFlow<String?>(null)

    val categoriaSeleccionada: StateFlow<String?> =
        _categoriaSeleccionada

    private val _subcategoriaSeleccionada = MutableStateFlow<String?>(null)

    val subcategoriaSeleccionada: StateFlow<String?> =
        _subcategoriaSeleccionada

    private val _generoSeleccionado = MutableStateFlow<String?>(null)

    val generoSeleccionado: StateFlow<String?> =
        _generoSeleccionado

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

    val subcategorias: StateFlow<List<String>> = combine(
        catalogo.prendas,
        _categoriaSeleccionada,
    ) { prendas, categoria ->

        prendas
            .filter { prenda ->
                categoria == null ||
                        prenda.categoria.equals(categoria, ignoreCase = true)
            }
            .map { it.subcategoria }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val generos: StateFlow<List<String>> = catalogo.prendas
        .map { prendas ->
            prendas
                .mapNotNull { it.genero }
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
        _subcategoriaSeleccionada,
        _generoSeleccionado,
    ) { prendas, texto, categoria, subcategoria, genero ->

        val consulta = texto.trim()

        prendas.filter { prenda ->

            val coincideTexto =
                consulta.isBlank() ||
                        prenda.nombre.contains(consulta, ignoreCase = true) ||
                        prenda.marca.contains(consulta, ignoreCase = true)

            val coincideCategoria =
                categoria == null ||
                        prenda.categoria.equals(categoria, ignoreCase = true)

            val coincideSubcategoria =
                subcategoria == null ||
                        prenda.subcategoria.equals(
                            subcategoria,
                            ignoreCase = true,
                        )

            val coincideGenero =
                genero == null ||
                        prenda.genero.equals(
                            genero,
                            ignoreCase = true,
                        )

            coincideTexto &&
                    coincideCategoria &&
                    coincideSubcategoria &&
                    coincideGenero
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

        _subcategoriaSeleccionada.value = null
    }

    fun seleccionarSubcategoria(subcategoria: String?) {
        _subcategoriaSeleccionada.value = subcategoria
    }

    fun seleccionarGenero(genero: String?) {
        _generoSeleccionado.value = genero
    }
}