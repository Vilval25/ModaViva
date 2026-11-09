package pe.modaviva.app.domain.repository

import android.graphics.Bitmap
import android.net.Uri

interface FittingRepository {
    suspend fun validateImageMetadata(uri: Uri): Result<Unit>
    suspend fun validatePose(bitmap: Bitmap): Result<Boolean>
    suspend fun uploadFittingPhoto(
        bitmap: Bitmap,
        onProgress: (Float) -> Unit = {}
    ): Result<String>
    suspend fun updateHeight(heightCm: Int): Result<Unit>
    suspend fun deleteFittingPhoto(): Result<Unit>
}

