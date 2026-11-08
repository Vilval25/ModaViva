package pe.modaviva.app.ui.checkout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import pe.modaviva.app.domain.model.CuponAplicado
import pe.modaviva.app.domain.model.ItemPedidoCalculado
import pe.modaviva.app.domain.model.TarifaEnvio
import pe.modaviva.app.domain.usecase.TiendaCartConDistancia
import pe.modaviva.app.ui.format.formatearSoles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutSummaryScreen(
    onBack: () -> Unit,
    onContinueToDelivery: () -> Unit,
    viewModel: CheckoutSummaryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Resumen del pedido",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "Paso 1 de 3: Confirmación de importes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al carrito",
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (!state.cargando && state.items.isNotEmpty()) {
                val habilitado = state.totalPrendas > 0 && !state.calculando &&
                        (state.tipoEntrega == TipoEntregaUi.DELIVERY || (state.tipoEntrega == TipoEntregaUi.RECOJO && state.tiendaRecojoSeleccionada != null && state.hayStockSuficienteRecojo))

                CheckoutBottomBar(
                    total = state.total,
                    totalPrendas = state.totalPrendas,
                    habilitado = habilitado,
                    calculando = state.calculando,
                    onContinue = onContinueToDelivery,
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.cargando -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.items.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No hay prendas para calcular el pedido.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // 1. Avisos y revalidación (CA-06)
                        if (state.alertas.isNotEmpty()) {
                            item {
                                AvisosRevalidacionCard(
                                    alertas = state.alertas,
                                    onDismiss = { index -> viewModel.descartarAlerta(index) },
                                )
                            }
                        }

                        // 2. Modalidad de entrega y cálculo de tarifa (CA-03)
                        item {
                            ModalidadEntregaSection(
                                tipoEntrega = state.tipoEntrega,
                                distritoSeleccionado = state.distritoSeleccionado,
                                distritosDisponibles = state.distritosDisponibles,
                                tiendasElegibles = state.tiendasRecojoElegibles,
                                tiendaSeleccionada = state.tiendaRecojoSeleccionada,
                                hayStockSuficienteRecojo = state.hayStockSuficienteRecojo,
                                onTipoChanged = viewModel::onTipoEntregaChanged,
                                onDistritoChanged = viewModel::onDistritoChanged,
                                onTiendaChanged = viewModel::onTiendaRecojoChanged,
                            )
                        }

                        // 3. Lista de ítems revalidados (CA-01, CA-06)
                        item {
                            Text(
                                text = "Prendas (${state.totalPrendas})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        items(state.items, key = { "${it.codigo}_${it.talla}" }) { item ->
                            ItemCheckoutRow(item = item)
                        }

                        // 4. Sección de cupones (CA-04, CA-05)
                        item {
                            CuponSection(
                                cuponInput = state.cuponInput,
                                cuponesAplicados = state.cuponesAplicados,
                                errorCupon = state.mensajeErrorCupon,
                                exitoCupon = state.mensajeExitoCupon,
                                onInputChanged = viewModel::onCuponInputChanged,
                                onAplicar = viewModel::aplicarCupon,
                                onRemover = viewModel::removerCupon,
                                calculando = state.calculando,
                            )
                        }

                        // 5. Desglose detallado del pedido (CA-02)
                        item {
                            DesglosePedidoCard(
                                subtotal = state.subtotal,
                                descuento = state.descuentoTotal,
                                envio = state.costoEnvio,
                                total = state.total,
                                tipoEntrega = state.tipoEntrega,
                                cuponesAplicados = state.cuponesAplicados,
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }

            if (state.calculando && !state.cargando) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.15f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Text(
                                    text = "Recalculando totales...",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Banner de avisos de stock o precios actualizados (CA-06).
 */
@Composable
private fun AvisosRevalidacionCard(
    alertas: List<String>,
    onDismiss: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Aviso sobre tu pedido",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            alertas.forEachIndexed { index, alerta ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "• $alerta",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { onDismiss(index) },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar alerta",
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modalidad de entrega: Recojo (S/ 0) o Delivery (tarifa según distrito) (CA-03).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModalidadEntregaSection(
    tipoEntrega: TipoEntregaUi,
    distritoSeleccionado: TarifaEnvio?,
    distritosDisponibles: List<TarifaEnvio>,
    tiendasElegibles: List<TiendaCartConDistancia>,
    tiendaSeleccionada: TiendaCartConDistancia?,
    hayStockSuficienteRecojo: Boolean,
    onTipoChanged: (TipoEntregaUi) -> Unit,
    onDistritoChanged: (TarifaEnvio) -> Unit,
    onTiendaChanged: (TiendaCartConDistancia) -> Unit,
) {
    var expandedDropdown by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Modalidad de entrega",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Selector Recojo vs Delivery
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val esDelivery = tipoEntrega == TipoEntregaUi.DELIVERY
                val esRecojo = tipoEntrega == TipoEntregaUi.RECOJO

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTipoChanged(TipoEntregaUi.DELIVERY) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (esDelivery) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (esDelivery) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = if (esDelivery) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Envío",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (distritoSeleccionado != null) formatearSoles(distritoSeleccionado.costo) else "Tarifa según zona",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTipoChanged(TipoEntregaUi.RECOJO) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (esRecojo) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (esRecojo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = if (esRecojo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Recojo en tienda",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Gratis (S/ 0.00)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            // Si es delivery, mostrar selector de distrito
            if (tipoEntrega == TipoEntregaUi.DELIVERY && distritosDisponibles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Distrito de entrega en Lima:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = distritoSeleccionado?.let { "${it.distrito} (${formatearSoles(it.costo)})" } ?: "Selecciona tu distrito",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                    )

                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                    ) {
                        distritosDisponibles.forEach { tarifa ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(text = tarifa.distrito)
                                        Text(
                                            text = formatearSoles(tarifa.costo),
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                },
                                onClick = {
                                    onDistritoChanged(tarifa)
                                    expandedDropdown = false
                                },
                            )
                        }
                    }
                }
            }

            // Si es recojo en tienda, mostrar selector de tienda
            if (tipoEntrega == TipoEntregaUi.RECOJO) {
                Spacer(modifier = Modifier.height(12.dp))
                if (!hayStockSuficienteRecojo || tiendasElegibles.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Sin stock suficiente",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Lo sentimos, ninguna de nuestras tiendas cuenta con stock suficiente para todos los productos de tu carrito.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Selecciona la tienda para el recojo:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        tiendasElegibles.forEach { tiendaCart ->
                            val esElegida = tiendaSeleccionada?.tienda?.id == tiendaCart.tienda.id
                            Card(
                                onClick = { onTiendaChanged(tiendaCart) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (esElegida) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (esElegida) 2.dp else 1.dp,
                                    color = if (esElegida) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                ),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = tiendaCart.tienda.nombre,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        if (tiendaCart.esRecomendada) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                            ) {
                                                Text(
                                                    text = "Recomendada",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    if (tiendaCart.tienda.direccion.isNotBlank()) {
                                        Text(
                                            text = "${tiendaCart.tienda.direccion}, ${tiendaCart.tienda.distrito}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    tiendaCart.distanciaKm?.let { km ->
                                        Text(
                                            text = "Distancia aproximada: ${"%.1f".format(km)} km",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renglón de ítem en el checkout con datos de revalidación.
 */
@Composable
private fun ItemCheckoutRow(item: ItemPedidoCalculado) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.agotado) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = item.fotoUrl,
                contentDescription = item.nombre,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Talla: ${item.talla}  •  Cant: ${item.cantidadAprobada}${if (item.cantidadAprobada != item.cantidadOriginal) " (de ${item.cantidadOriginal})" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (item.cambioPrecio && item.precioAnterior != null) {
                    Text(
                        text = "Antes: ${formatearSoles(item.precioAnterior)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        textDecoration = TextDecoration.LineThrough,
                    )
                }

                if (item.agotado) {
                    Text(
                        text = "Agotado",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatearSoles(item.subtotal),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.agotado) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${formatearSoles(item.precioUnitario)} c/u",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Sección de cupones con caja de texto, feedback específico y chips de aplicados (CA-04, CA-05).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CuponSection(
    cuponInput: String,
    cuponesAplicados: List<CuponAplicado>,
    errorCupon: String?,
    exitoCupon: String?,
    onInputChanged: (String) -> Unit,
    onAplicar: () -> Unit,
    onRemover: (String) -> Unit,
    calculando: Boolean,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.ConfirmationNumber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cupón de descuento",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = cuponInput,
                    onValueChange = onInputChanged,
                    placeholder = { Text("Ej. MODA10") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            onAplicar()
                        },
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    onClick = {
                        keyboardController?.hide()
                        onAplicar()
                    },
                    enabled = cuponInput.isNotBlank() && !calculando,
                ) {
                    Text("Aplicar")
                }
            }

            // Feedback de error (CA-04, CA-05)
            AnimatedVisibility(
                visible = errorCupon != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = errorCupon.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // Feedback de éxito
            AnimatedVisibility(
                visible = exitoCupon != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = exitoCupon.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Chips de cupones aplicados
            if (cuponesAplicados.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    cuponesAplicados.forEach { cupon ->
                        InputChip(
                            selected = true,
                            onClick = {},
                            label = {
                                Text(
                                    text = "${cupon.codigo} (-${formatearSoles(cupon.descuentoCalculado)})",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Quitar cupón",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onRemover(cupon.codigo) },
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta de desglose: subtotal, descuento, envío y total (CA-02).
 */
@Composable
private fun DesglosePedidoCard(
    subtotal: Double,
    descuento: Double,
    envio: Double,
    total: Double,
    tipoEntrega: TipoEntregaUi,
    cuponesAplicados: List<CuponAplicado>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Desglose de pago",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Subtotal
            FilaDesglose(
                etiqueta = "Subtotal prendas",
                valor = formatearSoles(subtotal),
            )

            // Descuento
            if (descuento > 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                FilaDesglose(
                    etiqueta = "Descuento cupones",
                    valor = "- ${formatearSoles(descuento)}",
                    colorValor = MaterialTheme.colorScheme.primary,
                )
            }

            // Envío
            Spacer(modifier = Modifier.height(8.dp))
            FilaDesglose(
                etiqueta = if (tipoEntrega == TipoEntregaUi.RECOJO) "Recojo en tienda" else "Envío a domicilio",
                valor = if (envio <= 0.0) "Gratis" else formatearSoles(envio),
                colorValor = if (envio <= 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Total final (CA-02: total = subtotal - descuento + envio)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Total a pagar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Incluye impuestos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = formatearSoles(total),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun FilaDesglose(
    etiqueta: String,
    valor: String,
    colorValor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = colorValor,
        )
    }
}

/**
 * Barra inferior fija con el total y botón Continuar.
 */
@Composable
private fun CheckoutBottomBar(
    total: Double,
    totalPrendas: Int,
    habilitado: Boolean,
    calculando: Boolean,
    onContinue: () -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Total estimado",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatearSoles(total),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Button(
                onClick = onContinue,
                enabled = habilitado,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(48.dp),
            ) {
                Text(
                    text = "Continuar ($totalPrendas)",
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
