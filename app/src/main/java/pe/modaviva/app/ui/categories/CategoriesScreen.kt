package pe.modaviva.app.ui.categories

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import pe.modaviva.app.R
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.ui.components.GarmentImagePlaceholder
import pe.modaviva.app.ui.components.MvGarmentCard
import pe.modaviva.app.ui.search.SearchViewModel
import java.time.Instant
import java.util.Locale

/** Convierte "casacas-y-abrigos" en "Casacas y abrigos" (solo para mostrar). */
private fun String.paraMostrar(): String =
    replace("-", " ").replaceFirstChar { it.uppercase() }

/** Imagen decorativa de cada género (res/drawable). Si no hay, se muestra la inicial. */
@DrawableRes
private fun imagenGenero(genero: String): Int? = when (genero.lowercase()) {
    "mujer", "mujeres" -> R.drawable.cat_mujer
    "hombre", "hombres" -> R.drawable.cat_hombre
    else -> null
}

@Composable
private fun coloresTarjeta(index: Int): Pair<Color, Color> {
    val colores = MaterialTheme.colorScheme
    return when (index % 3) {
        0 -> colores.secondaryContainer to colores.onSecondaryContainer
        1 -> colores.tertiaryContainer to colores.onTertiaryContainer
        else -> colores.primaryContainer to colores.onPrimaryContainer
    }
}

@Composable
fun CategoriesScreen(
    onPrendaClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val categorias by viewModel.categorias.collectAsStateWithLifecycle()
    val subcategorias by viewModel.subcategorias.collectAsStateWithLifecycle()
    val generos by viewModel.generos.collectAsStateWithLifecycle()
    val resultados by viewModel.resultados.collectAsStateWithLifecycle()
    val categoriaSeleccionada by viewModel.categoriaSeleccionada.collectAsStateWithLifecycle()
    val subcategoriaSeleccionada by viewModel.subcategoriaSeleccionada.collectAsStateWithLifecycle()
    val generoSeleccionado by viewModel.generoSeleccionado.collectAsStateWithLifecycle()

    var verTodo by rememberSaveable { mutableStateOf(false) }

    val mostrarResultados = verTodo ||
            generoSeleccionado != null ||
            categoriaSeleccionada != null ||
            subcategoriaSeleccionada != null

    fun volver() {
        viewModel.seleccionarGenero(null)
        viewModel.seleccionarCategoria(null)
        viewModel.seleccionarSubcategoria(null)
        verTodo = false
    }

    BackHandler(enabled = mostrarResultados) { volver() }

    if (!mostrarResultados) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    text = "Categorías",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }

            item { BannerOferta() }

            itemsIndexed(generos) { index, genero ->
                val (fondo, texto) = coloresTarjeta(index)
                TarjetaGenero(
                    titulo = genero.paraMostrar(),
                    subtitulo = categorias
                        .take(3)
                        .joinToString(", ") { it.paraMostrar() },
                    containerColor = fondo,
                    contentColor = texto,
                    imagenALaIzquierda = index % 2 == 1,
                    imagen = imagenGenero(genero),
                    onClick = { viewModel.seleccionarGenero(genero) },
                )
            }

            item {
                TextButton(
                    onClick = { verTodo = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Ver todas las prendas") }
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 28.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Barra superior con volver
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = ::volver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                    Text(
                        text = generoSeleccionado?.paraMostrar() ?: "Todas las prendas",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }

            // Categorías
            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categorias) { categoria ->
                        FilterChip(
                            selected = categoriaSeleccionada == categoria,
                            onClick = {
                                viewModel.seleccionarCategoria(
                                    if (categoriaSeleccionada == categoria) null else categoria
                                )
                            },
                            label = { Text(categoria.paraMostrar()) },
                        )
                    }
                }
            }

            // Subcategorías (solo si hay)
            if (subcategorias.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(subcategorias) { sub ->
                            FilterChip(
                                selected = subcategoriaSeleccionada == sub,
                                onClick = {
                                    viewModel.seleccionarSubcategoria(
                                        if (subcategoriaSeleccionada == sub) null else sub
                                    )
                                },
                                label = { Text(sub.paraMostrar()) },
                            )
                        }
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "${resultados.size} prendas",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (resultados.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "No encontramos prendas para esta selección.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(items = resultados, key = { it.codigo }) { prenda ->
                    TarjetaPrendaCategoria(
                        prenda = prenda,
                        onClick = { onPrendaClick(prenda.codigo) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BannerOferta() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp)
                .fillMaxWidth(0.6f),
        ) {
            Text(
                text = "Sale",
                style = MaterialTheme.typography.headlineLarge,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Hasta 50% de descuento en prendas seleccionadas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Image(
            painter = painterResource(R.drawable.banner_sale),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(140.dp)
                .padding(end = 8.dp),
        )
    }
}

@Composable
private fun TarjetaGenero(
    titulo: String,
    subtitulo: String,
    containerColor: Color,
    contentColor: Color,
    imagenALaIzquierda: Boolean,
    @DrawableRes imagen: Int?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(MaterialTheme.shapes.large)
            .background(containerColor)
            .clickable(onClick = onClick),
    ) {
        val ladoImagen = if (imagenALaIzquierda) Alignment.CenterStart else Alignment.CenterEnd

        if (imagen != null) {
            Image(
                painter = painterResource(imagen),
                contentDescription = titulo,
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter,
                modifier = Modifier
                    .align(ladoImagen)
                    .fillMaxHeight()
                    .width(160.dp)
                    .padding(horizontal = 10.dp),
            )
        } else {
            Text(
                text = titulo.take(1).uppercase(),
                style = MaterialTheme.typography.displayLarge,
                color = contentColor.copy(alpha = 0.08f),
                modifier = Modifier
                    .align(ladoImagen)
                    .padding(horizontal = 28.dp),
            )
        }

        Column(
            modifier = Modifier
                .align(if (imagenALaIzquierda) Alignment.CenterEnd else Alignment.CenterStart)
                .fillMaxWidth(0.55f)
                .padding(horizontal = 20.dp),
            horizontalAlignment = if (imagenALaIzquierda) Alignment.End else Alignment.Start,
        ) {
            Text(
                text = titulo.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = contentColor,
            )
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.75f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (imagenALaIzquierda) TextAlign.End else TextAlign.Start,
            )
        }
    }
}

@Composable
private fun TarjetaPrendaCategoria(
    prenda: Prenda,
    onClick: () -> Unit,
) {
    val ahora = Instant.now()
    val enPromocion = prenda.enPromocion(ahora)

    val precio = String.format(
        Locale.US,
        "S/ %.2f",
        prenda.precioVigente(ahora),
    )

    val precioOriginal = if (enPromocion) {
        String.format(Locale.US, "S/ %.2f", prenda.precio)
    } else {
        null
    }

    MvGarmentCard(
        name = prenda.nombre,
        brand = prenda.marca,
        price = precio,
        originalPrice = precioOriginal,
        isSoldOut = prenda.agotada,
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