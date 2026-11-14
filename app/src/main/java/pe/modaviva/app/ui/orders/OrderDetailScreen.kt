package pe.modaviva.app.ui.orders

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import pe.modaviva.app.domain.model.EstadoPedido
import pe.modaviva.app.domain.model.HitoPedido
import pe.modaviva.app.domain.model.ItemPedido
import pe.modaviva.app.domain.model.Pedido
import pe.modaviva.app.domain.model.TipoMedioPago
import pe.modaviva.app.ui.format.formatearSoles
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    onBack: () -> Unit,
    viewModel: OrderDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(state.mensaje) {
        state.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.pedido?.numeroPedido ?: "Detalle del pedido",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        state.pedido?.let {
                            Text(
                                text = "Comprado el ${formatearFechaHora(it.creadoEnMillis)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.cargando) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            val p = state.pedido
            if (p == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No se encontró la información del pedido.")
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Estado y Simulador Demo de Transiciones
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        ),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Estado actual:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                ChipEstadoPedido(estado = p.estado)
                            }

                            // Simulador de estados para la presentación y evaluación
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Simulador de estados (para jurado / presentación):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                listOf(
                                    EstadoPedido.APROBADO,
                                    EstadoPedido.EN_PREPARACION,
                                    EstadoPedido.DESPACHADO,
                                    EstadoPedido.ENTREGADO,
                                ).forEach { est ->
                                    val activo = p.estado == est
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewModel.cambiarEstadoDemo(est) },
                                    ) {
                                        Text(
                                            text = when (est) {
                                                EstadoPedido.APROBADO -> "Aprob."
                                                EstadoPedido.EN_PREPARACION -> "Prep."
                                                EstadoPedido.DESPACHADO -> "Desp."
                                                EstadoPedido.ENTREGADO -> "Entreg."
                                                else -> est.name
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (activo) FontWeight.Bold else FontWeight.Normal,
                                            color = if (activo) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // HU-22: Banner de Reserva y Cuenta Regresiva de 30 minutos si es PENDIENTE
                    if (p.estado == EstadoPedido.PENDIENTE) {
                        val mins = state.segundosRestantes / 60
                        val segs = state.segundosRestantes % 60
                        val tiempoFormateado = "%02d:%02d".format(mins, segs)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Tiempo restante para pagar: $tiempoFormateado",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tus prendas están reservadas temporalmente. Si no completas el pago dentro de los 30 minutos, la reserva se anulará y las prendas regresarán al catálogo.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.confirmarPagoPendiente(TipoMedioPago.TARJETA_CREDITO) },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Pagar ahora")
                                    }
                                    OutlinedButton(
                                        onClick = viewModel::anularPedidoVencido,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("Liberar reserva")
                                    }
                                }
                            }
                        }
                    }

                    // HU-23 CA-06: Tarjeta de Transporte con guía y courier
                    if (p.estado == EstadoPedido.DESPACHADO && !p.numeroSeguimiento.isNullOrBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF0F9FF),
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(28.dp),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Courier: ${p.empresaTransporte ?: "Olva Courier"}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "Número de seguimiento: ${p.numeroSeguimiento}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Guía", p.numeroSeguimiento))
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Número de guía copiado al portapapeles")
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copiar seguimiento",
                                        tint = Color(0xFF0284C7),
                                    )
                                }
                            }
                        }
                    }

                    // HU-23 CA-07: Banner de Recojo en Tienda
                    if (p.esRecojoEnTienda && p.listoParaRecoger) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF0FDF4),
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(28.dp),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "¡Listo para recoger en tienda!",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D),
                                    )
                                    Text(
                                        text = "${p.tiendaRecojoNombre} (${p.tiendaRecojoDireccion})",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    p.tiendaRecojoHorario?.let {
                                        Text(text = "Horario: $it", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    // HU-23 CA-04: Línea de tiempo vertical de estados
                    Text(
                        text = "Seguimiento en tiempo real",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    LineaDeTiempoVertical(hitos = p.hitos)

                    // HU-23 CA-08 y HU-24: Botón Solicitar cambio o devolución
                    if (p.estado == EstadoPedido.ENTREGADO) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                if (p.solicitudPostventa != null) {
                                    val sol = p.solicitudPostventa
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = "Solicitud ${sol.numeroSolicitud}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                        ) {
                                            Text(
                                                text = sol.estado,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${sol.tipo} por: ${sol.motivo} (Prenda: ${sol.prendaNombre})",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                } else {
                                    Text(
                                        text = "¿Tienes algún problema con tu pedido?",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "Cuentas con hasta 7 días calendario tras la entrega para solicitar cambio de talla o devolución.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { viewModel.setMostrarDialogoPostventa(true) },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Icon(Icons.Default.AssignmentReturn, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Solicitar cambio o devolución")
                                    }
                                }
                            }
                        }
                    }

                    // Detalle de Prendas Compradas
                    Text(
                        text = "Prendas (${p.totalPrendas})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        p.items.forEach { item ->
                            ItemPedidoRow(item = item)
                        }
                    }

                    // Desglose final de importes
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            FilaImporte("Subtotal:", formatearSoles(p.subtotal))
                            if (p.descuentoTotal > 0.0) {
                                FilaImporte("Descuento cupón (${p.cuponAplicado ?: ""}):", "-${formatearSoles(p.descuentoTotal)}", colorTexto = MaterialTheme.colorScheme.primary)
                            }
                            FilaImporte("Costo de envío:", if (p.costoEnvio == 0.0) "¡Gratis!" else formatearSoles(p.costoEnvio))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            FilaImporte("Total pagado:", formatearSoles(p.total), esTotal = true)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Diálogo HU-24: Registro de Cambio o Devolución
    if (state.mostrarDialogoPostventa && state.pedido != null) {
        DialogoSolicitudPostventa(
            pedido = state.pedido!!,
            onDismiss = { viewModel.setMostrarDialogoPostventa(false) },
            onEnviar = { codigo, nombre, talla, tipo, motivo, otro ->
                viewModel.registrarPostventa(codigo, nombre, talla, tipo, motivo, otro)
            },
        )
    }
}

