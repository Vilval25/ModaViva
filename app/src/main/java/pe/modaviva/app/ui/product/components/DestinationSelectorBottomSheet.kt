package pe.modaviva.app.ui.product.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.modaviva.app.data.location.ResultadoUbicacion
import pe.modaviva.app.domain.model.DestinoEntrega
import pe.modaviva.app.domain.model.DireccionEntrega
import pe.modaviva.app.domain.model.TarifaEnvio
import pe.modaviva.app.domain.model.TipoDestino
import pe.modaviva.app.domain.model.UserProfile
import pe.modaviva.app.ui.components.MvPrimaryButton
import pe.modaviva.app.ui.format.formatearSoles
import androidx.compose.runtime.LaunchedEffect
import java.text.Normalizer
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationSelectorBottomSheet(
    currentUser: UserProfile?,
    destinoActual: DestinoEntrega?,
    ubicacionEstado: ResultadoUbicacion?,
    tarifas: List<TarifaEnvio>,
    direcciones: List<DireccionEntrega>,
    cargandoTarifas: Boolean,
    errorTarifas: String?,
    onRetryTarifas: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (DestinoEntrega) -> Unit,
    onGpsRequested: () -> Unit,
) {
    var tipoSeleccionado by remember {
        mutableStateOf(destinoActual?.tipo ?: TipoDestino.OTRA_DIRECCION)
    }

    var distritoSeleccionado by remember {
        mutableStateOf(
            tarifas.firstOrNull {
                it.id == destinoActual?.distritoSlug ||
                        it.distrito.equals(destinoActual?.distrito, ignoreCase = true)
            }
        )
    }

    var direccionManual by remember {
        mutableStateOf(destinoActual?.direccion.orEmpty())
    }
    var referenciaManual by remember {
        mutableStateOf(destinoActual?.referencia.orEmpty())
    }

    var gpsAplicado by remember { mutableStateOf(false) }

    LaunchedEffect(ubicacionEstado, tarifas) {
        if (ubicacionEstado is ResultadoUbicacion.Cargando) {
            gpsAplicado = false
        }

        val resultado = ubicacionEstado as? ResultadoUbicacion.Exito
            ?: return@LaunchedEffect

        if (gpsAplicado || tarifas.isEmpty()) {
            return@LaunchedEffect
        }

        // Buscar coincidencia exacta entre los componentes GPS
        // y los distritos oficiales de las tarifas.
        val tarifaDetectada = tarifas.firstOrNull { tarifa ->
            resultado.componentesDistrito.any { componente ->
                normalizarTexto(componente) == normalizarTexto(tarifa.distrito)
            }
        }

        if (tarifaDetectada != null) {
            distritoSeleccionado = tarifaDetectada
        }

        // Solo completar si el GPS obtuvo una dirección.
        if (!resultado.direccionDetectada.isNullOrBlank()) {
            direccionManual = resultado.direccionDetectada
        }

        // La referencia es opcional.
        if (!resultado.referenciaDetectada.isNullOrBlank()) {
            referenciaManual = resultado.referenciaDetectada
        }

        gpsAplicado = true
    }

    var expandedDropdown by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Seleccionar destino de entrega",
                style = MaterialTheme.typography.titleLarge,
            )

            // Opción 1: Direcciones guardadas del perfil
            val esDirRegistrada = tipoSeleccionado == TipoDestino.DIRECCION_REGISTRADA
            Card(
                onClick = { tipoSeleccionado = TipoDestino.DIRECCION_REGISTRADA },
                colors = CardDefaults.cardColors(
                    containerColor = if (esDirRegistrada)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = null,
                            tint = if (esDirRegistrada) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Direcciones guardadas",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (esDirRegistrada) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (direcciones.isEmpty()) {
                        Text(
                            text = "No hay direcciones registradas en tu perfil. Puedes agregarlas en Perfil > Mis direcciones.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (esDirRegistrada) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        direcciones.forEach { dir ->
                            val esSeleccionada = tipoSeleccionado == TipoDestino.DIRECCION_REGISTRADA && direccionManual == dir.direccion
                            Card(
                                onClick = {
                                    tipoSeleccionado = TipoDestino.DIRECCION_REGISTRADA
                                    direccionManual = dir.direccion
                                    referenciaManual = dir.referencia.orEmpty()
                                    val tarifaMatch = tarifas.firstOrNull { it.id == dir.distritoSlug || it.distrito.equals(dir.distrito, ignoreCase = true) }
                                    if (tarifaMatch != null) {
                                        distritoSeleccionado = tarifaMatch
                                    }
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (esSeleccionada) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = "${dir.alias}: ${dir.direccion}, ${dir.distrito}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(text = "Destinatario: ${dir.destinatario} • ${dir.telefono}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Opción 2: Ubicación GPS
            val esGps = tipoSeleccionado == TipoDestino.UBICACION_ACTUAL_GPS
            Card(
                onClick = {
                    tipoSeleccionado = TipoDestino.UBICACION_ACTUAL_GPS
                    onGpsRequested()
                },
                colors = CardDefaults.cardColors(
                    containerColor = if (esGps)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.MyLocation,
                            contentDescription = null,
                            tint = if (esGps) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Usar mi ubicación actual (GPS)",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (esGps) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    when (ubicacionEstado) {
                        is ResultadoUbicacion.Cargando -> {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Obteniendo coordenadas GPS...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        is ResultadoUbicacion.Exito -> {
                            Text(
                                text = "Ubicación GPS obtenida correctamente.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp),
                            )

                            Text(
                                text = if (distritoSeleccionado != null) {
                                    "Distrito detectado: ${distritoSeleccionado?.distrito}"
                                } else {
                                    "No se pudo identificar el distrito exacto. Selecciónalo manualmente."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        is ResultadoUbicacion.GpsDesactivado -> {
                            Text("El GPS está desactivado. Selecciona tu distrito manualmente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        is ResultadoUbicacion.PermisoDenegado -> {
                            Text("Permiso de ubicación denegado. Selecciona tu distrito manualmente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        is ResultadoUbicacion.NoDisponible -> {
                            Text("Ubicación no disponible. Selecciona tu distrito manualmente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        null -> {
                            Text(
                                text = "Toca para obtener coordenadas y seleccionar tu distrito.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Opción 3: Selección manual / Ingreso
            val esOtraDir = tipoSeleccionado == TipoDestino.OTRA_DIRECCION
            Card(
                onClick = { tipoSeleccionado = TipoDestino.OTRA_DIRECCION },
                colors = CardDefaults.cardColors(
                    containerColor = if (esOtraDir)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = if (esOtraDir) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Selección manual de distrito",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (esOtraDir) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Selecciona tu distrito de la lista oficial de tarifas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (esOtraDir) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Selector de distrito basado en tarifas oficiales
            Text(
                text = "Distrito de entrega *",
                style = MaterialTheme.typography.titleSmall,
            )

            when {
                cargandoTarifas -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text("Cargando distritos oficiales...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                errorTarifas != null -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = errorTarifas,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(onClick = onRetryTarifas) {
                            Text("Reintentar carga")
                        }
                    }
                }
                tarifas.isEmpty() -> {
                    Text(
                        text = "No hay distritos registrados en las tarifas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = distritoSeleccionado?.let { "${it.distrito} ${if (it.cobertura) "(${formatearSoles(it.costo)})" else "(Sin cobertura)"}" } ?: "Selecciona tu distrito",
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
                            tarifas.forEach { tarifa ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(text = tarifa.distrito)
                                            Text(
                                                text = if (tarifa.cobertura) formatearSoles(tarifa.costo) else "Sin cobertura",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (tarifa.cobertura) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    },
                                    onClick = {
                                        distritoSeleccionado = tarifa
                                        expandedDropdown = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = direccionManual,
                onValueChange = { direccionManual = it },
                label = { Text("Dirección *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = referenciaManual,
                onValueChange = { referenciaManual = it },
                label = { Text("Referencia (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            MvPrimaryButton(
                text = "Confirmar destino",
                onClick = {
                    val tarifa = distritoSeleccionado
                    val distritoFinal = tarifa?.distrito.orEmpty()
                    val slug = tarifa?.id ?: ""

                    val latFinal = if (tipoSeleccionado == TipoDestino.UBICACION_ACTUAL_GPS) {
                        (ubicacionEstado as? ResultadoUbicacion.Exito)?.lat ?: destinoActual?.lat
                    } else null

                    val lngFinal = if (tipoSeleccionado == TipoDestino.UBICACION_ACTUAL_GPS) {
                        (ubicacionEstado as? ResultadoUbicacion.Exito)?.lng ?: destinoActual?.lng
                    } else null

                    val destino = DestinoEntrega(
                        tipo = tipoSeleccionado,
                        direccion = direccionManual.trim(),
                        referencia = referenciaManual.trim().ifBlank { null },
                        distrito = distritoFinal,
                        distritoSlug = slug,
                        lat = latFinal,
                        lng = lngFinal,
                    )
                    onConfirm(destino)
                    onDismiss()
                },
                enabled = distritoSeleccionado != null &&
                        distritoSeleccionado?.cobertura == true &&
                        direccionManual.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun normalizarTexto(texto: String): String {
    return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase(Locale("es", "PE"))
        .replace("\\s+".toRegex(), " ")
}
