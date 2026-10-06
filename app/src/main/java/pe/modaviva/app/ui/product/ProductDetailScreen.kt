package pe.modaviva.app.ui.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import java.time.Instant
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import pe.modaviva.app.ui.stock.StockViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import pe.modaviva.app.ui.stock.StockAvailability
import androidx.compose.material3.Button
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    codigo: String,
    onBack: () -> Unit,
    onAddToCartClick: () -> Unit = {},
    viewModel: ProductDetailViewModel = hiltViewModel(),
    stockViewModel: StockViewModel = hiltViewModel(),
) {
    val prenda by viewModel
        .obtenerPrenda(codigo)
        .collectAsStateWithLifecycle(initialValue = null)

    var tallaSeleccionada by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tallaSeleccionada) {

        tallaSeleccionada?.let { talla ->
            stockViewModel.observeStock(
                prendaId = codigo,
                talla = talla,
            )
        }
    }

    val stock by stockViewModel.stock.collectAsState()


    var colorSeleccionado by remember { mutableStateOf(false) }
    var mostrarMedidas by remember { mutableStateOf(false) }

    if (prenda == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconButton(
            onClick = onBack,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
            )
        }
        val ahora = Instant.now()
        val fotoPrincipal = prenda!!.fotos.firstOrNull()

        if (fotoPrincipal != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(fotoPrincipal)
                    .build(),
                contentDescription = prenda!!.nombre,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentScale = ContentScale.Fit,
            )
        }

        Text(
            text = prenda!!.nombre,
            style = MaterialTheme.typography.headlineSmall,
        )

        Text(
            text = prenda!!.descripcion,
            style = MaterialTheme.typography.bodyLarge,
        )

        Text(text = "Código: ${prenda!!.codigo}")
        Text(text = "Marca: ${prenda!!.marca}")
        Text(text = "Categoría: ${prenda!!.categoria}")
        Text(text = "Subcategoría: ${prenda!!.subcategoria}")
        if (prenda!!.enPromocion(ahora)) {
            Text(
                text = "S/ ${prenda!!.precioVigente(ahora)}",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Precio original: S/ ${prenda!!.precio}",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Text(
                text = "S/ ${prenda!!.precio}",
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        Text(text = "Tallas disponibles")

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            prenda!!.tallas.forEach { talla ->
                FilterChip(
                    selected = tallaSeleccionada == talla,
                    onClick = {
                        tallaSeleccionada = talla
                    },
                    label = {
                        Text(text = talla)
                    },
                )
            }
        }

        TextButton(
            onClick = {
                mostrarMedidas = true
            },
        ) {
            Text("Ver tabla de medidas")
        }

        Text(text = "Color")

        FilterChip(
            selected = colorSeleccionado,
            onClick = {
                colorSeleccionado = !colorSeleccionado
            },
            label = {
                Text(text = prenda!!.color.nombre)
            },
        )

        stock?.let {
            StockAvailability(
                stockByStore = it.porTienda,
            )
        }

        Button(
            onClick = onAddToCartClick,
            enabled = tallaSeleccionada != null && colorSeleccionado,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Agregar al carrito")
        }
    }

    if (mostrarMedidas) {
        ModalBottomSheet(
            onDismissRequest = {
                mostrarMedidas = false
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Tabla de medidas",
                    style = MaterialTheme.typography.headlineSmall,
                )

                Text(
                    text = "Medidas en centímetros",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                        ) {
                            Text(
                                text = "Talla",
                                modifier = Modifier.width(70.dp),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "Pecho",
                                modifier = Modifier.width(80.dp),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "Cintura",
                                modifier = Modifier.width(80.dp),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "Cadera",
                                modifier = Modifier.width(80.dp),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "Hombro",
                                modifier = Modifier.width(80.dp),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "Largo",
                                modifier = Modifier.width(80.dp),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "Manga",
                                modifier = Modifier.width(80.dp),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }

                        HorizontalDivider()

                        prenda!!.medidas.forEach { (talla, medidas) ->
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                            ) {
                                Text(
                                    text = talla,
                                    modifier = Modifier.width(70.dp),
                                )
                                Text(
                                    text = "${medidas.pecho}",
                                    modifier = Modifier.width(80.dp),
                                )
                                Text(
                                    text = "${medidas.cintura}",
                                    modifier = Modifier.width(80.dp),
                                )
                                Text(
                                    text = "${medidas.cadera}",
                                    modifier = Modifier.width(80.dp),
                                )
                                Text(
                                    text = "${medidas.hombro}",
                                    modifier = Modifier.width(80.dp),
                                )
                                Text(
                                    text = "${medidas.largo}",
                                    modifier = Modifier.width(80.dp),
                                )
                                Text(
                                    text = "${medidas.manga}",
                                    modifier = Modifier.width(80.dp),
                                )
                            }

                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}