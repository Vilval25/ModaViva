package pe.modaviva.app.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip

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