package pe.modaviva.app.domain.usecase

import pe.modaviva.app.domain.model.AuthFormField
import pe.modaviva.app.domain.model.RegisterRequest
import pe.modaviva.app.domain.validation.EmailRule
import javax.inject.Inject

/**
 * Criterios de aceptación HU-01: valida los campos del formulario de
 * registro y la aceptación de Términos y Condiciones.
 */
class ValidateRegisterFormUseCase @Inject constructor() {

    private companion object {
        const val PASSWORD_MIN_LENGTH = 8
        const val NOMBRE_ERROR = "Solo letras, espacios y guiones"
        val NOMBRE_REGEX = Regex(
            "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?:[ '’\\-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$"
        )
    }

    operator fun invoke(
        form: RegisterRequest,
        confirmPassword: String,
    ): Map<AuthFormField, String> = buildMap {
        nombreError(form.nombres, "Ingresa tus nombres")
            ?.let { put(AuthFormField.NOMBRES, it) }
        nombreError(form.apellidos, "Ingresa tus apellidos")
            ?.let { put(AuthFormField.APELLIDOS, it) }
        EmailRule.validate(form.email)?.let { put(AuthFormField.EMAIL, it) }
        passwordError(form.password)?.let { put(AuthFormField.PASSWORD, it) }
        confirmPasswordError(confirmPassword, form.password)?.let {
            put(AuthFormField.CONFIRM_PASSWORD, it)
        }
        if (!form.aceptaTerminos) {
            put(AuthFormField.TERMS, "Debes aceptar los Términos y Condiciones")
        }
    }

    fun isValid(form: RegisterRequest, confirmPassword: String): Boolean =
        invoke(form, confirmPassword).isEmpty()

    private fun nombreError(nombre: String, emptyMessage: String): String? {
        val value = nombre.trim()
        if (value.isEmpty()) return emptyMessage
        if (!NOMBRE_REGEX.matches(value)) return NOMBRE_ERROR
        return null
    }

    private fun passwordError(password: String): String? {
        if (password.isEmpty()) return "Ingresa una contraseña"
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

    private fun confirmPasswordError(confirmPassword: String, password: String): String? {
        if (confirmPassword.isEmpty()) return "Confirma tu contraseña"
        if (confirmPassword != password) return "Las contraseñas no coinciden"
        return null
    }
}
