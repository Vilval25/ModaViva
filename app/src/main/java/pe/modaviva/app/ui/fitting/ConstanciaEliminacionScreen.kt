package pe.modaviva.app.ui.fitting

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.modaviva.app.R
import pe.modaviva.app.domain.model.ConstanciaEliminacionFoto
import pe.modaviva.app.ui.components.MvPrimaryButton
import pe.modaviva.app.ui.components.MvSecondaryButton
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Constancia que se muestra al terminar de eliminar la foto (HU-17 CA-03):
 * fecha y hora, qué se borró, qué se conserva (CA-06) y que el probador queda
 * deshabilitado hasta registrar otra foto (CA-05).
 */
@Composable
internal fun ConstanciaEliminacionScreen(
    constancia: ConstanciaEliminacionFoto,
    onNuevaFotoClick: () -> Unit,
    onCatalogoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp),
            )
        }

        Text(
            text = stringResource(R.string.fitting_delete_receipt_label).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = stringResource(R.string.fitting_delete_receipt_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Text(
            text = fechaConstancia(constancia.eliminadaEn),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                LineaConstancia(Icons.Outlined.DeleteOutline, stringResource(R.string.fitting_delete_receipt_removed))
                LineaConstancia(Icons.Outlined.Lock, stringResource(R.string.fitting_delete_receipt_kept))
                LineaConstancia(Icons.Outlined.PhotoCamera, stringResource(R.string.fitting_delete_receipt_disabled))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        MvPrimaryButton(
            text = stringResource(R.string.fitting_delete_receipt_new_photo),
            onClick = onNuevaFotoClick,
            modifier = Modifier.fillMaxWidth(),
        )
        MvSecondaryButton(
            text = stringResource(R.string.fitting_delete_receipt_catalog),
            onClick = onCatalogoClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LineaConstancia(icon: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = texto, style = MaterialTheme.typography.bodyMedium)
    }
}

private val FORMATO_CONSTANCIA: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", Locale.forLanguageTag("es-PE"))

/** "9 de noviembre de 2026, 15:42" en la zona horaria del teléfono. */
internal fun fechaConstancia(instante: Instant, zona: ZoneId = ZoneId.systemDefault()): String =
    FORMATO_CONSTANCIA.withZone(zona).format(instante)
