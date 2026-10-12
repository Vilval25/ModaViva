package pe.modaviva.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import pe.modaviva.app.R
import pe.modaviva.app.domain.model.Portada
import pe.modaviva.app.ui.components.MvEmptyState
import pe.modaviva.app.ui.components.MvErrorState
import pe.modaviva.app.ui.components.MvGarmentCard
import pe.modaviva.app.ui.components.MvGarmentCardSkeleton
import pe.modaviva.app.ui.components.MvOfflineBanner
import pe.modaviva.app.ui.components.MvOfflineState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Pestaña Inicio: la vitrina de la tienda (HU-04). De arriba abajo: portada de
 * campaña, accesos por público (Mujer, Hombre, Niños), Ofertas y Novedades.
 * Inicio nunca muestra resultados de búsqueda: el buscador, los accesos y la
 * portada llevan a otras pantallas.
 *
 * Se abre sin pedir sesión (CA-01) y muestra la copia local, que se mantiene
 * al día con el servidor mientras la pantalla está abierta.
 */
@Composable
fun HomeScreen(
    onNotificationsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onCartClick: () -> Unit,
    onPrendaClick: (String) -> Unit,
    onPortadaClick: (categoria: String) -> Unit,
    onAudienciaClick: (genero: String) -> Unit,
    modifier: Modifier = Modifier,
    unreadNotifications: Int = 0,
    cartItemCount: Int = 0,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val conteoCarrito by viewModel.cartItemCount.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val textoMensaje = mensaje?.let { stringResource(it.texto()) }

    LaunchedEffect(textoMensaje) {
        if (textoMensaje != null) {
            snackbar.showSnackbar(textoMensaje)
            viewModel.onMensajeMostrado()
        }
    }

    Scaffold(
        modifier = modifier,
        // La barra queda fija: solo se desplaza el catálogo (CA-02).
        topBar = {
            HomeTopBar(
                onNotificationsClick = onNotificationsClick,
                onSearchClick = onSearchClick,
                onFavoritesClick = onFavoritesClick,
                onCartClick = onCartClick,
                unreadNotifications = unreadNotifications,
                cartItemCount = conteoCarrito,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        HomeContent(
            state = state,
            onRefresh = viewModel::refrescar,
            onPrendaClick = onPrendaClick,
            onPortadaClick = onPortadaClick,
            onAudienciaClick = onAudienciaClick,
            modifier = Modifier.padding(padding),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onPrendaClick: (String) -> Unit,
    onPortadaClick: (String) -> Unit,
    onAudienciaClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(
        isRefreshing = state.refrescando,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        when (state.contenido) {
            ContenidoHome.CATALOGO, ContenidoHome.CARGANDO ->
                Vitrina(state, onPrendaClick, onPortadaClick, onAudienciaClick)
            ContenidoHome.VACIO -> MvEmptyState(
                icon = Icons.Outlined.Storefront,
                title = stringResource(R.string.catalog_empty_title),
                message = stringResource(R.string.catalog_empty_body),
            )
            ContenidoHome.SIN_CONEXION -> MvOfflineState(onRetry = onRefresh)
            ContenidoHome.ERROR -> MvErrorState(onRetry = onRefresh)
        }
    }
}

@Composable
private fun Vitrina(
    state: HomeUiState,
    onPrendaClick: (String) -> Unit,
    onPortadaClick: (String) -> Unit,
    onAudienciaClick: (String) -> Unit,
) {
    val textoSinConexion = textoSinConexion(state.ultimaActualizacion)
    val tituloDestacados = stringResource(R.string.catalog_featured)
    val tituloCatalogo = stringResource(R.string.catalog_all)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.contenido == ContenidoHome.CARGANDO) {
            // Claves propias: así la cuadrícula no confunde estas posiciones con
            // las del catálogo y arranca arriba cuando llegan los datos.
            items(ESQUELETOS, key = { "esqueleto-$it" }) { MvGarmentCardSkeleton() }
            return@LazyVerticalGrid
        }

        // Datos guardados sin conexión: se avisa de qué momento son (CA-07).
        if (state.sinConexion) {
            filaCompleta { MvOfflineBanner(message = textoSinConexion) }
        }

        // Portada y accesos sin clave propia: si la portada llega después que
        // el catálogo, la lista no se queda anclada en los accesos y la
        // portada aparece arriba, a la vista.
        state.portada?.let { portada ->
            filaCompleta {
                PortadaCampana(portada = portada, onClick = { onPortadaClick(portada.categoria) })
            }
        }

        filaCompleta { Audiencias(onClick = onAudienciaClick) }

        if (state.destacados.isNotEmpty()) {
            filaCompleta { TituloSeccion(tituloDestacados) }
            filaCompleta {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.destacados, key = { it.codigo }) { prenda ->
                        TarjetaPrenda(
                            prenda = prenda,
                            onClick = { onPrendaClick(prenda.codigo) },
                            modifier = Modifier.width(160.dp),
                        )
                    }
                }
            }
        }

        filaCompleta { TituloSeccion(tituloCatalogo) }
        items(state.prendas, key = { it.codigo }) { prenda ->
            TarjetaPrenda(prenda = prenda, onClick = { onPrendaClick(prenda.codigo) })
        }
    }
}

