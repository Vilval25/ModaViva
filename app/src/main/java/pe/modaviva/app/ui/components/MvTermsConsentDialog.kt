package pe.modaviva.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.modaviva.app.R
import pe.modaviva.app.domain.model.UserProfile

@Composable
fun MvTermsConsentDialog(
    userProfile: UserProfile,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.terms_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val greeting = if (userProfile.nombres.isNotBlank()) {
                    stringResource(R.string.terms_dialog_welcome_name, userProfile.nombres)
                } else {
                    stringResource(R.string.terms_dialog_welcome)
                }
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.terms_dialog_body, userProfile.email),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.terms_dialog_legal_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        },
        confirmButton = {
            MvPrimaryButton(
                text = stringResource(R.string.terms_dialog_accept),
                onClick = onAccept,
                loading = isLoading,
            )
        },
        dismissButton = {
            MvTextButton(
                text = stringResource(R.string.terms_dialog_decline),
                onClick = onDismiss,
                enabled = !isLoading,
            )
        },
    )
}
