package pe.modaviva.app.ui.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.modaviva.app.R
import pe.modaviva.app.ui.components.MvConfirmDialog
import pe.modaviva.app.ui.components.MvTermsDialog
import pe.modaviva.app.ui.session.components.ProfileGroupCard
import pe.modaviva.app.ui.session.components.ProfileHeaderCard
import pe.modaviva.app.ui.session.components.ProfileItemRow
import pe.modaviva.app.ui.session.components.ProfileSectionTitle
import pe.modaviva.app.ui.session.dialogs.AboutModaVivaDialog
import pe.modaviva.app.ui.session.dialogs.PersonalDataDialog
import pe.modaviva.app.ui.session.dialogs.ResetPasswordDialog

@Composable
fun SessionScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val isSendingReset by viewModel.isSendingPasswordReset.collectAsStateWithLifecycle()
    val resetSent by viewModel.passwordResetSent.collectAsStateWithLifecycle()
    val resetError by viewModel.passwordResetError.collectAsStateWithLifecycle()

    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showPersonalDataDialog by remember { mutableStateOf(false) }
    var showResetPasswordDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var soonDialogInfo by remember { mutableStateOf<Pair<String, String>?>(null) }

    val profile = user

    // Textos para diálogos de Próximamente desde strings.xml
    val addressesTitle = stringResource(R.string.profile_item_addresses)
    val addressesDesc = stringResource(R.string.profile_addresses_soon_desc)
    val measurementsTitle = stringResource(R.string.profile_item_measurements)
    val measurementsDesc = stringResource(R.string.profile_measurements_soon_desc)
    val paymentsTitle = stringResource(R.string.profile_item_payments)
    val paymentsDesc = stringResource(R.string.profile_payments_soon_desc)

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (profile == null) {
                Text(
                    text = stringResource(R.string.session_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                // Header: Avatar, Nombres, Correo, Insignia de método de acceso
                ProfileHeaderCard(profile = profile)

                // Alerta de correo no verificado
                if (!profile.emailVerificado && !profile.isGoogleUser) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.session_email_pending),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }

                // Sección: Mi cuenta
                ProfileSectionTitle(title = stringResource(R.string.profile_section_account))
                ProfileGroupCard {
                    ProfileItemRow(
                        icon = Icons.Outlined.Person,
                        title = stringResource(R.string.profile_item_personal_data),
                        subtitle = stringResource(R.string.profile_item_personal_data_desc),
                        onClick = { showPersonalDataDialog = true },
                    )
                    ProfileItemRow(
                        icon = Icons.Outlined.LocationOn,
                        title = addressesTitle,
                        subtitle = stringResource(R.string.profile_item_addresses_desc),
                        onClick = {
                            soonDialogInfo = Pair(addressesTitle, addressesDesc)
                        },
                    )
                    ProfileItemRow(
                        icon = Icons.Outlined.Straighten,
                        title = measurementsTitle,
                        subtitle = stringResource(R.string.profile_item_measurements_desc),
                        onClick = {
                            soonDialogInfo = Pair(measurementsTitle, measurementsDesc)
                        },
                    )
                    ProfileItemRow(
                        icon = Icons.Outlined.CreditCard,
                        title = paymentsTitle,
                        subtitle = stringResource(R.string.profile_item_payments_desc),
                        onClick = {
                            soonDialogInfo = Pair(paymentsTitle, paymentsDesc)
                        },
                        showDivider = false,
                    )
                }

                // Sección: Seguridad y privacidad
                ProfileSectionTitle(title = stringResource(R.string.profile_section_security))
                ProfileGroupCard {
                    if (!profile.isGoogleUser) {
                        ProfileItemRow(
                            icon = Icons.Outlined.Lock,
                            title = stringResource(R.string.profile_item_change_password),
                            subtitle = stringResource(R.string.profile_item_change_password_desc),
                            onClick = {
                                viewModel.dismissPasswordResetState()
                                showResetPasswordDialog = true
                            },
                        )
                    }
                    ProfileItemRow(
                        icon = Icons.Outlined.Shield,
                        title = stringResource(R.string.profile_item_terms),
                        subtitle = stringResource(R.string.profile_item_terms_desc),
                        onClick = { showTermsDialog = true },
                        showDivider = false,
                    )
                }

                // Sección: Información
                ProfileSectionTitle(title = stringResource(R.string.profile_section_info))
                ProfileGroupCard {
                    ProfileItemRow(
                        icon = Icons.Outlined.Info,
                        title = stringResource(R.string.profile_item_about),
                        subtitle = stringResource(R.string.profile_item_about_desc),
                        onClick = { showAboutDialog = true },
                        showDivider = false,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Botón Cerrar sesión con confirmación
                OutlinedButton(
                    onClick = { showSignOutConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.session_signout),
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Diálogo de Confirmación para Cerrar Sesión
    if (showSignOutConfirm) {
        MvConfirmDialog(
            title = stringResource(R.string.profile_dialog_signout_title),
            message = stringResource(R.string.profile_dialog_signout_message),
            confirmLabel = stringResource(R.string.profile_dialog_signout_confirm),
            destructive = true,
            onConfirm = {
                showSignOutConfirm = false
                viewModel.signOut()
                onSignOut()
            },
            onDismiss = { showSignOutConfirm = false },
        )
    }

    // Diálogo de Datos Personales
    if (showPersonalDataDialog && profile != null) {
        PersonalDataDialog(
            profile = profile,
            onDismiss = { showPersonalDataDialog = false },
        )
    }

    // Diálogo de Cambiar Contraseña
    if (showResetPasswordDialog && profile != null) {
        ResetPasswordDialog(
            email = profile.email,
            isSending = isSendingReset,
            isSent = resetSent,
            errorMessage = resetError,
            onSend = { viewModel.sendPasswordReset(profile.email) },
            onDismiss = {
                showResetPasswordDialog = false
                viewModel.dismissPasswordResetState()
            },
        )
    }

    // Diálogo de Próximamente (Direcciones, Medidas, Pagos)
    soonDialogInfo?.let { (title, description) ->
        AlertDialog(
            onDismissRequest = { soonDialogInfo = null },
            title = { Text(text = title, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.profile_soon_footer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { soonDialogInfo = null }) {
                    Text(text = stringResource(R.string.dialog_understood))
                }
            },
        )
    }

    // Diálogo de Términos y Privacidad
    if (showTermsDialog) {
        MvTermsDialog(onDismiss = { showTermsDialog = false })
    }

    // Diálogo Acerca de ModaViva
    if (showAboutDialog) {
        AboutModaVivaDialog(onDismiss = { showAboutDialog = false })
    }
}