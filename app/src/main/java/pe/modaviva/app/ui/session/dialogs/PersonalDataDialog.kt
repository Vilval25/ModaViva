package pe.modaviva.app.ui.session.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pe.modaviva.app.R
import pe.modaviva.app.domain.model.UserProfile
import pe.modaviva.app.ui.components.MvTextField

/**
 * Diálogo interactivo para editar datos personales del cliente (HU-03).
 *
 * Permite actualizar nombres, apellidos y teléfono. El correo electrónico se muestra
 * de solo lectura porque es el identificador principal de la cuenta (CA-05).
 */
@Composable
fun PersonalDataDialog(
    profile: UserProfile,
    isUpdating: Boolean,
    errorMessage: String?,
    onSave: (nombres: String, apellidos: String, telefono: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var nombres by remember(profile) { mutableStateOf(profile.nombres) }
    var apellidos by remember(profile) { mutableStateOf(profile.apellidos) }
    var telefono by remember(profile) { mutableStateOf(profile.telefono.orEmpty()) }

    var nombresTouched by remember { mutableStateOf(false) }
    var apellidosTouched by remember { mutableStateOf(false) }
    var telefonoTouched by remember { mutableStateOf(false) }

    val nombresClean = nombres.trim()
    val apellidosClean = apellidos.trim()
    val telefonoClean = telefono.trim()

    val nombresError = if (nombresTouched && nombresClean.isEmpty()) {
        stringResource(R.string.profile_edit_error_empty)
    } else null

    val apellidosError = if (apellidosTouched && apellidosClean.isEmpty()) {
        stringResource(R.string.profile_edit_error_empty)
    } else null

    val telefonoError = if (telefonoTouched && telefonoClean.isNotEmpty() && !telefonoClean.matches(Regex("^[0-9]{9}$"))) {
        stringResource(R.string.profile_edit_error_phone)
    } else null

    val isFormValid = nombresClean.isNotEmpty() &&
        apellidosClean.isNotEmpty() &&
        (telefonoClean.isEmpty() || telefonoClean.matches(Regex("^[0-9]{9}$")))

    AlertDialog(
        onDismissRequest = {
            if (!isUpdating) onDismiss()
        },
        title = {
            Text(
                text = stringResource(R.string.profile_dialog_personal_title),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }

                MvTextField(
                    value = nombres,
                    onValueChange = { nombres = it },
                    label = stringResource(R.string.field_nombres),
                    errorMessage = nombresError,
                    enabled = !isUpdating,
                    onFocusLost = { nombresTouched = true },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                MvTextField(
                    value = apellidos,
                    onValueChange = { apellidos = it },
                    label = stringResource(R.string.field_apellidos),
                    errorMessage = apellidosError,
                    enabled = !isUpdating,
                    onFocusLost = { apellidosTouched = true },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                MvTextField(
                    value = profile.email,
                    onValueChange = {},
                    label = stringResource(R.string.field_email),
                    enabled = false,
                    supportingText = stringResource(R.string.profile_edit_email_notice),
                    modifier = Modifier.fillMaxWidth(),
                )

                MvTextField(
                    value = telefono,
                    onValueChange = { input ->
                        if (input.length <= 9 && input.all { it.isDigit() }) {
                            telefono = input
                        }
                    },
                    label = stringResource(R.string.field_telefono_optional),
                    errorMessage = telefonoError,
                    enabled = !isUpdating,
                    onFocusLost = { telefonoTouched = true },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    nombresTouched = true
                    apellidosTouched = true
                    telefonoTouched = true
                    if (isFormValid && !isUpdating) {
                        onSave(nombresClean, apellidosClean, telefonoClean.takeIf { it.isNotBlank() })
                    }
                },
                enabled = isFormValid && !isUpdating,
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.profile_edit_saving))
                } else {
                    Text(text = stringResource(R.string.profile_edit_save))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUpdating,
            ) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}
