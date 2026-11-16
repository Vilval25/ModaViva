package pe.modaviva.app.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentReturn
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.modaviva.app.domain.model.Notificacion
import pe.modaviva.app.domain.model.TipoNotificacion
import pe.modaviva.app.ui.components.MvEmptyState
import pe.modaviva.app.ui.components.MvLoadingState
import pe.modaviva.app.ui.components.MvScreen
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.res.stringResource
import pe.modaviva.app.R

/**
 * Centro de notificaciones (HU-18): lista de avisos del más reciente al más
 * antiguo, con indicador de no leído y acción "Marcar todo como leído".
 */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onAvisoClick: (Notificacion) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MvScreen(
        title = "Notificaciones",
        onBack = onBack,
        actions = {
            val listo = state as? NotificationsUiState.Listo
            if (listo?.hayNoLeidos == true) {
                TextButton(onClick = viewModel::marcarTodoLeido) {
                    Text("Marcar todo como leído")
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                NotificationsUiState.Cargando -> MvLoadingState()
                is NotificationsUiState.Listo -> if (s.avisos.isEmpty()) {
                    // CA-07
                    MvEmptyState(
                        icon = Icons.Outlined.NotificationsNone,
                        title = "No tienes notificaciones",
                        message = "Aquí verás los avisos de tus pruebas virtuales, pedidos y solicitudes.",
                    )
                } else {
                    LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
                        items(s.avisos, key = { it.id }) { aviso ->
                            FilaAviso(
                                aviso = aviso,
                                onClick = {
                                    viewModel.abrir(aviso)
                                    onAvisoClick(aviso)
                                },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaAviso(aviso: Notificacion, onClick: () -> Unit) {
    val fondo = if (aviso.leida) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(fondo)
            .clickable(onClickLabel = stringResource(R.string.cd_action_open_notice), onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics {
                contentDescription = buildString {
                    if (!aviso.leida) append("No leído. ")
                    append(aviso.titulo)
                }
            },
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = aviso.tipo.icono(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = aviso.titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (aviso.leida) FontWeight.Medium else FontWeight.Bold,
            )
            if (aviso.texto.isNotBlank()) {
                Text(
                    text = aviso.texto,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = FORMATO_FECHA.format(aviso.creadaEn),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        if (!aviso.leida) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
    }
}

private fun TipoNotificacion.icono(): ImageVector = when (this) {
    TipoNotificacion.PRUEBA_VIRTUAL -> Icons.Outlined.Checkroom
    TipoNotificacion.ELIMINACION_FOTO -> Icons.Outlined.DeleteOutline
    TipoNotificacion.PEDIDO -> Icons.Outlined.LocalShipping
    TipoNotificacion.SOLICITUD -> Icons.Outlined.AssignmentReturn
    TipoNotificacion.GENERAL -> Icons.Outlined.Info
}

private val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter
    .ofPattern("dd/MM/yyyy · HH:mm", Locale.forLanguageTag("es-PE"))
    .withZone(ZoneId.of("America/Lima"))
