package pe.modaviva.app.domain.usecase

import pe.modaviva.app.domain.model.AuthFormField
import pe.modaviva.app.domain.model.RegisterRequest
import pe.modaviva.app.domain.validation.EmailRule
import javax.inject.Inject

/**
 * Criterios de aceptación HU-01: valida los siete campos del formulario de
 * registro y devuelve el error de cada campo que no cumple.
 *
 * Las reglas de formato viven aquí en lugar de en un use case por regla
 * porque son de un solo uso: solo este formulario las necesita.
 */
class ValidateRegisterFormUseCase @Inject constructor() {

    private companion object {
        const val DOCUMENTO_LENGTH = 8
        const val PHONE_LENGTH = 9
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
        documentoError(form.documento)?.let { put(AuthFormField.DOCUMENTO, it) }
        telefonoError(form.telefono)?.let { put(AuthFormField.TELEFONO, it) }
        EmailRule.validate(form.email)?.let { put(AuthFormField.EMAIL, it) }
        passwordError(form.password)?.let { put(AuthFormField.PASSWORD, it) }
        confirmPasswordError(confirmPassword, form.password)?.let {
            put(AuthFormField.CONFIRM_PASSWORD, it)
        }
    }

    fun isValid(form: RegisterRequest, confirmPassword: String): Boolean =
        invoke(form, confirmPassword).isEmpty()

    fun isDocumentoValid(documento: String): Boolean =
        documentoError(documento) == null

    fun isEmailValid(email: String): Boolean = EmailRule.isValid(email)

    private fun nombreError(nombre: String, emptyMessage: String): String? {
        val value = nombre.trim()
        if (value.isEmpty()) return emptyMessage
        if (!NOMBRE_REGEX.matches(value)) return NOMBRE_ERROR
        return null
    }

    private fun documentoError(documento: String): String? {
        val value = documento.trim()
        if (value.isEmpty()) return "Ingresa tu documento"
        if (value.length != DOCUMENTO_LENGTH || !value.all { it.isDigit() }) {
            return "El documento debe tener $DOCUMENTO_LENGTH dígitos"
        }
        return null
    }

    private fun telefonoError(telefono: String): String? {
        val value = telefono.trim()
        if (value.isEmpty()) return "Ingresa tu celular"
        if (value.length != PHONE_LENGTH || !value.all { it.isDigit() }) {
            return "Ingresa un celular válido de $PHONE_LENGTH dígitos"
        }
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
