package pe.modaviva.app.ui.fitting

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.modaviva.app.data.network.MonitorDeConexion
import pe.modaviva.app.domain.model.UserProfile
import pe.modaviva.app.domain.repository.AuthRepository
import pe.modaviva.app.domain.repository.FittingRepository
import pe.modaviva.app.domain.repository.SessionRepository
import javax.inject.Inject

@HiltViewModel
class FittingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository,
    private val fittingRepository: FittingRepository,
    private val monitorDeConexion: MonitorDeConexion,
) : ViewModel() {

    val currentUser: StateFlow<UserProfile?> = sessionRepository.currentUser

    // HU-10: Consentimiento
    private val _isSubmittingConsent = MutableStateFlow(false)
    val isSubmittingConsent: StateFlow<Boolean> = _isSubmittingConsent.asStateFlow()

    private val _consentDeclinedTemporarily = MutableStateFlow(false)
    val consentDeclinedTemporarily: StateFlow<Boolean> = _consentDeclinedTemporarily.asStateFlow()

    private val _consentError = MutableStateFlow<String?>(null)
    val consentError: StateFlow<String?> = _consentError.asStateFlow()

    // HU-11: Registro de Fotografía
    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    private val _isValidatingPhoto = MutableStateFlow(false)
    val isValidatingPhoto: StateFlow<Boolean> = _isValidatingPhoto.asStateFlow()

    private val _photoError = MutableStateFlow<String?>(null)
    val photoError: StateFlow<String?> = _photoError.asStateFlow()

    private val _isUploadError = MutableStateFlow(false)
    val isUploadError: StateFlow<Boolean> = _isUploadError.asStateFlow()

    private val _isUploadingPhoto = MutableStateFlow(false)
    val isUploadingPhoto: StateFlow<Boolean> = _isUploadingPhoto.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    private val _showCamera = MutableStateFlow(false)
    val showCamera: StateFlow<Boolean> = _showCamera.asStateFlow()

    // HU-14: Registro de Altura
    private val _showHeightDialog = MutableStateFlow(false)
    val showHeightDialog: StateFlow<Boolean> = _showHeightDialog.asStateFlow()

    private val _isSavingHeight = MutableStateFlow(false)
    val isSavingHeight: StateFlow<Boolean> = _isSavingHeight.asStateFlow()

    private val _heightError = MutableStateFlow<String?>(null)
    val heightError: StateFlow<String?> = _heightError.asStateFlow()

    fun acceptConsent(onSuccess: () -> Unit = {}) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            _isSubmittingConsent.value = true
            _consentError.value = null
            val result = authRepository.acceptProbadorConsent(UserProfile.PROBADOR_AVISO_VERSION_ACTUAL)
            _isSubmittingConsent.value = false
            result.onSuccess {
                val updated = user.copy(
                    consentimientoProbadorAceptado = true,
                    consentimientoProbadorVersion = UserProfile.PROBADOR_AVISO_VERSION_ACTUAL,
                )
                sessionRepository.signIn(updated)
                _consentDeclinedTemporarily.value = false
                onSuccess()
            }.onFailure { e ->
                _consentError.value = e.localizedMessage ?: "No se pudo registrar el consentimiento."
            }
        }
    }

    fun declineConsent() {
        _consentDeclinedTemporarily.value = true
    }

    fun requestConsentAgain() {
        _consentDeclinedTemporarily.value = false
    }

    fun dismissError() {
        _consentError.value = null
        _photoError.value = null
        _heightError.value = null
    }

    fun openCamera() {
        _photoError.value = null
        _showCamera.value = true
    }

    fun closeCamera() {
        _showCamera.value = false
    }

    fun onPhotoCaptured(bitmap: Bitmap) {
        _showCamera.value = false
        _previewBitmap.value?.recycle()
        _previewBitmap.value = bitmap
        _photoError.value = null
        _isUploadError.value = false

        // Validar que la foto capturada detecte cuerpo completo (HU-11 CA-03)
        viewModelScope.launch {
            _isValidatingPhoto.value = true
            val poseResult = fittingRepository.validatePose(bitmap)
            _isValidatingPhoto.value = false
            poseResult.onFailure { error ->
                _photoError.value = error.message ?: "Necesitamos ver tu cuerpo completo, de pies a cabeza"
            }
        }
    }

    fun onGalleryPhotoSelected(uri: Uri) {
        viewModelScope.launch {
            _isValidatingPhoto.value = true
            _photoError.value = null
            _isUploadError.value = false

            // 1. Validar metadata: formato, tamaño, dimensiones (HU-11 CA-02)
            val metaResult = fittingRepository.validateImageMetadata(uri)
            if (metaResult.isFailure) {
                _isValidatingPhoto.value = false
                _photoError.value = metaResult.exceptionOrNull()?.message
                    ?: "La imagen no cumple con los requisitos mínimos."
                return@launch
            }

            // 2. Decodificar bitmap
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } catch (_: Exception) {
                    null
                }
            }

            if (bitmap == null) {
                _isValidatingPhoto.value = false
                _photoError.value = "No se pudo leer la imagen seleccionada."
                return@launch
            }

            _previewBitmap.value?.recycle()
            _previewBitmap.value = bitmap

            // 3. Validar pose de cuerpo completo (HU-11 CA-03)
            val poseResult = fittingRepository.validatePose(bitmap)
            _isValidatingPhoto.value = false
            poseResult.onFailure { error ->
                _photoError.value = error.message ?: "Necesitamos ver tu cuerpo completo, de pies a cabeza"
            }
        }
    }

    fun retakePhoto() {
        _previewBitmap.value?.recycle()
        _previewBitmap.value = null
        _photoError.value = null
        _isUploadError.value = false
        _uploadProgress.value = 0f
    }

    fun confirmAndUploadPhoto() {
        val bitmap = _previewBitmap.value ?: return
        if ((_photoError.value != null && !_isUploadError.value) || _isValidatingPhoto.value) return

        viewModelScope.launch {
            _isUploadingPhoto.value = true
            _uploadProgress.value = 0f
            _photoError.value = null
            _isUploadError.value = false

            val result = fittingRepository.uploadFittingPhoto(bitmap) { progress ->
                _uploadProgress.value = progress
            }

            _isUploadingPhoto.value = false
            result.onSuccess {
                _previewBitmap.value?.recycle()
                _previewBitmap.value = null

                // HU-14 CA-01: Si no tiene altura registrada, mostrar modal de altura
                val user = currentUser.value
                if (user?.alturaCm == null) {
                    _showHeightDialog.value = true
                }
            }.onFailure { error ->
                _isUploadError.value = true
                val rawMsg = error.localizedMessage.orEmpty()
                _photoError.value = if (rawMsg.contains("permission", ignoreCase = true)) {
                    "No se pudo acceder al almacenamiento. Asegúrate de tener tu sesión activa."
                } else if (rawMsg.contains("network", ignoreCase = true) || rawMsg.contains("connection", ignoreCase = true)) {
                    "Error de conexión. Verifica tu conexión a internet e inténtalo de nuevo."
                } else {
                    rawMsg.ifBlank { "No se pudo subir la fotografía. Inténtalo de nuevo." }
                }
            }
        }
    }

    fun saveHeight(heightCm: Int) {
        viewModelScope.launch {
            _isSavingHeight.value = true
            _heightError.value = null

            val result = fittingRepository.updateHeight(heightCm)
            _isSavingHeight.value = false

            result.onSuccess {
                _showHeightDialog.value = false
            }.onFailure { error ->
                _heightError.value = error.localizedMessage ?: "No se pudo guardar la altura."
            }
        }
    }

    fun dismissHeightDialog() {
        _showHeightDialog.value = false
    }

    fun openHeightDialog() {
        _showHeightDialog.value = true
    }

    // HU-17: Eliminación de la fotografía
    val eliminacion = EliminacionDeFoto(
        repositorio = fittingRepository,
        hayConexion = { monitorDeConexion.conectado.first() },
        scope = viewModelScope,
    )

    override fun onCleared() {
        super.onCleared()
        _previewBitmap.value?.recycle()
        _previewBitmap.value = null
    }
}
