package pe.modaviva.app.ui.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.modaviva.app.R
import pe.modaviva.app.domain.model.UserProfile
import pe.modaviva.app.ui.components.MvConfirmDialog

@Composable
fun SessionScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    onOrdersClick: () -> Unit = {},
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
                        title = stringResource(R.string.profile_item_addresses),
                        subtitle = stringResource(R.string.profile_item_addresses_desc),
                        onClick = {
                            soonDialogInfo = Pair(
                                "Mis direcciones",
                                "Podrás registrar y gestionar tus direcciones de entrega en Lima Metropolitana para recibir tus compras en casa."
                            )
                        },
                    )
                    ProfileItemRow(
                        icon = Icons.Outlined.Straighten,
                        title = stringResource(R.string.profile_item_measurements),
                        subtitle = stringResource(R.string.profile_item_measurements_desc),
                        onClick = {
                            soonDialogInfo = Pair(
                                "Mis medidas corporales",
                                "Podrás registrar tus medidas de busto/pecho, cintura y cadera para obtener recomendaciones automáticas de talla con el probador virtual."
                            )
                        },
                    )
                    ProfileItemRow(
                        icon = Icons.Outlined.CreditCard,
                        title = stringResource(R.string.profile_item_payments),
                        subtitle = stringResource(R.string.profile_item_payments_desc),
                        onClick = {
                            soonDialogInfo = Pair(
                                "Métodos de pago",
                                "Podrás guardar tus tarjetas de crédito/débito y vincular pagos directos con Yape o Plin de forma 100% segura."
                            )
                        },
                        showDivider = false,
                    )
                }

                // Sección: Seguridad y privacidad
                ProfileSectionTitle(title = stringResource(R.string.profile_section_security))
                ProfileGroupCard {
                    // Cambiar contraseña solo visible para usuarios con correo y contraseña (HU-03 CA-06)
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
                        text = "Esta funcionalidad estará disponible en la próxima actualización de ModaViva.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { soonDialogInfo = null }) {
                    Text(text = "Entendido")
                }
            },
        )
    }

    // Diálogo de Términos y Privacidad
    if (showTermsDialog) {
        TermsAndPrivacyDialog(onDismiss = { showTermsDialog = false })
    }

    // Diálogo Acerca de ModaViva
    if (showAboutDialog) {
        AboutModaVivaDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
private fun ProfileHeaderCard(profile: UserProfile) {
    val initials = remember(profile) {
        val first = profile.nombres.trim().firstOrNull()?.uppercase() ?: ""
        val second = profile.apellidos.trim().firstOrNull()?.uppercase() ?: ""
        "$first$second".ifEmpty { "MV" }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${profile.nombres} ${profile.apellidos}".trim(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = profile.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (profile.isGoogleUser) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (profile.isGoogleUser) {
                                Icons.Outlined.AccountCircle
                            } else {
                                Icons.Outlined.MailOutline
                            },
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (profile.isGoogleUser) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(
                                if (profile.isGoogleUser) {
                                    R.string.profile_badge_google
                                } else {
                                    R.string.profile_badge_email
                                }
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (profile.isGoogleUser) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
private fun ProfileGroupCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content,
        )
    }
}

@Composable
private fun ProfileItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    showDivider: Boolean = true,
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp),
            )
        }
    }
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 66.dp, end = 16.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )
    }
}

@Composable
private fun PersonalDataDialog(
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
                DataRow(label = "Nombres", value = profile.nombres.ifBlank { "—" })
                DataRow(label = "Apellidos", value = profile.apellidos.ifBlank { "—" })
                DataRow(label = "Correo electrónico", value = profile.email)
                DataRow(
                    label = "Teléfono celular",
                    value = profile.telefono?.takeIf { it.isNotBlank() } ?: "No registrado"
                )
                DataRow(
                    label = "Método de acceso",
                    value = if (profile.isGoogleUser) "Cuenta Google" else "Correo y contraseña"
                )
                DataRow(
                    label = "Estado del correo",
                    value = if (profile.emailVerificado || profile.isGoogleUser) {
                        "Verificado ✓"
                    } else {
                        "Pendiente de verificación"
                    }
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

@Composable
private fun ResetPasswordDialog(
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

@Composable
private fun TermsAndPrivacyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.profile_item_terms),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Términos del Servicio ModaViva",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Al usar ModaViva, accedes a nuestro catálogo de moda y sistema de compra en línea con entregas en Lima Metropolitana y recojo en tiendas físicas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Protección de Datos Personales (Ley N° 29733)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Tus datos personales y fotografías empleadas en el probador virtual están rigurosamente protegidos por la Ley de Protección de Datos Personales de la República del Perú. No son compartidos ni comercializados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Probador Virtual con Inteligencia Artificial",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Las imágenes cargadas se emplean exclusivamente para generar la simulación de prueba de prendas. Puedes eliminar tus fotos registradas en cualquier momento.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun AboutModaVivaDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.profile_about_title),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.profile_about_version),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.profile_about_company),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.profile_about_stores_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.profile_about_stores),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.profile_about_contact),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
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