package pe.modaviva.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.modaviva.app.R
import pe.modaviva.app.domain.model.UserProfile

/**
 * Diálogo obligatorio de consentimiento legal para usuarios nuevos o que aún no
 * registraron la aceptación de Términos del Servicio y Política de Privacidad (Ley N° 29733).
 * Muestra el contenido completo con scroll para lectura previa a la aceptación.
 */
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
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
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
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Para continuar con tu cuenta (${userProfile.email}), por favor revisa las condiciones del servicio y tratamiento de datos personales:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Contenedor con scroll para leer los términos completos antes de aceptar
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                ) {
                    Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        MvTermsContent()
                    }
                }
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
