package pe.modaviva.app.ui.product

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.ui.text.font.FontWeight
import pe.modaviva.app.domain.model.RecomendacionTalla
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
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
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import pe.modaviva.app.domain.model.TipoDestino
import pe.modaviva.app.ui.favorites.LocalFavoritos
import pe.modaviva.app.ui.product.components.DestinationSelectorBottomSheet
import pe.modaviva.app.ui.stock.StockAvailability
import pe.modaviva.app.ui.stock.StockViewModel
import java.time.Instant
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import pe.modaviva.app.ui.component.MvImageGalleryDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
    codigo: String,
    onBack: () -> Unit,
    onCartClick: () -> Unit = {},
    onFittingClick: () -> Unit = {},
    viewModel: ProductDetailViewModel = hiltViewModel(),
    stockViewModel: StockViewModel = hiltViewModel(),
) {
    val prenda by viewModel
        .obtenerPrenda(codigo)
        .collectAsStateWithLifecycle(initialValue = null)

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val tieneConsentimiento = currentUser?.tieneConsentimientoProbador == true
    val disabledCategories by viewModel.disabledCategories.collectAsStateWithLifecycle()
    val prendaHabilitada = remember(prenda, disabledCategories) {
        prenda?.let { viewModel.isPrendaHabilitadaParaProbador(it) } ?: true
    }
    val cartItemCount by viewModel.cartItemCount.collectAsStateWithLifecycle()
    val allStocks by stockViewModel.stocksMap.collectAsStateWithLifecycle()
    val tiendas by stockViewModel.tiendas.collectAsStateWithLifecycle()

    val recomendacionTalla = remember(prenda, currentUser) {
        prenda?.let { viewModel.calcularRecomendacionTalla(it) } ?: RecomendacionTalla.SinDatosSuficientes
    }

    var tallaSeleccionada by remember { mutableStateOf<String?>(null) }
    var cantidad by remember { mutableIntStateOf(1) }
    var mostrarMedidas by remember { mutableStateOf(false) }

    var mostrarGaleria by remember { mutableStateOf(false) }
    var fotoSeleccionada by remember { mutableIntStateOf(0) }

    val destinoEntrega by viewModel.destinoEntrega.collectAsStateWithLifecycle()
    val ubicacionEstado by viewModel.ubicacionEstado.collectAsStateWithLifecycle()
    val estimacionEntrega by viewModel.estimacionEntrega.collectAsStateWithLifecycle()
    val resultadoTiendaCercana by viewModel.resultadoTiendaCercana.collectAsStateWithLifecycle()
    val tarifasDisponibles by viewModel.tarifasDisponibles.collectAsStateWithLifecycle()
    val cargandoTarifas by viewModel.cargandoTarifas.collectAsStateWithLifecycle()
    val errorTarifas by viewModel.errorTarifas.collectAsStateWithLifecycle()
    val direccionesGuardadas by viewModel.direccionesGuardadas.collectAsStateWithLifecycle()
    var mostrarSelectorDestino by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.obtenerUbicacionGps()
        }
    }

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

    LaunchedEffect(recomendacionTalla) {
        if (tallaSeleccionada == null && prenda != null) {
            val sugerida = when (recomendacionTalla) {
                is RecomendacionTalla.TallaExacta -> recomendacionTalla.talla
                is RecomendacionTalla.EntreDosTallas -> recomendacionTalla.tallaMenor
                else -> null
            }
            if (sugerida != null && prenda!!.tallas.contains(sugerida)) {
                tallaSeleccionada = sugerida
                viewModel.seleccionarTalla(codigo, sugerida)
            }
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
                Spacer(Modifier.weight(1f))
                val favoritos = LocalFavoritos.current
                if (favoritos != null) {
                    val esFavorita = favoritos.contiene(codigo)
                    IconButton(onClick = { favoritos.alternar(codigo) }) {
                        Icon(
                            imageVector = if (esFavorita) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(
                                if (esFavorita) R.string.cd_favorite_remove else R.string.cd_favorite_add,
                            ),
                        )
                    }
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

            val fotos = prenda!!.fotos

            val pagerState = rememberPagerState(
                pageCount = { fotos.size },
            )

            if (fotos.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth(),
                    ) { pagina ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(fotos[pagina])
                                .build(),
                            contentDescription = "${prenda!!.nombre}, foto ${pagina + 1}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .clickable {
                                    fotoSeleccionada = pagina
                                    mostrarGaleria = true
                                },
                            contentScale = ContentScale.Fit,
                        )
                    }

                    Text(
                        text = "${pagerState.currentPage + 1} / ${fotos.size}",
                        style = MaterialTheme.typography.bodySmall,
                    )

                    Text(
                        text = "Desliza para ver más fotos. Toca una para ampliarla.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Color:",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = prenda!!.color.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

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

            // Recomendación de talla con IA (HU-15 CA-01, CA-03, CA-04)
            when (recomendacionTalla) {
                is RecomendacionTalla.TallaExacta, is RecomendacionTalla.EntreDosTallas -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        ),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Straighten,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (recomendacionTalla) {
                                        is RecomendacionTalla.TallaExacta -> "Tu talla recomendada: ${recomendacionTalla.talla}"
                                        is RecomendacionTalla.EntreDosTallas -> "Tu talla recomendada: Entre ${recomendacionTalla.tallaMenor} y ${recomendacionTalla.tallaMayor}"
                                        else -> ""
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Recomendación aproximada, no garantiza el ajuste exacto",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
                is RecomendacionTalla.FotoInvalidaParaMedidas -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "No se pudieron calcular tus medidas con exactitud. Puedes registrar una nueva foto en el Probador.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
                RecomendacionTalla.SinDatosSuficientes -> {
                    // No se muestra banner intrusivo, se mantiene la tabla genérica
                }
            }

            Text(
                text = "Tallas disponibles",
                style = MaterialTheme.typography.titleMedium,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                prenda!!.tallas.forEach { talla ->
                    val claveStock = "${codigo}_${talla}"
                    val stockTalla = allStocks[claveStock]
                    val esAgotada = (stockTalla != null && stockTalla.total <= 0) || (stockTalla == null && prenda!!.stockTotal <= 0)

                    val esRecomendada = when (recomendacionTalla) {
                        is RecomendacionTalla.TallaExacta -> talla.equals(recomendacionTalla.talla, ignoreCase = true)
                        is RecomendacionTalla.EntreDosTallas -> talla.equals(recomendacionTalla.tallaMenor, ignoreCase = true) || talla.equals(recomendacionTalla.tallaMayor, ignoreCase = true)
                        else -> false
                    }

                    FilterChip(
                        selected = tallaSeleccionada == talla,
                        onClick = {
                            tallaSeleccionada = talla
                            viewModel.seleccionarTalla(codigo, talla)
                        },
                        enabled = !esAgotada,
                        leadingIcon = if (esRecomendada && !esAgotada) {
                            {
                                Icon(
                                    imageVector = Icons.Outlined.Straighten,
                                    contentDescription = "Talla recomendada",
                                    modifier = Modifier.size(16.dp),
                                    tint = if (tallaSeleccionada == talla) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary,
                                )
                            }
                        } else null,
                        label = {
                            Text(
                                text = when {
                                    esAgotada -> "$talla (Agotado)"
                                    esRecomendada -> "$talla (Recomendada)"
                                    else -> talla
                                },
                                maxLines = 1,
                            )
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

            resultadoTiendaCercana.tiendaRecomendada?.let { recomendada ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Tienda más cercana con stock",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = recomendada.tienda.nombre,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        if (recomendada.tienda.direccion.isNotBlank()) {
                            Text(
                                text = "${recomendada.tienda.direccion}, ${recomendada.tienda.distrito}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        recomendada.distanciaKm?.let { km ->
                            Text(
                                text = "Distancia: ${"%.1f".format(km)} km",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Text(
                            text = "Stock disponible: ${recomendada.stockCantidad} unidad(es)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            if (resultadoTiendaCercana.requiereCoordenadasParaDistancia && resultadoTiendaCercana.tiendasConStock.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Disponibilidad en tiendas",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "Usa tu ubicación GPS para calcular cuál tienda con stock está más cerca.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            stock?.let {
                StockAvailability(
                    stockByStore = it.porTienda,
                    tiendas = tiendas,
                )
            }

            Card(
                onClick = { mostrarSelectorDestino = true },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Destino de entrega",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    val textoDestino = if (destinoEntrega != null && destinoEntrega!!.estaCompleto) {
                        val tipoStr = when (destinoEntrega!!.tipo) {
                            TipoDestino.DIRECCION_REGISTRADA -> "Dirección registrada"
                            TipoDestino.UBICACION_ACTUAL_GPS -> "Ubicación GPS"
                            TipoDestino.OTRA_DIRECCION -> "Ingreso manual"
                        }
                        val dir = if (destinoEntrega!!.direccion.isNotBlank()) "${destinoEntrega!!.direccion}, " else ""
                        "$tipoStr: $dir${destinoEntrega!!.distrito}"
                    } else {
                        "Selecciona tu distrito o dirección de entrega"
                    }
                    Text(
                        text = textoDestino,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (destinoEntrega != null && destinoEntrega!!.estaCompleto)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (mostrarSelectorDestino) {
                DestinationSelectorBottomSheet(
                    currentUser = currentUser,
                    destinoActual = destinoEntrega,
                    ubicacionEstado = ubicacionEstado,
                    tarifas = tarifasDisponibles,
                    direcciones = direccionesGuardadas,
                    cargandoTarifas = cargandoTarifas,
                    errorTarifas = errorTarifas,
                    onRetryTarifas = { viewModel.cargarTarifas() },
                    onDismiss = { mostrarSelectorDestino = false },
                    onConfirm = { destino ->
                        viewModel.actualizarDestino(destino)
                    },
                    onGpsRequested = {
                        if (viewModel.locationService.tienePermisoUbicacion()) {
                            viewModel.obtenerUbicacionGps()
                        } else {
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }
                )
            }

            estimacionEntrega?.let { estimacion ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            estimacion.tieneCobertura && estimacion.tarifaRegistrada -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        }
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Estimación de entrega",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        when {
                            !estimacion.tarifaRegistrada -> {
                                Text(
                                    text = estimacion.mensajeInformativo ?: "Sin tarifa registrada para este distrito",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            !estimacion.tieneCobertura -> {
                                Text(
                                    text = estimacion.mensajeInformativo ?: "Sin cobertura de entrega a domicilio",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            else -> {
                                Text(
                                    text = "Costo de envío: S/ ${"%.2f".format(estimacion.costoEnvio)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = "Plazo estimado: ${estimacion.rangoDiasTexto}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            val stockDoc = if (tallaSeleccionada != null) allStocks["${codigo}_${tallaSeleccionada}"] else null
            val esCargaPendiente = tallaSeleccionada != null && allStocks.isEmpty()
            val maxStock = when {
                stockDoc != null -> stockDoc.total
                stock != null -> stock!!.total
                tallaSeleccionada != null -> prenda!!.stockTotal
                else -> prenda!!.stockTotal
            }
            val estaAgotado = tallaSeleccionada != null && !esCargaPendiente && ((stockDoc != null && stockDoc.total <= 0) || (stock != null && stock!!.total <= 0) || (stockDoc == null && stock == null && prenda!!.stockTotal <= 0))

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
                    enabled = !estaAgotado && !esCargaPendiente && tallaSeleccionada != null && cantidad > 1,
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Disminuir cantidad",
                    )
                }

                Text(
                    text = if (estaAgotado || tallaSeleccionada == null) "0" else cantidad.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                OutlinedIconButton(
                    onClick = {
                        if (cantidad < maxStock) {
                            cantidad++
                        }
                    },
                    enabled = !estaAgotado && !esCargaPendiente && tallaSeleccionada != null && cantidad < maxStock,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Aumentar cantidad",
                    )
                }
            }

            // CA-04: Avisos sobre disponibilidad y stock máximo
            if (!estaAgotado && !esCargaPendiente && tallaSeleccionada != null && maxStock > 0 && cantidad >= maxStock) {
                Text(
                    text = "Solo quedan $maxStock unidades",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else if (estaAgotado && tallaSeleccionada != null) {
                Text(
                    text = "Agotado en esta talla",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else if (esCargaPendiente) {
                Text(
                    text = "Verificando disponibilidad en tienda...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val textoBoton = when {
                tallaSeleccionada == null -> "Elige una talla"
                esCargaPendiente -> "Verificando disponibilidad..."
                estaAgotado -> "Agotado"
                else -> "Agregar al carrito"
            }

            Button(
                onClick = {
                    val talla = tallaSeleccionada ?: return@Button
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
                enabled = tallaSeleccionada != null && !esCargaPendiente && !estaAgotado && cantidad in 1..maxStock,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(textoBoton)
            }

            // HU-16 CA-01 y CA-02: Si la prenda pertenece a una categoría no habilitada,
            // no aparece "Probarme" y se muestra un mensaje breve con la política y enlace a medidas.
            if (!prendaHabilitada) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.fitting_disabled_policy_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.fitting_disabled_policy_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // CA-02: Al tocar 'Ver tabla de medidas', se abre la tabla de medidas de la prenda
                        TextButton(
                            onClick = { mostrarMedidas = true },
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text(
                                text = stringResource(R.string.fitting_view_measurements_table),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            } else {
                // CA-05: Botón "Probarme" con indicador de que requiere consentimiento si no lo ha otorgado
                OutlinedButton(
                    onClick = {
                        if (!tieneConsentimiento) {
                            scope.launch {
                                val res = snackbarHostState.showSnackbar(
                                    message = "El probador virtual requiere consentimiento de fotografía (Ley N° 29733)",
                                    actionLabel = "Ir al probador",
                                    duration = SnackbarDuration.Long,
                                )
                                if (res == SnackbarResult.ActionPerformed) {
                                    onFittingClick()
                                }
                            }
                        } else {
                            onFittingClick()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Checkroom,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.fitting_btn_try_on))
                    if (!tieneConsentimiento) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${stringResource(R.string.fitting_consent_declined_badge)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
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

    if (mostrarGaleria && prenda!!.fotos.isNotEmpty()) {
        MvImageGalleryDialog(
            fotos = prenda!!.fotos,
            initialPage = fotoSeleccionada,
            onDismiss = {
                mostrarGaleria = false
            },
        )
    }
}