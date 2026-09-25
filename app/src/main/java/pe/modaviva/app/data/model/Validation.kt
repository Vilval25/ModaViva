package pe.modaviva.app.data.model

object Validation {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private const val DOCUMENTO_LENGTH = 8
    private const val PHONE_LENGTH = 9
    private const val PASSWORD_MIN_LENGTH = 8

    fun isValidEmail(value: String): Boolean =
        EMAIL_REGEX.matches(value.trim())

    fun isValidDocumento(value: String): Boolean =
        value.trim().length == DOCUMENTO_LENGTH && value.trim().all { it.isDigit() }

    fun isValidPhone(value: String): Boolean {
        val digits = value.trim()
        return digits.length == PHONE_LENGTH && digits.all { it.isDigit() }
    }

    fun passwordError(password: String): String? {
        if (password.isBlank()) return "Ingresa una contraseña"
        if (password.length < PASSWORD_MIN_LENGTH) {
            return "La contraseña debe tener mínimo $PASSWORD_MIN_LENGTH caracteres"
        }
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        if (!hasUpper || !hasLower || !hasDigit) {
            return "Debe incluir mayúscula, minúscula y un número"
        }
        return null
    }

    fun confirmPasswordError(confirm: String, password: String): String? {
        if (confirm.isBlank()) return "Confirma tu contraseña"
        if (confirm != password) return "Las contraseñas no coinciden"
        return null
    }
}
