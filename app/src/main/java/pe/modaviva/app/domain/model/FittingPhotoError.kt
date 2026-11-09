package pe.modaviva.app.domain.model

sealed interface FittingPhotoError {
    data object InvalidFormat : FittingPhotoError
    data class FileTooLarge(val maxMb: Int = 10) : FittingPhotoError
    data class ResolutionTooLow(val minWidth: Int = 720, val minHeight: Int = 1280) : FittingPhotoError
    data object NotFullBody : FittingPhotoError
    data class General(val message: String) : FittingPhotoError
}

class FittingPhotoException(val error: FittingPhotoError) : Exception(
    when (error) {
        is FittingPhotoError.InvalidFormat -> "El formato debe ser JPG o PNG"
        is FittingPhotoError.FileTooLarge -> "La imagen no debe pesar más de ${error.maxMb} MB"
        is FittingPhotoError.ResolutionTooLow -> "La imagen debe medir al menos ${error.minWidth} × ${error.minHeight} píxeles"
        is FittingPhotoError.NotFullBody -> "Necesitamos ver tu cuerpo completo, de pies a cabeza"
        is FittingPhotoError.General -> error.message
    }
)

