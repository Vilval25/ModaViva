package pe.modaviva.app.domain.repository

import android.graphics.Bitmap
import android.net.Uri
import pe.modaviva.app.domain.model.ConstanciaEliminacionFoto

interface FittingRepository {
    suspend fun validateImageMetadata(uri: Uri): Result<Unit>
    suspend fun validatePose(bitmap: Bitmap): Result<Boolean>
    suspend fun uploadFittingPhoto(
        bitmap: Bitmap,
        onProgress: (Float) -> Unit = {}
    ): Result<String>
    suspend fun updateHeight(heightCm: Int): Result<Unit>
    /** Elimina la foto del probador y lo que se derivó de ella (HU-17). */
    suspend fun deleteFittingPhoto(): Result<ConstanciaEliminacionFoto>
}

