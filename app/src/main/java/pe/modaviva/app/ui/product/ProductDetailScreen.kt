package pe.modaviva.app.ui.product

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pe.modaviva.app.R
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.domain.model.RecomendacionTalla
import pe.modaviva.app.ui.component.MvImageGalleryDialog
import pe.modaviva.app.ui.format.formatearSoles
import pe.modaviva.app.ui.product.components.DestinationSelectorBottomSheet
import pe.modaviva.app.ui.product.components.ProductAccordionSections
import pe.modaviva.app.ui.product.components.ProductAddToBagBar
import pe.modaviva.app.ui.product.components.ProductHeroGallery
import pe.modaviva.app.ui.product.components.ProductSizeAndQuantitySection
import pe.modaviva.app.ui.product.components.SizeGuideBottomSheet
import pe.modaviva.app.ui.stock.StockViewModel
import java.time.Instant

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
    val stock by stockViewModel.stock.collectAsState()

    val recomendacionTalla = remember(prenda, currentUser) {
        prenda?.let { viewModel.calcularRecomendacionTalla(it) } ?: RecomendacionTalla.SinDatosSuficientes
    }

    var tallaSeleccionada by remember { mutableStateOf<String?>(null) }
    var cantidad by remember { mutableIntStateOf(1) }
    var mostrarMedidas by remember { mutableStateOf(false) }
    var mostrarGaleria by remember { mutableStateOf(false) }
    var fotoSeleccionada by remember { mutableIntStateOf(0) }
    var mostrarSelectorDestino by remember { mutableStateOf(false) }

    val destinoEntrega by viewModel.destinoEntrega.collectAsStateWithLifecycle()
    val ubicacionEstado by viewModel.ubicacionEstado.collectAsStateWithLifecycle()
    val estimacionEntrega by viewModel.estimacionEntrega.collectAsStateWithLifecycle()
    val resultadoTiendaCercana by viewModel.resultadoTiendaCercana.collectAsStateWithLifecycle()
    val tarifasDisponibles by viewModel.tarifasDisponibles.collectAsStateWithLifecycle()
    val cargandoTarifas by viewModel.cargandoTarifas.collectAsStateWithLifecycle()
    val errorTarifas by viewModel.errorTarifas.collectAsStateWithLifecycle()
    val direccionesGuardadas by viewModel.direccionesGuardadas.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
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
            stockViewModel.observeStock(prendaId = codigo, talla = talla)
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

    val item = prenda
    if (item == null) {
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

    val ahora = Instant.now()
    val stockDoc = tallaSeleccionada?.let { allStocks["${codigo}_$it"] }
    val esCargaPendiente = tallaSeleccionada != null && allStocks.isEmpty()
    val maxStock = stockDoc?.total ?: stock?.total ?: item.stockTotal
    val estaAgotado = tallaSeleccionada != null && !esCargaPendiente && (
        (stockDoc != null && stockDoc.total <= 0) ||
            (stock != null && stock!!.total <= 0) ||
            (stockDoc == null && stock == null && item.stockTotal <= 0)
        )
    val precioVigente = item.precioVigente(ahora)

    val textoBoton = when {
        tallaSeleccionada == null -> "Elige una talla"
        esCargaPendiente -> "Verificando…"
        estaAgotado -> "Agotado"
        else -> "Agregar al carrito"
    }
    val buttonEnabled = tallaSeleccionada != null && !esCargaPendiente && !estaAgotado && cantidad in 1..maxStock

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            ProductAddToBagBar(
                buttonText = textoBoton,
                buttonEnabled = buttonEnabled,
                totalPriceFormatted = formatearSoles(precioVigente * cantidad),
                onClick = {
                    val talla = tallaSeleccionada ?: return@ProductAddToBagBar
                    viewModel.agregarAlCarrito(
                        prenda = item,
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
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
                .verticalScroll(rememberScrollState()),
        ) {
            ProductHeroGallery(
                nombrePrenda = item.nombre,
                codigoPrenda = codigo,
                fotos = item.fotos,
                cartItemCount = cartItemCount,
                onBack = onBack,
                onCartClick = onCartClick,
                onPhotoClick = { pagina ->
                    fotoSeleccionada = pagina
                    mostrarGaleria = true
                },
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ProductEditorialHeader(prenda = item, precioVigente = precioVigente, ahora = ahora)

                ProductSizeAndQuantitySection(
                    codigoPrenda = codigo,
                    prenda = item,
                    recomendacionTalla = recomendacionTalla,
                    tallaSeleccionada = tallaSeleccionada,
                    cantidad = cantidad,
                    maxStock = maxStock,
                    esCargaPendiente = esCargaPendiente,
                    estaAgotado = estaAgotado,
                    allStocks = allStocks,
                    onSelectSize = { talla ->
                        tallaSeleccionada = talla
                        viewModel.seleccionarTalla(codigo, talla)
                    },
                    onOpenSizeGuide = { mostrarMedidas = true },
                    onDecreaseQty = { if (cantidad > 1) cantidad-- },
                    onIncreaseQty = { if (cantidad < maxStock) cantidad++ },
                )

                Spacer(modifier = Modifier.height(4.dp))

                ProductAccordionSections(
                    prenda = item,
                    destinoEntrega = destinoEntrega,
                    estimacionEntrega = estimacionEntrega,
                    resultadoTiendaCercana = resultadoTiendaCercana,
                    stock = stock,
                    tiendas = tiendas,
                    onOpenDestinationSelector = { mostrarSelectorDestino = true },
                )

                Spacer(modifier = Modifier.height(8.dp))

                VirtualFittingSection(
                    prendaHabilitada = prendaHabilitada,
                    tieneConsentimiento = tieneConsentimiento,
                    onTryOnClick = {
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
                )
            }
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
            onConfirm = { destino -> viewModel.actualizarDestino(destino) },
            onGpsRequested = {
                if (viewModel.locationService.tienePermisoUbicacion()) {
                    viewModel.obtenerUbicacionGps()
                } else {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
        )
    }

    if (mostrarMedidas) {
        SizeGuideBottomSheet(
            medidas = item.medidas,
            onDismiss = { mostrarMedidas = false },
        )
    }

    if (mostrarGaleria && item.fotos.isNotEmpty()) {
        MvImageGalleryDialog(
            fotos = item.fotos,
            initialPage = fotoSeleccionada,
            onDismiss = { mostrarGaleria = false },
        )
    }
}

@Composable
private fun ProductEditorialHeader(
    prenda: Prenda,
    precioVigente: Double,
    ahora: Instant,
) {
    Text(
        text = "MODAVIVA ATELIER • ${prenda.marca.uppercase()}",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.tertiary,
        letterSpacing = 1.5.sp,
    )

    Text(
        text = prenda.nombre,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = formatearSoles(precioVigente),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (prenda.enPromocion(ahora)) {
            Text(
                text = formatearSoles(prenda.precio),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = TextDecoration.LineThrough,
            )
        }
    }
}

@Composable
private fun VirtualFittingSection(
    prendaHabilitada: Boolean,
    tieneConsentimiento: Boolean,
    onTryOnClick: () -> Unit,
) {
    if (!prendaHabilitada) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
            }
        }
        return
    }

    OutlinedButton(
        onClick = onTryOnClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(25.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Checkroom,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.fitting_btn_try_on),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
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
