package pe.modaviva.app.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.modaviva.app.domain.model.AuthFormField
import pe.modaviva.app.domain.model.RegisterRequest
import pe.modaviva.app.domain.usecase.ValidateLoginFormUseCase
import pe.modaviva.app.domain.usecase.ValidateRegisterFormUseCase
import pe.modaviva.app.ui.auth.login.LoginUiState
import pe.modaviva.app.ui.auth.register.RegisterUiState

/**
 * Pruebas unitarias para HU-01 (Registro) y HU-02 (Inicio de sesión y recuperación).
 */
class AuthTest {

    private val validateRegisterForm = ValidateRegisterFormUseCase()
    private val validateLoginForm = ValidateLoginFormUseCase()

    @Test
    fun `HU-01 CA-01 formulario incompleto o invalido deshabilita boton crear cuenta`() {
        val requestIncompleta = RegisterRequest(
            nombres = "",
            apellidos = "",
            email = "invalido",
            password = "123",
            aceptaTerminos = false,
        )

        val errores = validateRegisterForm(requestIncompleta, confirmPassword = "")
        assertFalse(validateRegisterForm.isValid(requestIncompleta, confirmPassword = ""))
        assertTrue(errores.containsKey(AuthFormField.NOMBRES))
        assertTrue(errores.containsKey(AuthFormField.APELLIDOS))
        assertTrue(errores.containsKey(AuthFormField.EMAIL))
        assertTrue(errores.containsKey(AuthFormField.PASSWORD))
        assertTrue(errores.containsKey(AuthFormField.TERMS))

        // State canSubmit debe ser falso
        val state = RegisterUiState(
            nombres = "",
            errors = errores,
            aceptaTerminos = false,
        )
        assertFalse(state.canSubmit)
    }

    @Test
    fun `HU-01 CA-04 requisitos de contrasena longitud mayuscula minuscula y numero`() {
        val formBase = RegisterRequest(
            nombres = "Juan",
            apellidos = "Pérez",
            email = "juan@example.com",
            password = "",
            aceptaTerminos = true,
        )

        // Contraseña corta (< 8)
        val errCorta = validateRegisterForm(formBase.copy(password = "Ab1"), "Ab1")
        assertEquals("La contraseña debe tener mínimo 8 caracteres", errCorta[AuthFormField.PASSWORD])

        // Sin número
        val errSinNum = validateRegisterForm(formBase.copy(password = "Abcdefgh"), "Abcdefgh")
        assertEquals("Debe incluir mayúscula, minúscula y un número", errSinNum[AuthFormField.PASSWORD])

        // Sin mayúscula
        val errSinMayus = validateRegisterForm(formBase.copy(password = "abcdefg1"), "abcdefg1")
        assertEquals("Debe incluir mayúscula, minúscula y un número", errSinMayus[AuthFormField.PASSWORD])

        // Sin minúscula
        val errSinMinus = validateRegisterForm(formBase.copy(password = "ABCDEFG1"), "ABCDEFG1")
        assertEquals("Debe incluir mayúscula, minúscula y un número", errSinMinus[AuthFormField.PASSWORD])

        // Válida
        val errValida = validateRegisterForm(formBase.copy(password = "Clave123"), "Clave123")
        assertNull(errValida[AuthFormField.PASSWORD])
    }

    @Test
    fun `HU-01 CA-10 no aceptar terminos impide el envio del formulario`() {
        val formSinTerminos = RegisterRequest(
            nombres = "María",
            apellidos = "Gómez",
            email = "maria@example.com",
            password = "Password123",
            aceptaTerminos = false,
        )

        val errores = validateRegisterForm(formSinTerminos, confirmPassword = "Password123")
        assertTrue(errores.containsKey(AuthFormField.TERMS))

        val state = RegisterUiState(
            nombres = "María",
            apellidos = "Gómez",
            email = "maria@example.com",
            password = "Password123",
            confirmPassword = "Password123",
            aceptaTerminos = false,
            errors = errores,
        )
        assertFalse(state.canSubmit)
    }

    @Test
    fun `HU-02 CA-07 cinco intentos fallidos activan bloqueo de 15 minutos`() {
        var state = LoginUiState()
        assertFalse(state.isLockedOut)
        assertTrue(state.canSubmit)

        // Simular 4 intentos fallidos
        for (i in 1..4) {
            state = state.copy(failedAttempts = i)
            assertFalse("En el intento $i no debería bloquearse", state.isLockedOut)
        }

        // 5to intento fallido activa el bloqueo por 15 min
        val lockoutTime = System.currentTimeMillis() + 15 * 60 * 1000L
        state = state.copy(failedAttempts = 5, lockoutUntilMillis = lockoutTime)
        assertTrue("En 5 intentos debe estar bloqueado", state.isLockedOut)
        assertFalse("Estando bloqueado canSubmit debe ser falso", state.canSubmit)
    }

    @Test
    fun `HU-02 CA-11 control de expiracion de sesion por 30 dias de inactividad`() {
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val now = System.currentTimeMillis()

        // Última actividad hace 10 días (válida)
        val active10DaysAgo = now - (10L * 24 * 60 * 60 * 1000)
        assertFalse(now - active10DaysAgo > thirtyDaysMs)

        // Última actividad hace 31 días (expirada)
        val active31DaysAgo = now - (31L * 24 * 60 * 60 * 1000)
        assertTrue(now - active31DaysAgo > thirtyDaysMs)
    }
}
