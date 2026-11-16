package pe.modaviva.app.ui.search.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.TextButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material.icons.outlined.History
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.modaviva.app.R

/**
 * Buscador editorial de 48 dp con subrayado completo, alineado al estilo que
 * antes vivía en Inicio.
 */
@Composable
fun SearchEditorialBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {},
    focusRequester: FocusRequester? = null,
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    val lineColor = if (isFocused || query.isNotEmpty()) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(48.dp)
            .drawBehind {
                val stroke = if (isFocused) 1.5.dp.toPx() else 1.dp.toPx()
                drawLine(
                    color = lineColor,
                    start = Offset(0f, size.height - stroke / 2),
                    end = Offset(size.width, size.height - stroke / 2),
                    strokeWidth = stroke,
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

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_search_hint).uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    onSearch()
                    focusManager.clearFocus()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                    .onFocusChanged { isFocused = it.isFocused },
            )
        }

        if (query.isNotEmpty()) {
            IconButton(
                onClick = onClearQuery,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Limpiar búsqueda",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * Barra compacta de acciones (Filtros, Ordenar, Categorías rápidas) debajo del buscador.
 */
@Composable
fun SearchControlsSection(
    mostrarVolverExplorar: Boolean,
    onBackToExplore: () -> Unit,
    cantidadFiltrosActivos: Int,
    ordenSeleccionado: String,
    hayFiltrosOBusqueda: Boolean,
    onOpenFilters: () -> Unit,
    onOpenSort: () -> Unit,
    onClearAll: () -> Unit,
    categorias: List<String>,
    categoriaSeleccionada: String?,
    subcategorias: List<String>,
    subcategoriaSeleccionada: String?,
    onSelectCategoria: (String?) -> Unit,
    onSelectSubcategoria: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (mostrarVolverExplorar) {
                IconButton(
                    onClick = onBackToExplore,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver a explorar",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            OutlinedButton(
                onClick = onOpenFilters,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = if (cantidadFiltrosActivos > 0) "Filtros ($cantidadFiltrosActivos)" else "Filtros",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }

            OutlinedButton(
                onClick = onOpenSort,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.SwapVert,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = if (ordenSeleccionado == "Relevancia") "Ordenar" else ordenSeleccionado,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }

            if (hayFiltrosOBusqueda) {
                Text(
                    text = "Limpiar",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable(onClick = onClearAll)
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                )
            }
        }

        if (categorias.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categorias) { categoria ->
                    FilterChip(
                        selected = categoriaSeleccionada == categoria,
                        onClick = {
                            onSelectCategoria(if (categoriaSeleccionada == categoria) null else categoria)
                        },
                        label = { Text(categoria.paraMostrar()) },
                    )
                }
            }
        }

        if (categoriaSeleccionada != null && subcategorias.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                subcategorias.forEach { sub ->
                    FilterChip(
                        selected = subcategoriaSeleccionada == sub,
                        onClick = {
                            onSelectSubcategoria(if (subcategoriaSeleccionada == sub) null else sub)
                        },
                        label = { Text(sub.paraMostrar()) },
                    )
                }
            }
        }
    }
}

private fun String.paraMostrar(): String =
    replace("-", " ").replaceFirstChar { it.uppercase() }

/** HU-06: chips con las búsquedas recientes; al tocar una se vuelve a buscar. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BusquedasRecientes(
    recientes: List<String>,
    onSeleccionar: (String) -> Unit,
    onBorrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (recientes.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Búsquedas recientes",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onBorrar) { Text("Borrar") }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            recientes.forEach { texto ->
                SuggestionChip(onClick = { onSeleccionar(texto) }, label = { Text(texto) })
            }
        }
    }
}
