package pe.modaviva.app.domain.usecase

import javax.inject.Inject

class ValidateEmailUseCase @Inject constructor() {

    private companion object {
        val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }

    operator fun invoke(email: String): String? {
        val value = email.trim()
        if (value.isEmpty()) return "Ingresa tu correo"
        if (!EMAIL_REGEX.matches(value)) return "Ingresa un correo válido"
        return null
    }

    fun isValid(email: String): Boolean = invoke(email) == null


}
