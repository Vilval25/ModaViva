package pe.modaviva.app.ui.fitting

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import pe.modaviva.app.R
import pe.modaviva.app.domain.model.UserProfile
import pe.modaviva.app.ui.components.MvEmptyState
import pe.modaviva.app.ui.components.MvPrimaryButton
import pe.modaviva.app.ui.components.MvScreen
import pe.modaviva.app.ui.components.MvSecondaryButton

/**
 * Pestaña Probador Virtual (HU-10, HU-11 y HU-14).
 *
 * Flujo integrado:
 * 1. Verificación de sesión y consentimiento de privacidad (HU-10).
 * 2. Guía, captura (CameraX) y selección de galería con validación de pose (HU-11).
 * 3. Modal de registro de altura para estimación de talla (HU-14).
 * 4. Estado activo con foto registrada, altura editable y acceso a pruebas.
 */
@Composable
fun FittingTab(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onExploreClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FittingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val isSubmittingConsent by viewModel.isSubmittingConsent.collectAsStateWithLifecycle()
    val consentDeclined by viewModel.consentDeclinedTemporarily.collectAsStateWithLifecycle()
    val consentError by viewModel.consentError.collectAsStateWithLifecycle()

    val showCamera by viewModel.showCamera.collectAsStateWithLifecycle()
    val previewBitmap by viewModel.previewBitmap.collectAsStateWithLifecycle()
    val isValidatingPhoto by viewModel.isValidatingPhoto.collectAsStateWithLifecycle()
    val isUploadingPhoto by viewModel.isUploadingPhoto.collectAsStateWithLifecycle()
    val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()
    val photoError by viewModel.photoError.collectAsStateWithLifecycle()
    val isUploadError by viewModel.isUploadError.collectAsStateWithLifecycle()

    val showHeightDialog by viewModel.showHeightDialog.collectAsStateWithLifecycle()
    val isSavingHeight by viewModel.isSavingHeight.collectAsStateWithLifecycle()

    // Launcher para selector de galería (Photo Picker / MediaStore)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onGalleryPhotoSelected(uri)
        }
    }

    // Launcher para permiso de cámara (CameraX)
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.openCamera()
        }
    }

    val requestCamera: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Modal de Altura (HU-14)
    if (showHeightDialog) {
        HeightDialog(
            initialHeightCm = user?.alturaCm,
            allowSkip = true,
            isLoading = isSavingHeight,
            onDismiss = viewModel::dismissHeightDialog,
            onSave = viewModel::saveHeight,
        )
    }

    // Cámara en pantalla completa
    if (showCamera) {
        FittingCameraView(
            onPhotoCaptured = viewModel::onPhotoCaptured,
            onClose = viewModel::closeCamera,
        )
        return
    }

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

            // Con sesión pero sin consentimiento de privacidad vigente (HU-10)
            !currentUser.tieneConsentimientoProbador -> {
                FittingConsentScreen(
                    isSubmitting = isSubmittingConsent,
                    errorMessage = consentError,
                    onAccept = { viewModel.acceptConsent() },
                    onDecline = viewModel::declineConsent,
                    modifier = Modifier.padding(padding),
                )
            }

            // Vista previa de confirmación y subida de foto capturada o elegida (HU-11 CA-02, CA-03, CA-04)
            previewBitmap != null -> {
                FittingPhotoPreviewScreen(
                    bitmap = previewBitmap!!,
                    isValidating = isValidatingPhoto,
                    isUploading = isUploadingPhoto,
                    uploadProgress = uploadProgress,
                    errorMessage = photoError,
                    isUploadError = isUploadError,
                    onConfirm = viewModel::confirmAndUploadPhoto,
                    onRetake = viewModel::retakePhoto,
                    modifier = Modifier.padding(padding),
                )
            }

            // Con consentimiento pero aún sin foto registrada (HU-11 CA-01)
            currentUser.fotoProbadorUrl.isNullOrBlank() -> {
                FittingPhotoGuideScreen(
                    errorMessage = photoError,
                    onTakePhotoClick = requestCamera,
                    onChooseGalleryClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.padding(padding),
                )
            }

            // Foto registrada y probador listo para usar (HU-11 + HU-14)
            else -> {
                FittingActivePhotoScreen(
                    photoUrl = currentUser.fotoProbadorUrl!!,
                    alturaCm = currentUser.alturaCm,
                    onEditHeightClick = viewModel::openHeightDialog,
                    onChangePhotoClick = requestCamera,
                    onExploreClick = onExploreClick,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

/**
 * Guía de fotografía para el probador (HU-11 CA-01).
 */
@Composable
private fun FittingPhotoGuideScreen(
    errorMessage: String?,
    onTakePhotoClick: () -> Unit,
    onChooseGalleryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        }

        Text(
            text = stringResource(R.string.fitting_photo_guide_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Text(
            text = stringResource(R.string.fitting_photo_guide_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        // Consejos con iconos
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                TipItem(
                    icon = Icons.Outlined.Person,
                    title = stringResource(R.string.fitting_photo_tip_body_title),
                    description = stringResource(R.string.fitting_photo_tip_body_desc),
                )
                TipItem(
                    icon = Icons.Outlined.Lightbulb,
                    title = stringResource(R.string.fitting_photo_tip_light_title),
                    description = stringResource(R.string.fitting_photo_tip_light_desc),
                )
                TipItem(
                    icon = Icons.Outlined.Wallpaper,
                    title = stringResource(R.string.fitting_photo_tip_bg_title),
                    description = stringResource(R.string.fitting_photo_tip_bg_desc),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        MvPrimaryButton(
            text = stringResource(R.string.fitting_take_photo_btn),
            onClick = onTakePhotoClick,
            modifier = Modifier.fillMaxWidth(),
        )

        MvSecondaryButton(
            text = stringResource(R.string.fitting_choose_gallery_btn),
            onClick = onChooseGalleryClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TipItem(
    icon: ImageVector,
    title: String,
    description: String,
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Pantalla de vista previa y confirmación de la foto (HU-11 CA-02, CA-03, CA-04).
 */
@Composable
private fun FittingPhotoPreviewScreen(
    bitmap: Bitmap,
    isValidating: Boolean,
    isUploading: Boolean,
    uploadProgress: Float,
    errorMessage: String?,
    isUploadError: Boolean = false,
    onConfirm: () -> Unit,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.fitting_photo_preview_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        // Vista previa de la foto seleccionada
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Foto capturada",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            if (isValidating) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp),
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.fitting_photo_validating),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // Estado de subida de fotografía con barra de progreso (CA-04)
        if (isUploading) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LinearProgressIndicator(
                    progress = { uploadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${stringResource(R.string.fitting_photo_uploading)} ${(uploadProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Mensaje de rechazo / error específico (CA-02, CA-03)
        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botón Confirmar y usar foto / Reintentar subida
        val canConfirm = (errorMessage == null || isUploadError) && !isValidating && !isUploading
        MvPrimaryButton(
            text = if (isUploadError) "Reintentar subida" else stringResource(R.string.fitting_photo_use_btn),
            onClick = onConfirm,
            enabled = canConfirm,
            loading = isUploading,
            modifier = Modifier.fillMaxWidth(),
        )

        // Botón Repetir / Tomar otra
        MvSecondaryButton(
            text = stringResource(R.string.fitting_photo_retake_btn),
            onClick = onRetake,
            enabled = !isUploading,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Pantalla cuando la foto ya está registrada y validada (HU-11 CA-05 + HU-14 CA-01).
 */
@Composable
private fun FittingActivePhotoScreen(
    photoUrl: String,
    alturaCm: Int?,
    onEditHeightClick: () -> Unit,
    onChangePhotoClick: () -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Tarjeta con foto registrada
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
        ) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Foto registrada",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Foto lista para probar",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Tarjeta de información de altura (HU-14)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Straighten,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (alturaCm != null) "Altura: $alturaCm cm" else "Altura no registrada",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = if (alturaCm != null) "Recomendación de talla activada" else "Registra tu altura para sugerirte talla",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(onClick = onEditHeightClick) {
                    Text(if (alturaCm != null) "Editar" else "Registrar")
                }
            }
        }

        // Botón para cambiar fotografía (CA-05)
        OutlinedButton(
            onClick = onChangePhotoClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.fitting_change_photo_btn))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botón principal: Explorar catálogo para probar prendas
        MvPrimaryButton(
            text = "Explorar prendas para probar",
            onClick = onExploreClick,
            modifier = Modifier.fillMaxWidth(),
        )
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
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        }

        Text(
            text = stringResource(R.string.fitting_consent_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Text(
            text = stringResource(R.string.fitting_consent_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        ) {
            Text(
                text = stringResource(R.string.fitting_consent_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }

        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = acceptedCheckbox,
                onCheckedChange = { acceptedCheckbox = it },
                enabled = !isSubmitting,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.fitting_consent_checkbox),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        MvPrimaryButton(
            text = stringResource(R.string.fitting_consent_accept),
            onClick = onAccept,
            enabled = acceptedCheckbox && !isSubmitting,
            loading = isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )

        MvSecondaryButton(
            text = stringResource(R.string.fitting_consent_not_now),
            onClick = onDecline,
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
