package pe.modaviva.app.ui.session.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.modaviva.app.R

@Composable
fun ResetPasswordDialog(
    email: String,
    isSending: Boolean,
    isSent: Boolean,
    errorMessage: String?,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.profile_dialog_reset_title),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isSent) {
                    Text(
                        text = stringResource(R.string.profile_dialog_reset_success),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.profile_dialog_reset_message, email),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isSent) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.profile_dialog_close))
                }
            } else {
                TextButton(
                    onClick = onSend,
                    enabled = !isSending,
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Text(text = stringResource(R.string.profile_dialog_reset_confirm))
                    }
                }
            }
        },
        dismissButton = {
            if (!isSent) {
                TextButton(onClick = onDismiss, enabled = !isSending) {
                    Text(text = stringResource(R.string.dialog_cancel))
                }
            }
        },
    )
}
