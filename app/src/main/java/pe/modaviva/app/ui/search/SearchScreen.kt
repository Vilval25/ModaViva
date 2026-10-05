package pe.modaviva.app.ui.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.ui.components.MvGarmentCard
import pe.modaviva.app.ui.format.formatearSoles
import java.time.Clock
import java.time.Instant
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.RangeSlider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onPrendaClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val textoBusqueda by viewModel.textoBusqueda.collectAsStateWithLifecycle()
    val resultados by viewModel.resultados.collectAsStateWithLifecycle()
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

    var mostrarFiltros by remember { mutableStateOf(false) }

    var rangoPrecio by remember {
        mutableStateOf(0f..0f)
    }

    LaunchedEffect(precioMinimo, precioMaximo) {
        if (precioMaximo > precioMinimo) {
            rangoPrecio = precioMinimo.toFloat()..precioMaximo.toFloat()
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = textoBusqueda,
                        onValueChange = viewModel::actualizarBusqueda,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Buscar prendas")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                            )
                        },
                        singleLine = true,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),

        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = categoriaSeleccionada == null,
                    onClick = {
                        viewModel.seleccionarCategoria(null)
                    },
                    label = {
                        Text("Todas")
                    },
                )

                categorias.forEach { categoria ->
                    FilterChip(
                        selected = categoriaSeleccionada == categoria,
                        onClick = {
                            viewModel.seleccionarCategoria(categoria)
                        },
                        label = {
                            Text(categoria)
                        },
                    )
                }
            }
            if (subcategorias.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = subcategoriaSeleccionada == null,
                        onClick = {
                            viewModel.seleccionarSubcategoria(null)
                        },
                        label = {
                            Text("Todas las subcategorías")
                        },
                    )

                    subcategorias.forEach { subcategoria ->
                        FilterChip(
                            selected = subcategoriaSeleccionada == subcategoria,
                            onClick = {
                                viewModel.seleccionarSubcategoria(subcategoria)
                            },
                            label = {
                                Text(subcategoria)
                            },
                        )
                    }
                }
            }

            if (generos.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = generoSeleccionado == null,
                        onClick = {
                            viewModel.seleccionarGenero(null)
                        },
                        label = {
                            Text("Todos los géneros")
                        },
                    )

                    generos.forEach { genero ->
                        FilterChip(
                            selected = generoSeleccionado == genero,
                            onClick = {
                                viewModel.seleccionarGenero(genero)
                            },
                            label = {
                                Text(genero)
                            },
                        )
                    }
                }
            }

            Button(
                onClick = {
                    mostrarFiltros = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("Filtros")
            }

            if (
                categoriaSeleccionada != null ||
                subcategoriaSeleccionada != null ||
                generoSeleccionado != null ||
                marcaSeleccionada != null ||
                tallaSeleccionada != null ||
                colorSeleccionado != null
            ) {

                Button(
                    onClick = {
                        viewModel.limpiarFiltros()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text("Limpiar filtros")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {

                    if (categoriaSeleccionada != null) {
                        FilterChip(
                            selected = true,
                            onClick = {
                                viewModel.seleccionarCategoria(null)
                            },
                            label = {
                                Text("Categoría: $categoriaSeleccionada")
                            },
                        )
                    }

                    if (subcategoriaSeleccionada != null) {
                        FilterChip(
                            selected = true,
                            onClick = {
                                viewModel.seleccionarSubcategoria(null)
                            },
                            label = {
                                Text("Subcategoría: $subcategoriaSeleccionada")
                            },
                        )
                    }

                    if (generoSeleccionado != null) {
                        FilterChip(
                            selected = true,
                            onClick = {
                                viewModel.seleccionarGenero(null)
                            },
                            label = {
                                Text("Género: $generoSeleccionado")
                            },
                        )
                    }

                    if (marcaSeleccionada != null) {
                        FilterChip(
                            selected = true,
                            onClick = {
                                viewModel.seleccionarMarca(null)
                            },
                            label = {
                                Text("Marca: $marcaSeleccionada")
                            },
                        )
                    }

                    if (tallaSeleccionada != null) {
                        FilterChip(
                            selected = true,
                            onClick = {
                                viewModel.seleccionarTalla(null)
                            },
                            label = {
                                Text("Talla: $tallaSeleccionada")
                            },
                        )
                    }

                    if (colorSeleccionado != null) {
                        FilterChip(
                            selected = true,
                            onClick = {
                                viewModel.seleccionarColor(null)
                            },
                            label = {
                                Text("Color: $colorSeleccionado")
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (resultados.isEmpty()) {
                Text(
                    text = if (textoBusqueda.isBlank()) {
                        "No hay prendas disponibles."
                    } else {
                        "No se encontraron prendas."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(24.dp),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        items = resultados,
                        key = { it.codigo },
                    ) { prenda ->
                        TarjetaResultado(
                            prenda = prenda,
                            onClick = {
                                onPrendaClick(prenda.codigo)
                            },
                        )
                    }
                }
            }
        }
    }

    if (mostrarFiltros) {
        ModalBottomSheet(
            onDismissRequest = {
                mostrarFiltros = false
            },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Marca",
                    style = MaterialTheme.typography.titleMedium,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = marcaSeleccionada == null,
                        onClick = {
                            viewModel.seleccionarMarca(null)
                        },
                        label = {
                            Text("Todas")
                        },
                    )

                    marcas.forEach { marca ->
                        FilterChip(
                            selected = marcaSeleccionada == marca,
                            onClick = {
                                viewModel.seleccionarMarca(marca)
                            },
                            label = {
                                Text(marca)
                            },
                        )
                    }
                }

                Text(
                    text = "Rango de precio",
                    style = MaterialTheme.typography.titleMedium,
                )

                Text(
                    text = "S/ ${"%.2f".format(rangoPrecio.start)} - S/ ${"%.2f".format(rangoPrecio.endInclusive)}",
                    style = MaterialTheme.typography.bodyMedium,
                )

                RangeSlider(
                    value = rangoPrecio,
                    onValueChange = { nuevoRango ->
                        rangoPrecio = nuevoRango

                        viewModel.seleccionarRangoPrecio(
                            nuevoRango.start.toDouble(),
                            nuevoRango.endInclusive.toDouble(),
                        )
                    },
                    valueRange = precioMinimo.toFloat()..precioMaximo.toFloat(),
                )

                Text(
                    text = "Talla",
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = tallaSeleccionada == null,
                        onClick = {
                            viewModel.seleccionarTalla(null)
                        },
                        label = {
                            Text("Todas")
                        },
                    )

                    tallas.forEach { talla ->
                        FilterChip(
                            selected = tallaSeleccionada == talla,
                            onClick = {
                                viewModel.seleccionarTalla(talla)
                            },
                            label = {
                                Text(talla)
                            },
                        )
                    }
                }
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.titleMedium,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = colorSeleccionado == null,
                        onClick = {
                            viewModel.seleccionarColor(null)
                        },
                        label = {
                            Text("Todos")
                        },
                    )

                    colores.forEach { color ->
                        FilterChip(
                            selected = colorSeleccionado == color,
                            onClick = {
                                viewModel.seleccionarColor(color)
                            },
                            label = {
                                Text(color)
                            },
                        )
                    }
                }


                Spacer(
                    modifier = Modifier.height(8.dp),
                )

                Button(
                    onClick = {
                        mostrarFiltros = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Aplicar filtros")
                }
            }
        }
    }
}

@Composable
private fun TarjetaResultado(
    prenda: Prenda,
    onClick: () -> Unit,
) {
    val ahora = Instant.now(Clock.systemDefaultZone())

    MvGarmentCard(
        name = prenda.nombre,
        brand = prenda.marca,
        price = formatearSoles(prenda.precioVigente(ahora)),
        originalPrice = if (prenda.enPromocion(ahora)) {
            formatearSoles(prenda.precio)
        } else {
            null
        },
        isSoldOut = prenda.agotada,
        onClick = onClick,
        image = {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(prenda.fotos.firstOrNull())
                    .build(),
                contentDescription = prenda.nombre,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}