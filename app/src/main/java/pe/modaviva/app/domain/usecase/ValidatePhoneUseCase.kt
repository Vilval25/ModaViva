package pe.modaviva.app.domain.usecase

import javax.inject.Inject

class ValidatePhoneUseCase @Inject constructor() {

    private companion object {
        const val PHONE_LENGTH = 9
    }

    operator fun invoke(telefono: String): String? {
        val value = telefono.trim()
        if (value.isEmpty()) return "Ingresa tu celular"
        if (value.length != PHONE_LENGTH || !value.all { it.isDigit() }) {
            return "Ingresa un celular válido de $PHONE_LENGTH dígitos"
        }
        return null
    }

}
