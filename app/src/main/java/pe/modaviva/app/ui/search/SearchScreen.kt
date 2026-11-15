package pe.modaviva.app.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.ui.components.GarmentImagePlaceholder
import pe.modaviva.app.ui.components.MvEmptyState
import pe.modaviva.app.ui.components.MvGarmentCard
import pe.modaviva.app.ui.favorites.LocalFavoritos
import pe.modaviva.app.ui.format.formatearSoles
import pe.modaviva.app.ui.search.components.ExploreQuickAccess
import pe.modaviva.app.ui.search.components.SearchControlsSection
import pe.modaviva.app.ui.search.components.SearchEditorialBar
import pe.modaviva.app.ui.search.components.SearchFiltersSheet
import pe.modaviva.app.ui.search.components.SearchSortSheet
import java.time.Clock
import java.time.Instant

/**
 * Pestaña unificada de búsqueda y exploración por categorías/públicos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onPrendaClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val textoBusqueda by viewModel.textoBusqueda.collectAsStateWithLifecycle()
    val resultados by viewModel.resultados.collectAsStateWithLifecycle()
    val cargandoMas by viewModel.cargandoMas.collectAsStateWithLifecycle()
    val hayMasResultados by viewModel.hayMasResultados.collectAsStateWithLifecycle()
    val categorias by viewModel.categorias.collectAsStateWithLifecycle()
    val categoriaSeleccionada by viewModel.categoriaSeleccionada.collectAsStateWithLifecycle()
    val subcategorias by viewModel.subcategorias.collectAsStateWithLifecycle()
    val subcategoriaSeleccionada by viewModel.subcategoriaSeleccionada.collectAsStateWithLifecycle()
    val generos by viewModel.generos.collectAsStateWithLifecycle()
    val generoSeleccionado by viewModel.generoSeleccionado.collectAsStateWithLifecycle()
    val marcas by viewModel.marcas.collectAsStateWithLifecycle()
    val marcaSeleccionada by viewModel.marcaSeleccionada.collectAsStateWithLifecycle()
    val tallas by viewModel.tallas.collectAsStateWithLifecycle()
    val tallaSeleccionada by viewModel.tallaSeleccionada.collectAsStateWithLifecycle()
    val colores by viewModel.colores.collectAsStateWithLifecycle()
    val colorSeleccionado by viewModel.colorSeleccionado.collectAsStateWithLifecycle()
    val precioMinimo by viewModel.precioMinimo.collectAsStateWithLifecycle()
    val precioMaximo by viewModel.precioMaximo.collectAsStateWithLifecycle()
    val precioMinSeleccionado by viewModel.precioMinSeleccionado.collectAsStateWithLifecycle()
    val precioMaxSeleccionado by viewModel.precioMaxSeleccionado.collectAsStateWithLifecycle()
    val ordenSeleccionado by viewModel.ordenSeleccionado.collectAsStateWithLifecycle()
    val soloOfertas by viewModel.soloOfertas.collectAsStateWithLifecycle()

    var verTodo by rememberSaveable { mutableStateOf(false) }
    var mostrarFiltros by remember { mutableStateOf(false) }
    var mostrarOrden by remember { mutableStateOf(false) }
    var rangoPrecio by remember { mutableStateOf(0f..0f) }

    val gridState = rememberLazyGridState()
    var usuarioDesplazo by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(precioMinimo, precioMaximo) {
        if (precioMaximo > precioMinimo) {
            rangoPrecio = precioMinimo.toFloat()..precioMaximo.toFloat()
        }
    }

    LaunchedEffect(resultados, cargandoMas) {
        if (cargandoMas) {
            viewModel.finalizarCarga()
        }
    }

    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.firstVisibleItemIndex to
                gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }.collect { (primerIndice, ultimoIndice) ->
            if (primerIndice > 0) usuarioDesplazo = true
            if (
                usuarioDesplazo &&
                hayMasResultados &&
                !cargandoMas &&
                resultados.isNotEmpty() &&
                ultimoIndice != null &&
                ultimoIndice >= gridState.layoutInfo.totalItemsCount - 1
            ) {
                viewModel.cargarSiguientePagina()
            }
        }
    }

    val cantidadFiltrosAvanzados = listOfNotNull(
        marcaSeleccionada,
        tallaSeleccionada,
        colorSeleccionado,
        precioMinSeleccionado ?: precioMaxSeleccionado,
    ).size

    val hayFiltrosActivos = soloOfertas ||
        categoriaSeleccionada != null ||
        subcategoriaSeleccionada != null ||
        generoSeleccionado != null ||
        cantidadFiltrosAvanzados > 0

    val mostrarResultados = verTodo ||
        textoBusqueda.isNotBlank() ||
        hayFiltrosActivos ||
        ordenSeleccionado != "Relevancia"

    fun limpiarTodo() {
        viewModel.actualizarBusqueda("")
        viewModel.limpiarFiltros()
        viewModel.seleccionarOrden("Relevancia")
        verTodo = false
    }

    BackHandler(enabled = mostrarResultados) {
        limpiarTodo()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        SearchEditorialBar(
            query = textoBusqueda,
            onQueryChange = viewModel::actualizarBusqueda,
            onClearQuery = { viewModel.actualizarBusqueda("") },
            modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
        )

        SearchControlsSection(
            mostrarVolverExplorar = mostrarResultados,
            onBackToExplore = ::limpiarTodo,
            cantidadFiltrosActivos = cantidadFiltrosAvanzados,
            ordenSeleccionado = ordenSeleccionado,
            hayFiltrosOBusqueda = textoBusqueda.isNotBlank() || hayFiltrosActivos || ordenSeleccionado != "Relevancia",
            onOpenFilters = { mostrarFiltros = true },
            onOpenSort = { mostrarOrden = true },
            onClearAll = ::limpiarTodo,
            categorias = categorias,
            categoriaSeleccionada = categoriaSeleccionada,
            subcategorias = subcategorias,
            subcategoriaSeleccionada = subcategoriaSeleccionada,
            onSelectCategoria = viewModel::seleccionarCategoria,
            onSelectSubcategoria = viewModel::seleccionarSubcategoria,
        )

        ActiveExtraFilterChips(
            soloOfertas = soloOfertas,
            generoSeleccionado = generoSeleccionado,
            marcaSeleccionada = marcaSeleccionada,
            tallaSeleccionada = tallaSeleccionada,
            colorSeleccionado = colorSeleccionado,
            onClearOfertas = { viewModel.seleccionarSoloOfertas(false) },
            onClearGenero = { viewModel.seleccionarGenero(null) },
            onClearMarca = { viewModel.seleccionarMarca(null) },
            onClearTalla = { viewModel.seleccionarTalla(null) },
            onClearColor = { viewModel.seleccionarColor(null) },
        )

        if (!mostrarResultados) {
            ExploreQuickAccess(
                generos = generos,
                categorias = categorias,
                onOfertasClick = { viewModel.seleccionarSoloOfertas(true) },
                onGeneroClick = { genero -> viewModel.seleccionarGenero(genero) },
                onVerTodoClick = { verTodo = true },
            )
        } else if (resultados.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                MvEmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "Sin resultados",
                    message = if (textoBusqueda.isNotBlank()) {
                        "No encontramos prendas para \"$textoBusqueda\"."
                    } else {
                        "No encontramos prendas con los filtros seleccionados."
                    },
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "${resultados.size} prenda${if (resultados.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                items(items = resultados, key = { it.codigo }) { prenda ->
                    TarjetaResultado(
                        prenda = prenda,
                        onClick = { onPrendaClick(prenda.codigo) },
                    )
                }

                if (cargandoMas) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    if (mostrarOrden) {
        SearchSortSheet(
            ordenSeleccionado = ordenSeleccionado,
            onSelectOrden = { orden ->
                viewModel.seleccionarOrden(orden)
                mostrarOrden = false
            },
            onDismiss = { mostrarOrden = false },
        )
    }

    if (mostrarFiltros) {
        SearchFiltersSheet(
            sheetState = sheetState,
            marcas = marcas,
            marcaSeleccionada = marcaSeleccionada,
            onSelectMarca = viewModel::seleccionarMarca,
            precioMinimo = precioMinimo,
            precioMaximo = precioMaximo,
            rangoPrecio = rangoPrecio,
            onRangoPrecioChange = { nuevoRango ->
                rangoPrecio = nuevoRango
                viewModel.seleccionarRangoPrecio(
                    nuevoRango.start.toDouble(),
                    nuevoRango.endInclusive.toDouble(),
                )
            },
            tallas = tallas,
            tallaSeleccionada = tallaSeleccionada,
            onSelectTalla = viewModel::seleccionarTalla,
            colores = colores,
            colorSeleccionado = colorSeleccionado,
            onSelectColor = viewModel::seleccionarColor,
            onDismiss = { mostrarFiltros = false },
        )
    }
}

@Composable
private fun ActiveExtraFilterChips(
    soloOfertas: Boolean,
    generoSeleccionado: String?,
    marcaSeleccionada: String?,
    tallaSeleccionada: String?,
    colorSeleccionado: String?,
    onClearOfertas: () -> Unit,
    onClearGenero: () -> Unit,
    onClearMarca: () -> Unit,
    onClearTalla: () -> Unit,
    onClearColor: () -> Unit,
) {
    val filtros = listOfNotNull(
        if (soloOfertas) "Ofertas" to onClearOfertas else null,
        generoSeleccionado?.let { "Público: ${it.replaceFirstChar(Char::uppercase)}" to onClearGenero },
        marcaSeleccionada?.let { "Marca: $it" to onClearMarca },
        tallaSeleccionada?.let { "Talla: $it" to onClearTalla },
        colorSeleccionado?.let { "Color: $it" to onClearColor },
    )
    if (filtros.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filtros.forEach { (etiqueta, onClear) ->
            FilterChip(
                selected = true,
                onClick = onClear,
                label = { Text(etiqueta) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Quitar $etiqueta",
                    )
                },
            )
        }
    }
}

@Composable
private fun TarjetaResultado(
    prenda: Prenda,
    onClick: () -> Unit,
) {
    val ahora = Instant.now(Clock.systemDefaultZone())
    val favoritos = LocalFavoritos.current

    MvGarmentCard(
        name = prenda.nombre,
        brand = prenda.marca,
        price = formatearSoles(prenda.precioVigente(ahora)),
        originalPrice = if (prenda.enPromocion(ahora)) formatearSoles(prenda.precio) else null,
        isSoldOut = prenda.agotada,
        isFavorite = favoritos?.contiene(prenda.codigo),
        onFavoriteClick = { favoritos?.alternar(prenda.codigo) },
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        image = {
            val foto = prenda.fotos.firstOrNull()
            if (foto.isNullOrBlank()) {
                GarmentImagePlaceholder(modifier = Modifier.fillMaxSize())
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(foto)
                        .build(),
                    contentDescription = prenda.nombre,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
    )
}
