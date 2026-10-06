package pe.modaviva.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LockPerson
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.modaviva.app.R

/**
 * Hoja modal "Inicia sesión para continuar" (HU-02 CA-02).
 *
 * Aparece cuando un usuario invitado intenta usar una acción que requiere cuenta
 * (usar el probador, abrir o marcar favoritos, abrir notificaciones, abrir Mis pedidos o pagar).
 * Con 'Iniciar sesión' o 'Crear cuenta' se dirige a la pantalla de acceso correspondiente.
 * Si se cierra (onDismiss), el usuario continúa navegando como invitado sin perder su pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MvAuthPromptBottomSheet(
    onDismiss: () -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    title: String = stringResource(R.string.auth_prompt_title),
    message: String = stringResource(R.string.auth_prompt_body_default),
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.LockPerson,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Spacer(modifier = Modifier.height(4.dp))

            MvPrimaryButton(
                text = stringResource(R.string.profile_guest_login),
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth(),
            )

            MvSecondaryButton(
                text = stringResource(R.string.profile_guest_register),
                onClick = onRegisterClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
