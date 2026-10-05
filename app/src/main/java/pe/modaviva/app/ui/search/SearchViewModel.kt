package pe.modaviva.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.domain.repository.CatalogoRepository
import java.text.Normalizer

private data class FiltrosA(
    val texto: String,
    val categoria: String?,
    val subcategoria: String?,
    val genero: String?,
    val marca: String?,
)

private data class FiltrosB(
    val talla: String?,
    val color: String?,
    val precioMin: Double?,
    val precioMax: Double?,
)

private fun normalizarTexto(texto: String): String {
    return Normalizer
        .normalize(texto, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .lowercase()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val catalogo: CatalogoRepository,
) : ViewModel() {

    private val _textoBusqueda = MutableStateFlow("")
    val textoBusqueda: StateFlow<String> = _textoBusqueda

    private val _categoriaSeleccionada = MutableStateFlow<String?>(null)
    val categoriaSeleccionada: StateFlow<String?> = _categoriaSeleccionada

    private val _subcategoriaSeleccionada = MutableStateFlow<String?>(null)
    val subcategoriaSeleccionada: StateFlow<String?> = _subcategoriaSeleccionada

    private val _generoSeleccionado = MutableStateFlow<String?>(null)
    val generoSeleccionado: StateFlow<String?> = _generoSeleccionado

    private val _marcaSeleccionada = MutableStateFlow<String?>(null)
    val marcaSeleccionada: StateFlow<String?> = _marcaSeleccionada

    private val _tallaSeleccionada = MutableStateFlow<String?>(null)
    val tallaSeleccionada: StateFlow<String?> = _tallaSeleccionada

    private val _colorSeleccionado = MutableStateFlow<String?>(null)
    val colorSeleccionado: StateFlow<String?> = _colorSeleccionado

    private val _precioMinSeleccionado = MutableStateFlow<Double?>(null)
    val precioMinSeleccionado: StateFlow<Double?> = _precioMinSeleccionado

    private val _precioMaxSeleccionado = MutableStateFlow<Double?>(null)
    val precioMaxSeleccionado: StateFlow<Double?> = _precioMaxSeleccionado

    private val _ordenSeleccionado = MutableStateFlow("Relevancia")
    val ordenSeleccionado: StateFlow<String> = _ordenSeleccionado

    val precioMinimo: StateFlow<Double> = catalogo.prendas
        .map { prendas -> prendas.minOfOrNull { it.precio } ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val precioMaximo: StateFlow<Double> = catalogo.prendas
        .map { prendas -> prendas.maxOfOrNull { it.precio } ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val categorias: StateFlow<List<String>> = catalogo.prendas
        .map { prendas ->
            prendas
                .map { it.categoria }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val generos: StateFlow<List<String>> = catalogo.prendas
        .map { prendas ->
            prendas
                .mapNotNull { it.genero }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val marcas: StateFlow<List<String>> = catalogo.prendas
        .map { prendas ->
            prendas
                .map { it.marca }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tallas: StateFlow<List<String>> = catalogo.prendas
        .map { prendas ->
            prendas
                .flatMap { it.tallas }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val colores: StateFlow<List<String>> = catalogo.prendas
        .map { prendas ->
            prendas
                .map { it.color.nombre }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Deben declararse ANTES de "resultados" (se inicializan en orden)
    private val filtrosA = combine(
        _textoBusqueda,
        _categoriaSeleccionada,
        _subcategoriaSeleccionada,
        _generoSeleccionado,
        _marcaSeleccionada,
    ) { texto, categoria, subcategoria, genero, marca ->
        FiltrosA(texto, categoria, subcategoria, genero, marca)
    }

    private val filtrosB = combine(
        _tallaSeleccionada,
        _colorSeleccionado,
        _precioMinSeleccionado,
        _precioMaxSeleccionado,
    ) { talla, color, precioMin, precioMax ->
        FiltrosB(talla, color, precioMin, precioMax)
    }

    val resultados: StateFlow<List<Prenda>> = combine(
        catalogo.prendas,
        filtrosA,
        filtrosB,
        _ordenSeleccionado,
    ) { prendas, a, b, orden  ->

        val consulta = normalizarTexto(a.texto.trim())

        val filtradas = prendas.filter { prenda ->

            val nombre = normalizarTexto(prenda.nombre)
            val marca = normalizarTexto(prenda.marca)
            val categoria = normalizarTexto(prenda.categoria)

            val coincideTexto =
                consulta.isBlank() ||
                        (consulta.length >= 2 &&
                                (
                                        nombre.contains(consulta) ||
                                                marca.contains(consulta) ||
                                                categoria.contains(consulta)
                                        ))

            val coincideCategoria = a.categoria == null ||
                    prenda.categoria.equals(a.categoria, ignoreCase = true)

            val coincideSubcategoria = a.subcategoria == null ||
                    prenda.subcategoria.equals(a.subcategoria, ignoreCase = true)

            val coincideGenero = a.genero == null ||
                    prenda.genero.equals(a.genero, ignoreCase = true)

            val coincideMarca = a.marca == null ||
                    prenda.marca.equals(a.marca, ignoreCase = true)

            val coincidePrecio =
                (b.precioMin == null || prenda.precio >= b.precioMin) &&
                        (b.precioMax == null || prenda.precio <= b.precioMax)

            val coincideTalla = b.talla == null ||
                    prenda.tallas.any { it.equals(b.talla, ignoreCase = true) }

            val coincideColor = b.color == null ||
                    prenda.color.nombre.equals(b.color, ignoreCase = true)

            coincideTexto &&
                    coincideCategoria &&
                    coincideSubcategoria &&
                    coincideGenero &&
                    coincideMarca &&
                    coincidePrecio &&
                    coincideTalla &&
                    coincideColor
        }
        when (orden) {
            "Precio menor a mayor" ->
                filtradas.sortedBy { it.precio }

            "Precio mayor a menor" ->
                filtradas.sortedByDescending { it.precio }

            "Más recientes" -> {
                filtradas.sortedByDescending { it.creadaEn }
            }

            else ->
                filtradas
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

    fun seleccionarMarca(marca: String?) {
        _marcaSeleccionada.value = marca
    }

    fun seleccionarTalla(talla: String?) {
        _tallaSeleccionada.value = talla
    }

    fun seleccionarColor(color: String?) {
        _colorSeleccionado.value = color
    }

    fun seleccionarRangoPrecio(minimo: Double, maximo: Double) {
        _precioMinSeleccionado.value = minimo
        _precioMaxSeleccionado.value = maximo
    }

    fun limpiarFiltros() {
        _categoriaSeleccionada.value = null
        _subcategoriaSeleccionada.value = null
        _generoSeleccionado.value = null
        _marcaSeleccionada.value = null
        _tallaSeleccionada.value = null
        _colorSeleccionado.value = null
        _precioMinSeleccionado.value = null
        _precioMaxSeleccionado.value = null
    }

    fun seleccionarOrden(orden: String) {
        _ordenSeleccionado.value = orden
    }
}