package pe.modaviva.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.modaviva.app.R

/*
 * Estados comunes de las pantallas que consultan datos (HT-01 CA-08). Cada
 * pantalla elige cuál mostrar según su UiState; así todas se ven igual al
 * cargar, al no tener resultados, al fallar o al quedarse sin conexión.
 */

@Composable
fun MvLoadingState(
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        CircularProgressIndicator()
        Text(
            text = message ?: stringResource(R.string.state_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Mensaje centrado con ícono, título y texto. Es la base del resto de
 * estados y sirve también para "sin resultados" o "aún no tienes…".
 * [actions] recibe los botones que correspondan a la pantalla.
 */
@Composable
fun MvEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.outline,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = iconTint,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = actions,
        )
    }
}

@Composable
fun MvErrorState(
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.state_error_body),
) {
    MvEmptyState(
        icon = Icons.Outlined.ErrorOutline,
        title = stringResource(R.string.state_error_title),
        message = message,
        modifier = modifier,
        iconTint = MaterialTheme.colorScheme.error,
    ) {
        if (onRetry != null) {
            MvPrimaryButton(
                text = stringResource(R.string.state_retry),
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Sin conexión y sin datos guardados que mostrar. */
@Composable
fun MvOfflineState(
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    MvEmptyState(
        icon = Icons.Outlined.CloudOff,
        title = stringResource(R.string.state_offline_title),
        message = stringResource(R.string.state_offline_body),
        modifier = modifier,
    ) {
        if (onRetry != null) {
            MvPrimaryButton(
                text = stringResource(R.string.state_retry),
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Aviso sobre el contenido cuando se muestran datos guardados sin conexión,
 * p. ej. "Sin conexión – datos del 04/10 18:30" (HU-04).
 */
@Composable
fun MvOfflineBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(imageVector = Icons.Outlined.CloudOff, contentDescription = null)
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
