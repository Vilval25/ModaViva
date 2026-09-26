package pe.modaviva.app.domain.usecase

import javax.inject.Inject

/**
 * Criterio de aceptación HU-01: contraseña de 8+ caracteres con
 * mayúscula, minúscula y un número.
 */
class ValidatePasswordUseCase @Inject constructor() {

    private companion object {
        const val MIN_LENGTH = 8
    }

    operator fun invoke(password: String): String? {
        if (password.isEmpty()) return "Ingresa una contraseña"
        if (password.length < MIN_LENGTH) {
            return "La contraseña debe tener mínimo $MIN_LENGTH caracteres"
        }
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        if (!hasUpper || !hasLower || !hasDigit) {
            return "Debe incluir mayúscula, minúscula y un número"
        }
        return null
    }


}
