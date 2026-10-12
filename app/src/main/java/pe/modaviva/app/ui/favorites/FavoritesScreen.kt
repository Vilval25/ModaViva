package pe.modaviva.app.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.launch
import pe.modaviva.app.R
import pe.modaviva.app.ui.components.GarmentImagePlaceholder
import pe.modaviva.app.ui.components.MvEmptyState
import pe.modaviva.app.ui.components.MvGarmentCard
import pe.modaviva.app.ui.components.MvLoadingState
import pe.modaviva.app.ui.components.MvPrimaryButton
import pe.modaviva.app.ui.components.MvScreen

/**
 * Favoritos (HU-08): cuadrícula de las prendas guardadas, de la más reciente
 * a la más antigua, con su precio y disponibilidad actuales. El corazón las
 * quita, con la opción de deshacerlo unos segundos.
 */
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onPrendaClick: (String) -> Unit,
    onGoToHome: () -> Unit,
    viewModel: FavoritosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val textoQuitado = stringResource(R.string.favorites_removed)
    val textoDeshacer = stringResource(R.string.favorites_undo)
    val textoRetirada = stringResource(R.string.favorites_unavailable_message)

    MvScreen(
        title = stringResource(R.string.favorites_title),
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        when {
            state.cargando -> MvLoadingState(modifier = modifier)
            state.favoritos.isEmpty() -> MvEmptyState(
                icon = Icons.Outlined.FavoriteBorder,
                title = stringResource(R.string.favorites_empty_title),
                message = stringResource(R.string.favorites_empty_body),
                modifier = modifier,
            ) {
                MvPrimaryButton(text = stringResource(R.string.favorites_go_home), onClick = onGoToHome)
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 156.dp),
                modifier = modifier,
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(state.favoritos, key = { it.codigo }) { item ->
                    TarjetaFavorito(
                        item = item,
                        onClick = {
                            if (item.disponible) {
                                onPrendaClick(item.codigo)
                            } else {
                                scope.launch { snackbarHostState.showSnackbar(textoRetirada) }
                            }
                        },
                        onQuitar = {
                            viewModel.quitar(item.favorito)
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val resultado = snackbarHostState.showSnackbar(
                                    message = textoQuitado,
                                    actionLabel = textoDeshacer,
                                    duration = SnackbarDuration.Short,
                                )
                                if (resultado == SnackbarResult.ActionPerformed) {
                                    viewModel.deshacer(item.favorito)
                                }
                            }
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaFavorito(
    item: FavoritoUi,
    onClick: () -> Unit,
    onQuitar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val relleno = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHigh)
    val nombre = item.nombre.ifBlank { stringResource(R.string.favorites_unavailable_name) }
    MvGarmentCard(
        name = nombre,
        brand = item.marca,
        price = item.precio.orEmpty(),
        originalPrice = item.precioOriginal,
        isSoldOut = item.agotada,
        isUnavailable = !item.disponible,
        isFavorite = true,
        onFavoriteClick = onQuitar,
        onClick = onClick,
        modifier = modifier,
        image = {
            if (item.foto == null) {
                GarmentImagePlaceholder()
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.foto)
                        .crossfade(true)
                        .build(),
                    contentDescription = nombre,
                    placeholder = relleno,
                    error = relleno,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
    )
}
