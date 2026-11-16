package pe.modaviva.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import pe.modaviva.app.R
import pe.modaviva.app.ui.components.MvConfirmDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import pe.modaviva.app.domain.model.MensajeChat
import pe.modaviva.app.domain.model.PrendaSugeridaChat
import pe.modaviva.app.ui.format.formatearSoles
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAssistantScreen(
    onBack: () -> Unit,
    onPrendaClick: (String) -> Unit,
    prendaCodigoContexto: String? = null,
    viewModel: ChatAssistantViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var inputTexto by remember { mutableStateOf("") }
    var confirmarNueva by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val sugerenciasRapidas = remember(prendaCodigoContexto) {
        if (!prendaCodigoContexto.isNullOrBlank()) {
            listOf(
                "¿Qué talla me recomiendas en esta prenda?",
                "¿Con qué otra prenda la puedo combinar?",
                "¿Cuánto cuesta el envío a Miraflores?",
            )
        } else {
            listOf(
                "¿Qué ofertas hay disponibles hoy?",
                "Recomiéndame un outfit casual",
                "¿A qué distritos tienen envío?",
            )
        }
    }

    LaunchedEffect(state.mensajes.size, state.enviando) {
        val total = state.mensajes.size + if (state.enviando) 1 else 0
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    val enviarActual = {
        val limpio = inputTexto.trim()
        if (limpio.isNotEmpty() && !state.enviando) {
            inputTexto = ""
            viewModel.enviarMensaje(limpio, prendaCodigoContexto)
        }
    }

    if (confirmarNueva) {
        MvConfirmDialog(
            title = "Nueva conversación",
            message = "Se borrará la conversación actual con el asistente.",
            confirmLabel = "Borrar",
            destructive = true,
            onConfirm = {
                confirmarNueva = false
                viewModel.nuevaConversacion()
            },
            onDismiss = { confirmarNueva = false },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_bot_assistant),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Column {
                            Text(
                                text = "Asistente ModaViva",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            // HU-27: aviso de que responde un asistente automático (no una persona).
                            Text(
                                text = "Asistente automático · respuestas generadas por IA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    if (state.hayConversacion) {
                        IconButton(onClick = { confirmarNueva = true }) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = stringResource(R.string.cd_chat_new))
                        }
                    }
                },
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                if (state.mensajes.size <= 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        sugerenciasRapidas.forEach { sugerencia ->
                            AssistChip(
                                onClick = {
                                    viewModel.enviarMensaje(sugerencia, prendaCodigoContexto)
                                },
                                label = { Text(sugerencia, maxLines = 1) },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = inputTexto,
                        onValueChange = { inputTexto = it },
                        placeholder = { Text("Pregunta sobre tallas, outfits o envíos…") },
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { enviarActual() }),
                        modifier = Modifier.weight(1f),
                    )

                    FilledIconButton(
                        onClick = enviarActual,
                        enabled = inputTexto.isNotBlank() && !state.enviando,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.cd_chat_send),
                        )
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.mensajes, key = { it.id }) { msg ->
                ChatBubbleItem(
                    mensaje = msg,
                    onPrendaClick = onPrendaClick,
                    // HU-27 CA-05: el mensaje que no se pudo enviar muestra el error y Reintentar.
                    error = state.error.takeIf { msg.id == state.mensajeFallidoId },
                    reintentando = state.enviando,
                    onReintentar = viewModel::reintentar,
                )
            }

            if (state.enviando) {
                item(key = "typing") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 4.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = "Asistente escribiendo…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(
    mensaje: MensajeChat,
    onPrendaClick: (String) -> Unit,
    error: String? = null,
    reintentando: Boolean = false,
    onReintentar: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (mensaje.esUsuario) Alignment.End else Alignment.Start,
    ) {
        Surface(
            color = if (mensaje.esUsuario) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            },
            contentColor = if (mensaje.esUsuario) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (mensaje.esUsuario) 18.dp else 4.dp,
                bottomEnd = if (mensaje.esUsuario) 4.dp else 18.dp,
            ),
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(
                text = mensaje.texto,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }

        if (error != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp).widthIn(max = 300.dp),
            ) {
                Icon(
                    Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f, fill = false),
                )
                TextButton(onClick = onReintentar, enabled = !reintentando) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reintentar")
                }
            }
        }

        if (mensaje.prendasSugeridas.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                mensaje.prendasSugeridas.forEach { prenda ->
                    SuggestedGarmentCard(
                        prenda = prenda,
                        onClick = { onPrendaClick(prenda.codigo) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestedGarmentCard(
    prenda: PrendaSugeridaChat,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(190.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (prenda.fotoUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(prenda.fotoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null, // el nombre ya está escrito al lado
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prenda.nombre,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatearSoles(prenda.precio),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

