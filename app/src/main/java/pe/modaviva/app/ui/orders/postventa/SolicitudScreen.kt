package pe.modaviva.app.ui.orders.postventa

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import pe.modaviva.app.domain.model.ItemPedido
import pe.modaviva.app.domain.model.SolicitudPostventa
import pe.modaviva.app.domain.model.TipoSolicitud
import pe.modaviva.app.ui.components.MvSnackbarHost
import java.io.File
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import pe.modaviva.app.R

/** HU-24 — Pantalla completa para solicitar un cambio o una devolución. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitudScreen(
    onBack: () -> Unit,
    viewModel: SolicitudViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.avisoFotos) {
        state.avisoFotos?.let {
            snackbar.showSnackbar(it)
            viewModel.avisoMostrado()
        }
    }

    state.registrada?.let { sol ->
        SolicitudRegistrada(solicitud = sol, onVolver = onBack)
        return
    }

    // --- Fotos: galería (Photo Picker) y cámara ---------------------------------------------
    val elegirVarias = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(SolicitudPostventa.MAX_FOTOS),
    ) { uris -> viewModel.agregarFotos(uris) }
    val elegirUna = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { viewModel.agregarFotos(listOf(it)) } }

    var fotoCamara by rememberSaveable { mutableStateOf<Uri?>(null) }
    val tomarFoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = fotoCamara
        if (ok && uri != null) viewModel.agregarFotos(listOf(uri))
    }
    val pedirCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) {
            nuevaFotoCamara(context).also { fotoCamara = it; tomarFoto.launch(it) }
        }
    }

    val abrirGaleria: () -> Unit = {
        val soloImagenes = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        when {
            state.fotosRestantes <= 0 -> viewModel.avisarMaximoFotos()
            state.fotosRestantes == 1 -> elegirUna.launch(soloImagenes)
            else -> elegirVarias.launch(soloImagenes)
        }
    }
    val abrirCamara: () -> Unit = {
        when {
            state.fotosRestantes <= 0 -> viewModel.avisarMaximoFotos()
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED ->
                nuevaFotoCamara(context).also { fotoCamara = it; tomarFoto.launch(it) }
            else -> pedirCamara.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cambio o devolución", modifier = Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !state.enviando) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    state.error?.let { MensajeError(it) }
                    if (state.enviando) {
                        LinearProgressIndicator(
                            progress = { state.progreso },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        )
                    }
                    Button(
                        onClick = viewModel::enviar,
                        enabled = state.puedeEnviar && state.dentroDelPlazo,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        if (state.enviando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Enviando solicitud…")
                        } else {
                            Text("Enviar solicitud", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        },
        snackbarHost = { MvSnackbarHost(snackbar) },
    ) { padding ->
        val pedido = state.pedido
        when {
            state.cargando -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            pedido == null || !state.dentroDelPlazo -> Box(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Este pedido ya no admite solicitudes. El plazo es de 7 días desde la entrega.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Pedido ${pedido.numeroPedido}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Seccion("1. ¿Qué prenda?")
                pedido.items.forEach { item ->
                    val usada = viewModel.claveItem(item.codigo, item.talla) in state.prendasConSolicitud
                    PrendaOpcion(
                        item = item,
                        elegida = state.itemSeleccionado == item,
                        yaSolicitada = usada,
                        onClick = { viewModel.onItem(item) },
                    )
                }

                Seccion("2. ¿Qué necesitas?")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    TipoSolicitud.entries.forEachIndexed { i, tipo ->
                        SegmentedButton(
                            selected = state.tipo == tipo,
                            onClick = { viewModel.onTipo(tipo) },
                            shape = SegmentedButtonDefaults.itemShape(i, TipoSolicitud.entries.size),
                        ) {
                            Text(if (tipo == TipoSolicitud.CAMBIO) "Cambio" else "Devolución")
                        }
                    }
                }

                Seccion("3. Motivo")
                Column {
                    SolicitudPostventa.MOTIVOS.forEach { m ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = state.motivo == m,
                                    onClick = { viewModel.onMotivo(m) },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = state.motivo == m, onClick = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(m, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (state.motivo == SolicitudPostventa.MOTIVO_OTRO) {
                    OutlinedTextField(
                        value = state.detalleOtro,
                        onValueChange = viewModel::onDetalleOtro,
                        label = { Text("Describe el motivo *") },
                        isError = state.faltaDetalleOtro && state.detalleOtro.isNotEmpty(),
                        supportingText = {
                            Text(
                                if (state.faltaDetalleOtro) {
                                    "Obligatorio: mínimo ${SolicitudPostventa.MIN_DETALLE_OTRO} caracteres."
                                } else {
                                    "${state.detalleOtro.length}/300"
                                },
                            )
                        },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Seccion("4. Fotos de la prenda (opcional, máximo ${SolicitudPostventa.MAX_FOTOS})")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = abrirGaleria, enabled = !state.enviando, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Galería")
                    }
                    OutlinedButton(onClick = abrirCamara, enabled = !state.enviando, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cámara")
                    }
                }
                if (state.fotos.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.fotos.forEach { uri ->
                            Miniatura(uri = uri, onQuitar = { viewModel.quitarFoto(uri) }, habilitada = !state.enviando)
                        }
                    }
                }
                Text(
                    text = "${state.fotos.size} de ${SolicitudPostventa.MAX_FOTOS} fotos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/** Archivo temporal en caché donde la cámara guarda la foto. */
