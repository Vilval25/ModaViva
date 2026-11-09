package pe.modaviva.app.ui.fitting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pe.modaviva.app.R
import pe.modaviva.app.ui.components.MvPrimaryButton

/**
 * Modal de registro y edición de altura (HU-14).
 *
 * Cumple:
 * - CA-02: Explica el propósito (recomendar talla aproximada).
 * - CA-03: Valida rango 100–230 cm.
 * - CA-04: Botón 'Omitir' cuando allowSkip = true.
 * - CA-05: Reutilizable en Perfil > Mis medidas.
 */
@Composable
fun HeightDialog(
    initialHeightCm: Int?,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
    allowSkip: Boolean = true,
    isLoading: Boolean = false,
) {
    var heightText by remember { mutableStateOf(initialHeightCm?.toString() ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    val validateAndSave: () -> Unit = {
        val parsed = heightText.toIntOrNull()
        if (parsed == null || parsed !in 100..230) {
            errorMessage = "Ingresa una altura válida entre 100 y 230 cm"
        } else {
            errorMessage = null
            onSave(parsed)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Straighten,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = stringResource(R.string.height_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.height_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = heightText,
                    onValueChange = { input ->
                        if (input.length <= 3 && input.all { it.isDigit() }) {
                            heightText = input
                            errorMessage = null
                        }
                    },
                    label = { Text(stringResource(R.string.height_input_label)) },
                    placeholder = { Text("Ej. 168") },
                    suffix = { Text("cm") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            validateAndSave()
                        }
                    ),
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { error ->
                        { Text(text = error, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            MvPrimaryButton(
                text = stringResource(R.string.height_save_button),
                onClick = validateAndSave,
                loading = isLoading,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text(
                    if (allowSkip) stringResource(R.string.height_skip_button)
                    else "Cancelar"
                )
            }
        },
    )
}