@Composable
private fun LineaDeTiempoVertical(hitos: List<HitoPedido>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        hitos.forEachIndexed { index, hito ->
            val esUltimo = index == hitos.size - 1
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (esUltimo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    if (index < hitos.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(44.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(
                        text = hito.titulo,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = hito.descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatearFechaHora(hito.fechaEpochMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemPedidoRow(item: ItemPedido) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = item.fotoUrl,
                contentDescription = item.nombre,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Talla: ${item.talla} • Cant: ${item.cantidad}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.usoProbadorVirtual) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Checkroom,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Probado con IA",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Text(
                text = formatearSoles(item.subtotal),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun FilaImporte(
    etiqueta: String,
    valor: String,
    colorTexto: Color = Color.Unspecified,
    esTotal: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = etiqueta,
            style = if (esTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (esTotal) FontWeight.Bold else FontWeight.Normal,
        )
        Text(
            text = valor,
            style = if (esTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (esTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (esTotal) MaterialTheme.colorScheme.primary else colorTexto,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoSolicitudPostventa(
    pedido: Pedido,
    onDismiss: () -> Unit,
    onEnviar: (codigo: String, nombre: String, talla: String, tipo: String, motivo: String, otro: String?) -> Unit,
) {
    var prendaSeleccionada by remember { mutableStateOf(pedido.items.firstOrNull()) }
    var tipo by remember { mutableStateOf("CAMBIO") } // "CAMBIO" | "DEVOLUCION"
    var motivo by remember { mutableStateOf("Talla no me quedó") }
    var detalleOtro by remember { mutableStateOf("") }
    var errorValidacion by remember { mutableStateOf<String?>(null) }

    val motivosPredefinidos = listOf(
        "Talla no me quedó",
        "El corte o caída no me favorece",
        "Color diferente al esperado",
        "Falla de costura o tela",
        "Otro",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Solicitar cambio o devolución", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Selecciona la prenda:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                pedido.items.forEach { item ->
                    val esElegida = prendaSeleccionada?.codigo == item.codigo
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { prendaSeleccionada = item },
                        shape = RoundedCornerShape(8.dp),
                        color = if (esElegida) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    ) {
                        Text(
                            text = "${item.nombre} (Talla ${item.talla})",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tipo de solicitud:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = tipo == "CAMBIO", onClick = { tipo = "CAMBIO" })
                    Text("Cambio de talla", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(12.dp))
                    RadioButton(selected = tipo == "DEVOLUCION", onClick = { tipo = "DEVOLUCION" })
                    Text("Devolución", style = MaterialTheme.typography.bodySmall)
                }

                Text(
                    text = "Motivo:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                motivosPredefinidos.forEach { m ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { motivo = m },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = motivo == m, onClick = { motivo = m })
                        Text(m, style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (motivo == "Otro") {
                    OutlinedTextField(
                        value = detalleOtro,
                        onValueChange = { detalleOtro = it },
                        label = { Text("Especifica el motivo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
                    )
                }

                if (errorValidacion != null) {
                    Text(
                        text = errorValidacion.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = prendaSeleccionada
                    if (p == null) {
                        errorValidacion = "Selecciona una prenda."
                        return@Button
                    }
                    if (motivo == "Otro" && detalleOtro.trim().length < 5) {
                        errorValidacion = "Escribe el detalle del motivo."
                        return@Button
                    }
                    onEnviar(p.codigo, p.nombre, p.talla, tipo, motivo, detalleOtro.ifBlank { null })
                },
            ) {
                Text("Enviar solicitud")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}

private fun formatearFechaHora(epochMillis: Long): String {
    return DateTimeFormatter.ofPattern("dd/MM/yyyy, hh:mm a")
        .withZone(ZoneId.of("America/Lima"))
        .format(Instant.ofEpochMilli(epochMillis))
}

