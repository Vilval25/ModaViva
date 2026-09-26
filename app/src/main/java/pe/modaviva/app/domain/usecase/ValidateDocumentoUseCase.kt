package pe.modaviva.app.domain.usecase

import javax.inject.Inject

class ValidateDocumentoUseCase @Inject constructor() {

    operator fun invoke(documento: String): String? {
        val value = documento.trim()
        if (value.isEmpty()) return "Ingresa tu documento"
        if (value.length != DOCUMENTO_LENGTH || !value.all { it.isDigit() }) {
            return "El documento debe tener $DOCUMENTO_LENGTH dígitos"
        }
        return null
    }

    fun isValid(documento: String): Boolean = invoke(documento) == null

    private companion object {
        const val DOCUMENTO_LENGTH = 8
    }
}
