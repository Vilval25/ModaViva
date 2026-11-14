package pe.modaviva.app.ui.product

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlinx.coroutines.launch
import pe.modaviva.app.R
import pe.modaviva.app.domain.model.RecomendacionTalla
import pe.modaviva.app.domain.model.TipoDestino
import pe.modaviva.app.ui.component.MvImageGalleryDialog
import pe.modaviva.app.ui.favorites.LocalFavoritos
import pe.modaviva.app.ui.format.formatearSoles
import pe.modaviva.app.ui.product.components.DestinationSelectorBottomSheet
import pe.modaviva.app.ui.stock.StockAvailability
import pe.modaviva.app.ui.stock.StockViewModel
import java.time.Instant

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

    // Accordion state management
    var detailsExpanded by remember { mutableStateOf(true) }
    var fabricExpanded by remember { mutableStateOf(false) }
    var deliveryExpanded by remember { mutableStateOf(false) }

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

    val ahora = Instant.now()
    val fotos = prenda!!.fotos
    val stockDoc = if (tallaSeleccionada != null) allStocks["${codigo}_${tallaSeleccionada}"] else null
    val esCargaPendiente = tallaSeleccionada != null && allStocks.isEmpty()
    val maxStock = when {
        stockDoc != null -> stockDoc.total
        stock != null -> stock!!.total
        tallaSeleccionada != null -> prenda!!.stockTotal
        else -> prenda!!.stockTotal
    }
    val estaAgotado = tallaSeleccionada != null && !esCargaPendiente && ((stockDoc != null && stockDoc.total <= 0) || (stock != null && stock!!.total <= 0) || (stockDoc == null && stock == null && prenda!!.stockTotal <= 0))

    val precioVigente = prenda!!.precioVigente(ahora)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Sticky Floating Pill Bottom Bar (Figma Reference)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                color = Color.Transparent,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Cápsula principal de acción
                    val textoBoton = when {
                        tallaSeleccionada == null -> "Elige una talla"
                        esCargaPendiente -> "Verificando…"
                        estaAgotado -> "Agotado"
                        else -> "Agregar a la bolsa"
                    }

                    val buttonEnabled = tallaSeleccionada != null && !esCargaPendiente && !estaAgotado && cantidad in 1..maxStock

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .clickable(enabled = buttonEnabled) {
                                val talla = tallaSeleccionada ?: return@clickable
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
                        color = if (buttonEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (buttonEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = RoundedCornerShape(28.dp),
                        shadowElevation = if (buttonEnabled) 4.dp else 0.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = textoBoton,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(18.dp)
                                        .background(
                                            if (buttonEnabled) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                        ),
                                )
                                Text(
                                    text = formatearSoles(precioVigente * cantidad),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
                .verticalScroll(rememberScrollState()),
        ) {
            // HERO IMAGE CON OVERLAYS LIMPIOS (ESTILO FIGMA)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                val pagerState = rememberPagerState(pageCount = { fotos.size })

                if (fotos.isNotEmpty()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                    ) { pagina ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(fotos[pagina])
                                .build(),
                            contentDescription = "${prenda!!.nombre}, foto ${pagina + 1}",
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    fotoSeleccionada = pagina
                                    mostrarGaleria = true
                                },
                            contentScale = ContentScale.Crop,
                        )
                    }

                    // Degradado sutil inferior para que las líneas blancas del indicador siempre contrasten
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f)),
                                ),
                            ),
                    )

                    // OVERLAYS INFERIORES (LÍNEAS DEL CARRUSEL E INSIGNIA 1 / N)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            repeat(fotos.size) { index ->
                                val isSelected = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .height(3.dp)
                                        .width(if (isSelected) 28.dp else 16.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            if (isSelected) Color.White
                                            else Color.White.copy(alpha = 0.5f),
                                        ),
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.55f),
                            contentColor = Color.White,
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} / ${fotos.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                // BOTONES FLOTANTES SUPERIORES (VOLVER, FAVORITO, CARRITO) CON INSET DE BARRA DE ESTADO
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val overlayBg = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.92f)
                    val overlayFg = MaterialTheme.colorScheme.onSurface

                    Surface(
                        onClick = onBack,
                        shape = CircleShape,
                        color = overlayBg,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = overlayFg,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val favoritos = LocalFavoritos.current
                        if (favoritos != null) {
                            val esFavorita = favoritos.contiene(codigo)
                            Surface(
                                onClick = { favoritos.alternar(codigo) },
                                shape = CircleShape,
                                color = overlayBg,
                                shadowElevation = 2.dp,
                                modifier = Modifier.size(40.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (esFavorita) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = stringResource(
                                            if (esFavorita) R.string.cd_favorite_remove else R.string.cd_favorite_add,
                                        ),
                                        tint = if (esFavorita) MaterialTheme.colorScheme.error else overlayFg,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }

                        Surface(
                            onClick = onCartClick,
                            shape = CircleShape,
                            color = overlayBg,
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
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
                                        tint = overlayFg,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECCIÓN EDITORIAL DE DETALLE DE PRENDA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Etiqueta superior en terracota
                Text(
                    text = "MODAVIVA ATELIER • ${prenda!!.marca.uppercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    letterSpacing = 1.5.sp,
                )

                // Título editorial (Bodoni Moda)
                Text(
                    text = prenda!!.nombre,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // Precio vigente formateado
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
                    if (prenda!!.enPromocion(ahora)) {
                        Text(
                            text = formatearSoles(prenda!!.precio),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough,
                        )
                    }
                }

                // Descripción corta
                if (prenda!!.descripcion.isNotBlank()) {
                    Text(
                        text = prenda!!.descripcion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                    )
                }

                // IA RECOMENDACION DE TALLA BANNER (HU-15)
                when (recomendacionTalla) {
                    is RecomendacionTalla.TallaExacta, is RecomendacionTalla.EntreDosTallas -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Straighten,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = when (recomendacionTalla) {
                                            is RecomendacionTalla.TallaExacta -> "Tu talla recomendada: ${recomendacionTalla.talla}"
                                            is RecomendacionTalla.EntreDosTallas -> "Tu talla recomendada: Entre ${recomendacionTalla.tallaMenor} y ${recomendacionTalla.tallaMayor}"
                                            else -> ""
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                    Text(
                                        text = "Calculada por IA según tus medidas registradas",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                    )
                                }
                            }
                        }
                    }
                    is RecomendacionTalla.FotoInvalidaParaMedidas -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "No se pudieron calcular tus medidas con exactitud. Registra una nueva foto en el Probador.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                    RecomendacionTalla.SinDatosSuficientes -> {}
                }

                Spacer(modifier = Modifier.height(4.dp))

                // SIZE SELECTION HEADER & PILLS (FIGMA REFERENCE)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Elige tu talla",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    Text(
                        text = "Guía de tallas",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable { mostrarMedidas = true },
                    )
                }

                val renderSizePill: @Composable (String, Modifier) -> Unit = { talla, pillModifier ->
                    val claveStock = "${codigo}_${talla}"
                    val stockTalla = allStocks[claveStock]
                    val esAgotada = (stockTalla != null && stockTalla.total <= 0) || (stockTalla == null && prenda!!.stockTotal <= 0)
                    val isSelected = tallaSeleccionada == talla

                    Surface(
                        onClick = {
                            if (!esAgotada) {
                                tallaSeleccionada = talla
                                viewModel.seleccionarTalla(codigo, talla)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            esAgotada -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f)
                            else -> MaterialTheme.colorScheme.surfaceContainerLowest
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            },
                        ),
                        enabled = !esAgotada,
                        modifier = pillModifier.height(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = talla,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    esAgotada -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                textDecoration = if (esAgotada) TextDecoration.LineThrough else TextDecoration.None,
                            )
                        }
                    }
                }

                // SIZE PILLS ROW (distribuidas de forma uniforme cuando caben en una fila)
                if (prenda!!.tallas.size <= 6) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        prenda!!.tallas.forEach { talla ->
                            renderSizePill(talla, Modifier.weight(1f))
                        }
                    }
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        prenda!!.tallas.forEach { talla ->
                            renderSizePill(talla, Modifier.width(60.dp))
                        }
                    }
                }

                // QUANTITY CONTROLS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Cantidad",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        OutlinedIconButton(
                            onClick = { if (cantidad > 1) cantidad-- },
                            enabled = !estaAgotado && !esCargaPendiente && tallaSeleccionada != null && cantidad > 1,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Disminuir cantidad",
                                modifier = Modifier.size(16.dp),
                            )
                        }

                        Text(
                            text = if (estaAgotado || tallaSeleccionada == null) "0" else cantidad.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )

                        OutlinedIconButton(
                            onClick = { if (cantidad < maxStock) cantidad++ },
                            enabled = !estaAgotado && !esCargaPendiente && tallaSeleccionada != null && cantidad < maxStock,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Aumentar cantidad",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                // STOCK STATUS WARNING
                if (!estaAgotado && !esCargaPendiente && tallaSeleccionada != null && maxStock > 0 && cantidad >= maxStock) {
                    Text(
                        text = "Solo quedan $maxStock unidades disponibles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else if (estaAgotado && tallaSeleccionada != null) {
                    Text(
                        text = "Agotado en esta talla",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ACCORDION SECTIONS (FIGMA REFERENCE: Detalles, Material y cuidado, Envío y devoluciones)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // 1. Detalles Accordion Item
                    AccordionItem(
                        title = "Detalles",
                        isExpanded = detailsExpanded,
                        onToggle = { detailsExpanded = !detailsExpanded },
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 12.dp),
                        ) {
                            Text(
                                text = prenda!!.descripcion,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Categoría: ${prenda!!.categoria} / ${prenda!!.subcategoria}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Color: ${prenda!!.color.nombre}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Código de producto: ${prenda!!.codigo}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // 2. Material y cuidado Accordion Item
                    AccordionItem(
                        title = "Material y cuidado",
                        isExpanded = fabricExpanded,
                        onToggle = { fabricExpanded = !fabricExpanded },
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 12.dp),
                        ) {
                            Text(
                                text = "Confección de alta calidad. Lavado en seco recomendado. Planchar a baja temperatura con paño protector.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // 3. Envío y devoluciones Accordion Item
                    AccordionItem(
                        title = "Envío y devoluciones",
                        isExpanded = deliveryExpanded,
                        onToggle = { deliveryExpanded = !deliveryExpanded },
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 12.dp),
                        ) {
                            // Selector de Destino
                            Card(
                                onClick = { mostrarSelectorDestino = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                                shape = RoundedCornerShape(10.dp),
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

                            // Estimación de entrega
                            estimacionEntrega?.let { estimacion ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when {
                                            estimacion.tieneCobertura && estimacion.tarifaRegistrada -> MaterialTheme.colorScheme.surfaceVariant
                                            else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                        }
                                    ),
                                    shape = RoundedCornerShape(10.dp),
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

                            // Tienda más cercana
                            resultadoTiendaCercana.tiendaRecomendada?.let { recomendada ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    ),
                                    shape = RoundedCornerShape(10.dp),
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
                                    }
                                }
                            }

                            // Stock Availability in stores
                            stock?.let {
                                StockAvailability(
                                    stockByStore = it.porTienda,
                                    tiendas = tiendas,
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // VIRTUAL FITTING BUTTON (HU-15 / HU-16)
                if (!prendaHabilitada) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
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
                } else {
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
            }
        }
    }

    // MODAL BOTTOM SHEETS & DIALOGS
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
            }
        )
    }

    if (mostrarMedidas) {
        ModalBottomSheet(
            onDismissRequest = { mostrarMedidas = false },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Tabla de medidas",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = "Medidas en centímetros (cm)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                    ) {
                        Row(modifier = Modifier.padding(vertical = 12.dp)) {
                            Text(text = "Talla", modifier = Modifier.width(70.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(text = "Pecho", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(text = "Cintura", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(text = "Cadera", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(text = "Hombro", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(text = "Largo", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(text = "Manga", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider()

                        prenda!!.medidas.forEach { (talla, medidas) ->
                            Row(modifier = Modifier.padding(vertical = 10.dp)) {
                                Text(text = talla, modifier = Modifier.width(70.dp), fontWeight = FontWeight.SemiBold)
                                Text(text = "${medidas.pecho}", modifier = Modifier.width(80.dp))
                                Text(text = "${medidas.cintura}", modifier = Modifier.width(80.dp))
                                Text(text = "${medidas.cadera}", modifier = Modifier.width(80.dp))
                                Text(text = "${medidas.hombro}", modifier = Modifier.width(80.dp))
                                Text(text = "${medidas.largo}", modifier = Modifier.width(80.dp))
                                Text(text = "${medidas.manga}", modifier = Modifier.width(80.dp))
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
            onDismiss = { mostrarGaleria = false },
        )
    }
}

/** Accordion Item Composable (Clean Figma-style collapsible row) */
@Composable
private fun AccordionItem(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Icon(
                imageVector = if (isExpanded) Icons.Default.Remove else Icons.Default.Add,
                contentDescription = if (isExpanded) "Colapsar $title" else "Expandir $title",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            content()
        }
    }
}
