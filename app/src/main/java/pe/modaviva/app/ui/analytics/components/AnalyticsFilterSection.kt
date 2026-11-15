package pe.modaviva.app.ui.analytics.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.modaviva.app.domain.model.RangoFechasActividad
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Selector de rango temporal con presets rápidos y calendario de rango
 * personalizado (HU-25 CA-04).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsFilterSection(
    rangoSeleccionado: RangoFechasActividad,
    fechaDesdeMillis: Long?,
    fechaHastaMillis: Long?,
    onSelectPreset: (RangoFechasActividad) -> Unit,
    onApplyCustomRange: (Long, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mostrarPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                RangoFechasActividad.ULTIMO_MES,
                RangoFechasActividad.ULTIMOS_3_MESES,
                RangoFechasActividad.ULTIMOS_6_MESES,
                RangoFechasActividad.TODO,
            ).forEach { preset ->
                FilterChip(
                    selected = rangoSeleccionado == preset,
                    onClick = { onSelectPreset(preset) },
                    label = { Text(preset.etiqueta) },
                )
            }

            FilterChip(
                selected = rangoSeleccionado == RangoFechasActividad.PERSONALIZADO,
                onClick = { mostrarPicker = true },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                label = { Text(RangoFechasActividad.PERSONALIZADO.etiqueta) },
            )
        }

        if (rangoSeleccionado == RangoFechasActividad.PERSONALIZADO && fechaDesdeMillis != null && fechaHastaMillis != null) {
            Text(
                text = "Mostrando del ${formatearFechaCorta(fechaDesdeMillis)} al ${formatearFechaCorta(fechaHastaMillis)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (mostrarPicker) {
        val pickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { mostrarPicker = false },
            confirmButton = {
                TextButton(
                    enabled = pickerState.selectedStartDateMillis != null &&
                        pickerState.selectedEndDateMillis != null,
                    onClick = {
                        val start = pickerState.selectedStartDateMillis
                        val end = pickerState.selectedEndDateMillis
                        if (start != null && end != null) {
                            onApplyCustomRange(start, end)
                        }
                        mostrarPicker = false
                    },
                ) {
                    Text("Aplicar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarPicker = false }) {
                    Text("Cancelar")
                }
            },
        ) {
            DateRangePicker(
                state = pickerState,
                title = {
                    Text(
                        text = "Elige el rango de fechas",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    .withZone(ZoneId.of("America/Lima"))

private fun formatearFechaCorta(millis: Long): String =
    FORMATO_FECHA.format(Instant.ofEpochMilli(millis))

