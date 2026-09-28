package pe.modaviva.app.domain.usecase

import pe.modaviva.app.domain.model.AuthFormField
import pe.modaviva.app.domain.validation.EmailRule
import javax.inject.Inject

/**
 * Criterio de aceptación HU-02: acceso con las credenciales de la cuenta web.
 *
 * El correo sí se valida con el mismo formato del registro, pero la contraseña
 * solo se comprueba que no esté vacía: la regla de complejidad es una
 * restricción del registro y aplicarla aquí dejaría fuera a clientes con
 * contraseñas más antiguas.
 */
class ValidateLoginFormUseCase @Inject constructor() {

    operator fun invoke(email: String, password: String): Map<AuthFormField, String> = buildMap {
        EmailRule.validate(email)?.let { put(AuthFormField.EMAIL, it) }
        if (password.isEmpty()) put(AuthFormField.PASSWORD, "Ingresa tu contraseña")
    }

    fun isValid(email: String, password: String): Boolean =
        invoke(email, password).isEmpty()
}
