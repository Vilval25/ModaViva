package pe.modaviva.app.ui.orders.postventa

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.modaviva.app.domain.model.EstadoSolicitud
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Chip con el estado de la solicitud: En evaluación, Aprobada o Rechazada (HU-24 CA-05, CA-07). */
@Composable
fun EstadoSolicitudChip(estado: EstadoSolicitud, modifier: Modifier = Modifier) {
    val (fondo, texto) = when (estado) {
        EstadoSolicitud.EN_EVALUACION ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        EstadoSolicitud.APROBADA ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        EstadoSolicitud.RECHAZADA ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(shape = RoundedCornerShape(12.dp), color = fondo, modifier = modifier) {
        Text(
            text = estado.etiqueta,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = texto,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

private val formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy, hh:mm a", Locale.forLanguageTag("es-PE"))
    .withZone(ZoneId.of("America/Lima"))

internal fun fechaSolicitud(millis: Long): String = formatoFecha.format(Instant.ofEpochMilli(millis))
