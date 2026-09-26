package pe.modaviva.app.domain.usecase

import javax.inject.Inject

class ValidateConfirmPasswordUseCase @Inject constructor() {

    operator fun invoke(confirmPassword: String, password: String): String? {
        if (confirmPassword.isEmpty()) return "Confirma tu contraseña"
        if (confirmPassword != password) return "Las contraseñas no coinciden"
        return null
    }
}
