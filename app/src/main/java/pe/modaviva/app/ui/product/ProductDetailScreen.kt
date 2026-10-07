package pe.modaviva.app.ui.product

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlinx.coroutines.launch
import pe.modaviva.app.R
import pe.modaviva.app.ui.stock.StockAvailability
import pe.modaviva.app.ui.stock.StockViewModel
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    codigo: String,
    onBack: () -> Unit,
    onCartClick: () -> Unit = {},
    viewModel: ProductDetailViewModel = hiltViewModel(),
    stockViewModel: StockViewModel = hiltViewModel(),
) {
    val prenda by viewModel
        .obtenerPrenda(codigo)
        .collectAsStateWithLifecycle(initialValue = null)

    val cartItemCount by viewModel.cartItemCount.collectAsStateWithLifecycle()

    var tallaSeleccionada by remember { mutableStateOf<String?>(null) }
    var cantidad by remember { mutableIntStateOf(1) }
    var colorSeleccionado by remember { mutableStateOf(false) }
    var mostrarMedidas by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(tallaSeleccionada) {
        cantidad = 1
        tallaSeleccionada?.let { talla ->
            stockViewModel.observeStock(
                prendaId = codigo,
                talla = talla,
            )
        }
    }

    val stock by stockViewModel.stock.collectAsState()

    if (prenda == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                    )
                }
                IconButton(onClick = onCartClick) {
                    BadgedBox(
                        badge = {
                            if (cartItemCount > 0) {
                                Badge {
                                    Text(text = if (cartItemCount > 99) "99+" else cartItemCount.toString())
                                }
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ShoppingCart,
                            contentDescription = stringResource(R.string.cd_cart),
                        )
                    }
                }
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

            Text(
                text = "Tallas disponibles",
                style = MaterialTheme.typography.titleMedium,
            )

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

            Text(
                text = "Color",
                style = MaterialTheme.typography.titleMedium,
            )

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

            val maxStock = when {
                stock != null -> stock!!.total
                tallaSeleccionada != null -> prenda!!.stockTotal
                else -> prenda!!.stockTotal
            }
            val estaAgotado = tallaSeleccionada != null && stock != null && stock!!.total <= 0

            Text(
                text = "Cantidad",
                style = MaterialTheme.typography.titleMedium,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedIconButton(
                    onClick = {
                        if (cantidad > 1) {
                            cantidad--
                        }
                    },
                    enabled = !estaAgotado && cantidad > 1,
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Disminuir cantidad",
                    )
                }

                Text(
                    text = if (estaAgotado) "0" else cantidad.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                OutlinedIconButton(
                    onClick = {
                        if (cantidad < maxStock) {
                            cantidad++
                        }
                    },
                    enabled = !estaAgotado && cantidad < maxStock,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Aumentar cantidad",
                    )
                }
            }

            // CA-04: Aviso cuando se alcanza el stock máximo disponible
            if (!estaAgotado && tallaSeleccionada != null && maxStock > 0 && cantidad >= maxStock) {
                Text(
                    text = "Solo quedan $maxStock unidades",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else if (estaAgotado) {
                Text(
                    text = "Agotado en esta talla",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(
                onClick = {
                    val talla = tallaSeleccionada ?: return@Button
                    if (!colorSeleccionado) return@Button
                    viewModel.agregarAlCarrito(
                        prenda = prenda!!,
                        talla = talla,
                        cantidad = cantidad,
                        onCompletado = {
                            scope.launch {
                                val resultado = snackbarHostState.showSnackbar(
                                    message = "Se agregó al carrito ($cantidad unidad${if (cantidad > 1) "es" else ""})",
                                    actionLabel = "Ver carrito",
                                    duration = SnackbarDuration.Short,
                                )
                                if (resultado == SnackbarResult.ActionPerformed) {
                                    onCartClick()
                                }
                            }
                        },
                    )
                },
                enabled = tallaSeleccionada != null && colorSeleccionado && !estaAgotado && cantidad in 1..maxStock,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (estaAgotado) "Agotado" else "Agregar al carrito")
            }
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