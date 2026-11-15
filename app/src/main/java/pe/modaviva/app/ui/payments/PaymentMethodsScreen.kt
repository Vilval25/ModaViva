package pe.modaviva.app.ui.payments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.modaviva.app.domain.model.TarjetaGuardada
import pe.modaviva.app.ui.components.MvConfirmDialog
import pe.modaviva.app.ui.components.MvSnackbarHost

/** HU-21 — Perfil > Métodos de pago: lista, agregar y eliminar tarjetas (CA-04, CA-05, CA-06). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsScreen(
    onBack: () -> Unit,
    viewModel: PaymentMethodsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var tarjetaAEliminar by remember { mutableStateOf<TarjetaGuardada?>(null) }

    LaunchedEffect(state.mensaje) {
        state.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.mensajeMostrado()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Métodos de pago") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::abrirFormulario,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Agregar tarjeta") },
            )
        },
        snackbarHost = { MvSnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.cargando -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.tarjetas.isEmpty() -> SinTarjetas(modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { AvisoSeguridad() }
                    items(state.tarjetas, key = { it.id }) { tarjeta ->
                        TarjetaCard(
                            tarjeta = tarjeta,
                            onEliminar = { tarjetaAEliminar = tarjeta },
                            onPredeterminada = { viewModel.hacerPredeterminada(tarjeta) },
                        )
                    }
                }
            }
        }
    }

    state.formulario?.let { form ->
        AgregarTarjetaDialog(
            form = form,
            onNumero = viewModel::onNumero,
            onVencimiento = viewModel::onVencimiento,
            onCvv = viewModel::onCvv,
            onTitular = viewModel::onTitular,
            onEsDebito = viewModel::onEsDebito,
            onGuardar = viewModel::guardarTarjeta,
            onCerrar = viewModel::cerrarFormulario,
        )
    }

    tarjetaAEliminar?.let { tarjeta ->
        MvConfirmDialog(
            title = "Eliminar tarjeta",
            message = "¿Eliminar la tarjeta ${tarjeta.etiquetaResumen}? Ya no aparecerá al momento de pagar.",
            confirmLabel = "Eliminar",
            destructive = true,
            onConfirm = {
                viewModel.eliminar(tarjeta)
                tarjetaAEliminar = null
            },
            onDismiss = { tarjetaAEliminar = null },
        )
    }
}

@Composable
private fun SinTarjetas(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.CreditCard,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No tienes tarjetas guardadas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Guarda una tarjeta para pagar más rápido sin volver a escribir sus datos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AvisoSeguridad() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Solo guardamos la marca, los últimos 4 dígitos y el vencimiento. " +
                "Los datos completos los procesa la pasarela de pago.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TarjetaCard(
    tarjeta: TarjetaGuardada,
    onEliminar: () -> Unit,
    onPredeterminada: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.CreditCard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tarjeta.etiquetaResumen,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${if (tarjeta.tipo == "DEBITO") "Débito" else "Crédito"} • Vence ${tarjeta.vencimiento}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (tarjeta.esPredeterminada) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Predeterminada") },
                        modifier = Modifier.height(28.dp),
                    )
                } else {
                    Text(
                        text = "Usar como predeterminada",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clickable(onClick = onPredeterminada),
                    )
                }
            }
            IconButton(onClick = onEliminar) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Eliminar ${tarjeta.etiquetaResumen}",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun AgregarTarjetaDialog(
    form: FormularioTarjeta,
    onNumero: (String) -> Unit,
    onVencimiento: (String) -> Unit,
    onCvv: (String) -> Unit,
    onTitular: (String) -> Unit,
    onEsDebito: (Boolean) -> Unit,
    onGuardar: () -> Unit,
    onCerrar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Agregar tarjeta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !form.esDebito,
                        onClick = { onEsDebito(false) },
                        label = { Text("Crédito") },
                    )
                    FilterChip(
                        selected = form.esDebito,
                        onClick = { onEsDebito(true) },
                        label = { Text("Débito") },
                    )
                }
                OutlinedTextField(
                    value = form.numero,
                    onValueChange = onNumero,
                    label = { Text("Número de tarjeta") },
                    placeholder = { Text("4111 1111 1111 1111") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = form.vencimiento,
                        onValueChange = onVencimiento,
                        label = { Text("MM/AA") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = form.cvv,
                        onValueChange = onCvv,
                        label = { Text("CVV") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = form.titular,
                    onValueChange = onTitular,
                    label = { Text("Titular de la tarjeta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                form.error?.let { error ->
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onGuardar, enabled = !form.enviando) {
                if (form.enviando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Validando…")
                } else {
                    Text("Guardar tarjeta")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar, enabled = !form.enviando) { Text("Cancelar") }
        },
    )
}
