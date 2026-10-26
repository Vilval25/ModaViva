package pe.modaviva.app.ui.fitting

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.modaviva.app.R
import pe.modaviva.app.domain.model.UserProfile
import pe.modaviva.app.ui.components.MvEmptyState
import pe.modaviva.app.ui.components.MvPrimaryButton
import pe.modaviva.app.ui.components.MvScreen
import pe.modaviva.app.ui.components.MvSecondaryButton

/**
 * Pestaña Probador Virtual (HU-10).
 *
 * Implementa los criterios de aceptación:
 * - CA-01: Sin sesión muestra explicación breve y opciones Iniciar sesión / Crear cuenta.
 * - CA-02: Con sesión y sin consentimiento muestra el aviso de privacidad (Ley N° 29733).
 * - CA-03: Casilla desmarcada por defecto y botón 'Aceptar y continuar' deshabilitado hasta marcarla.
 * - CA-04: Guarda consentimiento con identificador, fecha y versión.
 * - CA-05: 'Ahora no' permite seguir navegando y comprar normalmente.
 * - CA-07: Si cambia la versión del aviso, se solicita aceptarla nuevamente.
 */
@Composable
fun FittingTab(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onExploreClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FittingViewModel = hiltViewModel(),
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmittingConsent.collectAsStateWithLifecycle()
    val consentDeclined by viewModel.consentDeclinedTemporarily.collectAsStateWithLifecycle()
    val consentError by viewModel.consentError.collectAsStateWithLifecycle()

    MvScreen(
        title = stringResource(R.string.fitting_title),
        modifier = modifier,
    ) { padding ->
        val currentUser = user
        when {
            // CA-01: Sin sesión activa
            currentUser == null -> {
                MvEmptyState(
                    icon = Icons.Outlined.Checkroom,
                    title = stringResource(R.string.fitting_guest_title),
                    message = stringResource(R.string.fitting_guest_body),
                    modifier = Modifier.padding(padding),
                ) {
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

            // CA-05: El cliente eligió 'Ahora no'
            consentDeclined -> {
                MvEmptyState(
                    icon = Icons.Outlined.Shield,
                    title = stringResource(R.string.fitting_consent_declined_badge),
                    message = stringResource(R.string.fitting_consent_declined_notice),
                    modifier = Modifier.padding(padding),
                ) {
                    MvPrimaryButton(
                        text = stringResource(R.string.fitting_consent_grant_button),
                        onClick = viewModel::requestConsentAgain,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    MvSecondaryButton(
                        text = "Ir al catálogo",
                        onClick = onExploreClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // CA-02 / CA-07: Con sesión activa pero sin consentimiento vigente
            !currentUser.tieneConsentimientoProbador -> {
                FittingConsentScreen(
                    isSubmitting = isSubmitting,
                    errorMessage = consentError,
                    onAccept = { viewModel.acceptConsent() },
                    onDecline = viewModel::declineConsent,
                    modifier = Modifier.padding(padding),
                )
            }

            // Probador con consentimiento vigente
            else -> {
                FittingActiveScreen(
                    version = currentUser.consentimientoProbadorVersion ?: UserProfile.PROBADOR_AVISO_VERSION_ACTUAL,
                    onExploreClick = onExploreClick,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

/**
 * Pantalla de aviso de privacidad para el probador virtual (HU-10 CA-02 y CA-03).
 */
@Composable
private fun FittingConsentScreen(
    isSubmitting: Boolean,
    errorMessage: String?,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var acceptedCheckbox by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Icono de cabecera con estilo editorial
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.PrivacyTip,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp),
            )
        }

        Text(
            text = stringResource(R.string.fitting_consent_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = stringResource(R.string.fitting_consent_subtitle),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        // Tarjeta con el texto legal detallado
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.fitting_consent_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                )
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        // CA-03: Casilla de consentimiento (desmarcada por defecto)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { acceptedCheckbox = !acceptedCheckbox }
                .padding(vertical = 4.dp),
        ) {
            Checkbox(
                checked = acceptedCheckbox,
                onCheckedChange = { acceptedCheckbox = it },
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.fitting_consent_checkbox),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // CA-03 y CA-04: Botón Aceptar y continuar (deshabilitado hasta marcar la casilla)
        MvPrimaryButton(
            text = stringResource(R.string.fitting_consent_accept),
            onClick = onAccept,
            enabled = acceptedCheckbox && !isSubmitting,
            loading = isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )

        // CA-05: Botón Ahora no
        MvSecondaryButton(
            text = stringResource(R.string.fitting_consent_not_now),
            onClick = onDecline,
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Pantalla que se muestra cuando el usuario ya otorgó su consentimiento.
 */
@Composable
private fun FittingActiveScreen(
    version: String,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.fitting_active_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.fitting_active_body, version),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        MvPrimaryButton(
            text = "Explorar prendas para probar",
            onClick = onExploreClick,
            modifier = Modifier.fillMaxWidth(0.8f),
        )
    }
}
