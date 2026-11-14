package pe.modaviva.app.ui.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.modaviva.app.R
import pe.modaviva.app.domain.model.EstadoPedido
import pe.modaviva.app.domain.model.Pedido
import pe.modaviva.app.ui.components.MvEmptyState
import pe.modaviva.app.ui.components.MvScreen
import pe.modaviva.app.ui.format.formatearSoles
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun OrderHistoryScreen(
    onOrderClick: (String) -> Unit = {},
    onGoToHome: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: OrderHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.mensaje) {
        state.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        MvScreen(
            title = stringResource(R.string.orders_title),
            modifier = Modifier.padding(innerPadding),
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
                    state.pedidos.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            MvEmptyState(
                                icon = Icons.Outlined.History,
                                title = stringResource(R.string.orders_empty_title),
                                message = stringResource(R.string.orders_empty_body),
                            ) {
                                Button(
                                    onClick = onGoToHome,
                                    modifier = Modifier.fillMaxWidth(0.8f),
                                ) {
                                    Text("Explorar catálogo")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = viewModel::sembrarDatosDemo,
                                    enabled = !state.sembrando,
                                    modifier = Modifier.fillMaxWidth(0.8f),
                                ) {
                                    if (state.sembrando) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Cargando datos demo...")
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Cargar pedidos demo (JSON)")
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "${state.pedidos.size} pedidos registrados",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    OutlinedButton(
                                        onClick = viewModel::sembrarDatosDemo,
                                        enabled = !state.sembrando,
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (state.sembrando) "Cargando..." else "Recargar Demo")
                                    }
                                }
                            }

                            items(state.pedidos, key = { it.id }) { pedido ->
                                OrderCardItem(
                                    pedido = pedido,
                                    onClick = { onOrderClick(pedido.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCardItem(
    pedido: Pedido,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = pedido.numeroPedido,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                ChipEstadoPedido(estado = pedido.estado)
            }

            Text(
                text = formatearFecha(pedido.creadoEnMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (pedido.esRecojoEnTienda) Icons.Default.Storefront else Icons.Default.LocalShipping,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (pedido.esRecojoEnTienda) "Recojo: ${pedido.tiendaRecojoNombre ?: "Tienda"}" else "Envío: ${pedido.distrito}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Text(
                    text = formatearSoles(pedido.total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${pedido.totalPrendas} prenda(s) • ${pedido.resumenMedioPago}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Ver detalle",
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
fun ChipEstadoPedido(estado: EstadoPedido) {
    val (fondo, texto) = when (estado) {
        EstadoPedido.PENDIENTE -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) to MaterialTheme.colorScheme.error
        EstadoPedido.APROBADO -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        EstadoPedido.EN_PREPARACION -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        EstadoPedido.DESPACHADO -> Color(0xFFE0F2FE) to Color(0xFF0369A1)
        EstadoPedido.ENTREGADO -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        EstadoPedido.ANULADO -> Color(0xFFF3F4F6) to Color(0xFF6B7280)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = fondo,
    ) {
        Text(
            text = estado.etiqueta,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = texto,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

private fun formatearFecha(epochMillis: Long): String {
    return DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
        .withZone(ZoneId.of("America/Lima"))
        .format(Instant.ofEpochMilli(epochMillis))
}
