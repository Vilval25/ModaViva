package pe.modaviva.app.ui.session.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.modaviva.app.R
import pe.modaviva.app.domain.model.UserProfile

@Composable
fun PersonalDataDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.profile_dialog_personal_title),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DataRow(
                    label = stringResource(R.string.field_nombres),
                    value = profile.nombres.ifBlank { "—" },
                )
                DataRow(
                    label = stringResource(R.string.field_apellidos),
                    value = profile.apellidos.ifBlank { "—" },
                )
                DataRow(
                    label = stringResource(R.string.field_email),
                    value = profile.email,
                )
                DataRow(
                    label = stringResource(R.string.field_telefono),
                    value = profile.telefono?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.profile_value_not_registered),
                )
                DataRow(
                    label = stringResource(R.string.profile_label_auth_method),
                    value = if (profile.isGoogleUser) {
                        stringResource(R.string.profile_value_google)
                    } else {
                        stringResource(R.string.profile_value_email_pass)
                    },
                )
                DataRow(
                    label = stringResource(R.string.profile_label_email_status),
                    value = if (profile.emailVerificado || profile.isGoogleUser) {
                        stringResource(R.string.profile_status_verified)
                    } else {
                        stringResource(R.string.profile_status_pending)
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.profile_dialog_close))
            }
        },
    )
}

@Composable
private fun DataRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