private fun nuevaFotoCamara(context: Context): Uri {
    val carpeta = File(context.cacheDir, "solicitudes").apply { mkdirs() }
    val archivo = File(carpeta, "foto_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo)
}

@Composable
private fun Seccion(titulo: String) {
    Text(
        text = titulo,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun PrendaOpcion(item: ItemPedido, elegida: Boolean, yaSolicitada: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !yaSolicitada, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (elegida) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (elegida) 2.dp else 1.dp,
            color = if (elegida) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.fotoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (yaSolicitada) "Talla ${item.talla} • Ya tiene una solicitud" else "Talla ${item.talla} • ${item.cantidad} und.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.usoProbadorVirtual) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "La probaste en el probador virtual",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            if (elegida) {
                Icon(Icons.Default.Check, contentDescription = "Elegida", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun Miniatura(uri: Uri, onQuitar: () -> Unit, habilitada: Boolean) {
    Box(modifier = Modifier.size(96.dp)) {
        AsyncImage(
            model = uri,
            contentDescription = "Foto adjunta",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        if (habilitada) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
                    .clickable(onClick = onQuitar),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Close, contentDescription = "Quitar foto", tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun MensajeError(texto: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}

/** Confirmación con el número y el estado 'En evaluación' (CA-05). */
@Composable
private fun SolicitudRegistrada(solicitud: SolicitudPostventa, onVolver: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
            }
            Text(
                "¡Solicitud registrada!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                "Evaluaremos tu solicitud y te avisaremos cuando tenga una respuesta.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Dato("Número de solicitud", solicitud.numeroSolicitud, destacado = true)
                    Dato("Estado", solicitud.estado.etiqueta)
                    Dato("Tipo", solicitud.tipo.etiqueta)
                    Dato("Prenda", "${solicitud.prendaNombre} (talla ${solicitud.talla})")
                    Dato("Motivo", solicitud.motivoTexto)
                    Dato("Fotos", "${solicitud.fotosUrls.size}")
                    if (solicitud.tuvoPruebaVirtual) Dato("Prueba virtual previa", "Sí")
                }
            }
            Button(
                onClick = onVolver,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) { Text("Volver al pedido") }
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String, destacado: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            valor,
            style = if (destacado) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.SemiBold,
            textAlign = TextAlign.End,
            color = if (destacado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