private fun LazyGridScope.filaCompleta(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun TituloSeccion(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier
            .padding(top = 8.dp)
            .semantics { heading() },
    )
}

/**
 * Portada de campaña (`inicio/portada`): foto grande con etiqueta, título y
 * acción. Abre el catálogo de la categoría de la campaña.
 */
@Composable
private fun PortadaCampana(portada: Portada, onClick: () -> Unit) {
    val relleno = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHigh)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 5f)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick, onClickLabel = portada.accion, role = Role.Button),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(portada.imagenUrl)
                .crossfade(true)
                .build(),
            // El título ya describe la campaña.
            contentDescription = null,
            placeholder = relleno,
            error = relleno,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Degradado sobre la foto para que el texto blanco se lea sobre
        // cualquier imagen, en tema claro y oscuro.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        1f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (portada.etiqueta.isNotBlank()) {
                Text(
                    text = portada.etiqueta.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                    color = Color.White,
                )
            }
            Text(
                text = portada.titulo,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
            )
            if (portada.accion.isNotBlank()) {
                Text(
                    text = portada.accion.uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
                    color = Color.White,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/** Accesos por público, debajo de la portada. */
@Composable
private fun Audiencias(onClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        AUDIENCIAS.forEach { (genero, texto) ->
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(role = Role.Button) { onClick(genero) }
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(texto).uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
                )
            }
        }
    }
}

/** Públicos de la tienda: valor del campo `genero` de las prendas y su texto. */
private val AUDIENCIAS = listOf(
    "mujer" to R.string.home_audience_women,
    "hombre" to R.string.home_audience_men,
    "ninos" to R.string.home_audience_kids,
)

/** Tarjeta del catálogo: foto, marca, nombre y precio vigente (CA-06). */
@Composable
private fun TarjetaPrenda(
    prenda: PrendaUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val relleno = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHigh)
    MvGarmentCard(
        name = prenda.nombre,
        brand = prenda.marca,
        price = prenda.precio,
        originalPrice = prenda.precioOriginal,
        isSoldOut = prenda.agotada,
        onClick = onClick,
        modifier = modifier,
        image = {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(prenda.foto)
                    .crossfade(true)
                    .build(),
                contentDescription = prenda.nombre,
                placeholder = relleno,
                error = relleno,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}

@Composable
private fun textoSinConexion(ultimaActualizacion: Instant?): String =
    if (ultimaActualizacion == null) {
        stringResource(R.string.catalog_offline_banner_unknown)
    } else {
        stringResource(
            R.string.catalog_offline_banner,
            FORMATO_FECHA.format(ultimaActualizacion.atZone(ZoneId.systemDefault())),
        )
    }

private fun MensajeHome.texto(): Int = when (this) {
    MensajeHome.NO_SE_PUDO_ACTUALIZAR -> R.string.catalog_refresh_failed
}

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM HH:mm")
private val ESQUELETOS = List(6) { it }

/**
 * Barra superior de Inicio (HT-01 CA-05): Notificaciones, el nombre de la
 * tienda al centro, Favoritos y Carrito; debajo, el buscador a lo ancho. El
 * buscador es un acceso a la pantalla de búsqueda, no un campo editable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    onNotificationsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onCartClick: () -> Unit,
    unreadNotifications: Int,
    cartItemCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        CenterAlignedTopAppBar(
            navigationIcon = {
                BadgedIconButton(
                    icon = Icons.Outlined.Notifications,
                    contentDescription = stringResource(R.string.cd_notifications),
                    count = unreadNotifications,
                    onClick = onNotificationsClick,
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.app_name).uppercase(),
                    style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 4.sp),
                    modifier = Modifier.semantics { heading() },
                )
            },
            actions = {
                IconButton(onClick = onFavoritesClick) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(R.string.cd_favorites),
                    )
                }
                BadgedIconButton(
                    icon = Icons.Outlined.ShoppingCart,
                    contentDescription = stringResource(R.string.cd_cart),
                    count = cartItemCount,
                    onClick = onCartClick,
                )
            },
        )
        SearchEntry(onClick = onSearchClick)
    }
}

/** Ícono con insignia numérica; la insignia se oculta cuando [count] es 0. */
@Composable
private fun BadgedIconButton(
    icon: ImageVector,
    contentDescription: String,
    count: Int,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (count > 0) {
                    Badge {
                        Text(text = if (count > 99) "99+" else count.toString())
                    }
                }
            },
        ) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    }
}

/** Buscador subrayado y a lo ancho, como en la maqueta editorial. */
@Composable
private fun SearchEntry(onClick: () -> Unit) {
    val linea = MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(48.dp)
            .clickable(onClick = onClick, role = Role.Button)
            .drawBehind {
                val grosor = 1.dp.toPx()
                drawLine(
                    color = linea,
                    start = Offset(0f, size.height - grosor / 2),
                    end = Offset(size.width, size.height - grosor / 2),
                    strokeWidth = grosor,
                )
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.home_search_hint).uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
