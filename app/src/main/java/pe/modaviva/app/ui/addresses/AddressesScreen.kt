package pe.modaviva.app.ui.addresses

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.modaviva.app.domain.model.DireccionEntrega
import pe.modaviva.app.domain.model.TarifaEnvio
import pe.modaviva.app.ui.components.MvConfirmDialog
import pe.modaviva.app.ui.components.MvPrimaryButton
import pe.modaviva.app.ui.format.formatearSoles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressesScreen(
    onBack: () -> Unit,
    viewModel: AddressesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showFormDialog by remember { mutableStateOf(false) }
    var direccionEnEdicion by remember { mutableStateOf<DireccionEntrega?>(null) }
    var direccionAEliminar by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.mensajeExito, state.error) {
        state.mensajeExito?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensajes()
        }
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensajes()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis direcciones de entrega") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    direccionEnEdicion = null
                    showFormDialog = true
                },
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar dirección")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.cargando && state.direcciones.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.direcciones.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No tienes direcciones registradas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Agrega una dirección para facilitar tus compras con entrega a domicilio.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.direcciones, key = { it.id }) { direccion ->
                            DireccionCard(
                                direccion = direccion,
                                onEdit = {
                                    direccionEnEdicion = direccion
                                    showFormDialog = true
                                },
                                onDelete = { direccionAEliminar = direccion.id },
                                onSetPrincipal = { viewModel.establecerPrincipal(direccion.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        DireccionFormDialog(
            direccionInicial = direccionEnEdicion,
            tarifas = state.tarifasDisponibles,
            onDismiss = { showFormDialog = false },
            onSave = { dir ->
                viewModel.guardarDireccion(dir) {
                    showFormDialog = false
                }
            },
        )
    }

    direccionAEliminar?.let { id ->
        MvConfirmDialog(
            title = "Eliminar dirección",
            message = "¿Estás seguro de que deseas eliminar esta dirección?",
            confirmLabel = "Eliminar",
            destructive = true,
            onConfirm = {
                viewModel.eliminarDireccion(id)
                direccionAEliminar = null
            },
            onDismiss = { direccionAEliminar = null },
        )
    }
}

@Composable
private fun DireccionCard(
    direccion: DireccionEntrega,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetPrincipal: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (direccion.esPrincipal)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = direccion.alias.ifBlank { "Dirección" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (direccion.esPrincipal) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Text(
                                text = "Principal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Text(
                text = "${direccion.destinatario} • ${direccion.telefono}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = "${direccion.direccion}${if (!direccion.referencia.isNullOrBlank()) " (${direccion.referencia})" else ""}, ${direccion.distrito}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!direccion.esPrincipal) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = onSetPrincipal) {
                    Text("Marcar como principal", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DireccionFormDialog(
    direccionInicial: DireccionEntrega?,
    tarifas: List<TarifaEnvio>,
    onDismiss: () -> Unit,
    onSave: (DireccionEntrega) -> Unit,
) {
    var alias by remember { mutableStateOf(direccionInicial?.alias.orEmpty()) }
    var destinatario by remember { mutableStateOf(direccionInicial?.destinatario.orEmpty()) }
    var telefono by remember { mutableStateOf(direccionInicial?.telefono.orEmpty()) }
    var direccion by remember { mutableStateOf(direccionInicial?.direccion.orEmpty()) }
    var referencia by remember { mutableStateOf(direccionInicial?.referencia.orEmpty()) }

    var tarifaSeleccionada by remember {
        mutableStateOf(
            tarifas.firstOrNull { it.id == direccionInicial?.distritoSlug || it.distrito.equals(direccionInicial?.distrito, ignoreCase = true) }
                ?: tarifas.firstOrNull()
        )
    }
    var esPrincipal by remember { mutableStateOf(direccionInicial?.esPrincipal ?: false) }
    var expandedDropdown by remember { mutableStateOf(false) }

    var errorValidacion by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (direccionInicial == null) "Nueva dirección" else "Editar dirección", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text("Alias (ej. Casa, Trabajo) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = destinatario,
                    onValueChange = { destinatario = it },
                    label = { Text("Nombre del destinatario *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Teléfono de contacto *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = direccion,
                    onValueChange = { direccion = it },
                    label = { Text("Dirección *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = referencia,
                    onValueChange = { referencia = it },
                    label = { Text("Referencia adicional (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Text(text = "Distrito *", style = MaterialTheme.typography.titleSmall)

                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = tarifaSeleccionada?.let { "${it.distrito} ${if (it.cobertura) "(${formatearSoles(it.costo)})" else "(Sin cobertura)"}" } ?: "Selecciona tu distrito",
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
                                    tarifaSeleccionada = tarifa
                                    expandedDropdown = false
                                },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Establecer como principal", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = esPrincipal,
                        onCheckedChange = { esPrincipal = it },
                    )
                }

                errorValidacion?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (alias.isBlank() || destinatario.isBlank() || telefono.isBlank() || direccion.isBlank() || tarifaSeleccionada == null) {
                        errorValidacion = "Completa todos los campos obligatorios (*)"
                        return@Button
                    }
                    val coordsDistrito = pe.modaviva.app.domain.model.DistritoResolver.coordenadasParaDistrito(tarifaSeleccionada!!.distrito)
                    val nuevaDir = DireccionEntrega(
                        id = direccionInicial?.id.orEmpty(),
                        alias = alias.trim(),
                        destinatario = destinatario.trim(),
                        telefono = telefono.trim(),
                        direccion = direccion.trim(),
                        referencia = referencia.trim().ifBlank { null },
                        distrito = tarifaSeleccionada!!.distrito,
                        distritoSlug = tarifaSeleccionada!!.id,
                        lat = direccionInicial?.lat ?: tarifaSeleccionada?.lat ?: coordsDistrito?.first,
                        lng = direccionInicial?.lng ?: tarifaSeleccionada?.lng ?: coordsDistrito?.second,
                        esPrincipal = esPrincipal,
                    )
                    onSave(nuevaDir)
                },
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}
