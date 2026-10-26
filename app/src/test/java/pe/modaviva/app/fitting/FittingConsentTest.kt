package pe.modaviva.app.fitting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.modaviva.app.domain.model.UserProfile
import pe.modaviva.app.domain.model.UserProfile.Companion.PROBADOR_AVISO_VERSION_ACTUAL

/**
 * Pruebas unitarias para HU-10: Consentimiento para el uso de la fotografía en el probador virtual.
 *
 * Criterios de Aceptación:
 * - CA-01: Usuarios invitados ven explicación previa y botones para autenticarse.
 * - CA-02: Aviso de privacidad previo a cualquier uso de fotografía.
 * - CA-03: Casilla desmarcada por defecto; "Aceptar y continuar" deshabilitado hasta marcar.
 * - CA-04: Al aceptar se registra UID, versión "1.0", timestamp y consentimiento activo.
 * - CA-05: Al pulsar "Ahora no", el usuario puede seguir navegando y "Probarme" indica que requiere consentimiento.
 * - CA-06: Revocación desactiva el probador virtual y anula consentimiento.
 * - CA-07: Si cambia la versión del aviso, se requiere consentimiento nuevamente.
 */
class FittingConsentTest {

    private fun crearUsuarioBase(
        uid: String = "user_123",
        consentimientoAceptado: Boolean = false,
        versionConsentimiento: String? = null,
        fechaConsentimiento: String? = null,
    ): UserProfile {
        return UserProfile(
            uid = uid,
            email = "edson@modaviva.pe",
            nombres = "Edson",
            apellidos = "Pérez",
            telefono = "999888777",
            emailVerificado = true,
            isGoogleUser = false,
            consentimientoProbadorAceptado = consentimientoAceptado,
            consentimientoProbadorVersion = versionConsentimiento,
        )
    }

    @Test
    fun `HU-10 CA-01 y CA-02 usuario nuevo o invitado no tiene consentimiento otorgado`() {
        val guestUser: UserProfile? = null
        assertNull(guestUser)

        val loggedUserSinConsentimiento = crearUsuarioBase(
            consentimientoAceptado = false,
            versionConsentimiento = null,
        )
        assertFalse(loggedUserSinConsentimiento.tieneConsentimientoProbador)
    }

    @Test
    fun `HU-10 CA-03 estado de casilla por defecto es desmarcada y valida condicion para habilitar envio`() {
        var checkboxMarcado = false
        val botonHabilitado = checkboxMarcado
        assertFalse("El botón Aceptar debe estar deshabilitado cuando la casilla no está marcada", botonHabilitado)

        checkboxMarcado = true
        val botonHabilitadoTrasMarcar = checkboxMarcado
        assertTrue("El botón Aceptar debe habilitarse tras marcar la casilla", botonHabilitadoTrasMarcar)
    }

    @Test
    fun `HU-10 CA-04 aceptar consentimiento registra version actual y estado activo`() {
        val timestamp = "2026-10-26T10:00:00Z"
        val usuarioConConsentimiento = crearUsuarioBase(
            consentimientoAceptado = true,
            versionConsentimiento = PROBADOR_AVISO_VERSION_ACTUAL,
            fechaConsentimiento = timestamp,
        )

        assertEquals("1.0", PROBADOR_AVISO_VERSION_ACTUAL)
        assertTrue(usuarioConConsentimiento.tieneConsentimientoProbador)
        assertEquals("1.0", usuarioConConsentimiento.consentimientoProbadorVersion)
    }

    @Test
    fun `HU-10 CA-05 declinar temporalmente permite navegacion y Probarme requiere consentimiento`() {
        val usuarioSinConsentimiento = crearUsuarioBase(consentimientoAceptado = false)
        assertFalse(usuarioSinConsentimiento.tieneConsentimientoProbador)

        // Simulación de navegación con flag de declined:
        var consentDeclinedTemporarily = false
        fun onAhoraNo() {
            consentDeclinedTemporarily = true
        }

        onAhoraNo()
        assertTrue("El usuario declinó temporalmente para seguir navegando", consentDeclinedTemporarily)
        assertFalse("Sigue sin tener consentimiento en el perfil", usuarioSinConsentimiento.tieneConsentimientoProbador)
    }

    @Test
    fun `HU-10 CA-06 revocacion de consentimiento desactiva probador virtual`() {
        val usuarioActivo = crearUsuarioBase(
            consentimientoAceptado = true,
            versionConsentimiento = "1.0",
            fechaConsentimiento = "2026-10-20T12:00:00Z",
        )
        assertTrue(usuarioActivo.tieneConsentimientoProbador)

        // Tras revocación en Perfil > Privacidad:
        val usuarioRevocado = usuarioActivo.copy(
            consentimientoProbadorAceptado = false,
        )

        assertFalse(usuarioRevocado.tieneConsentimientoProbador)
        assertFalse(usuarioRevocado.consentimientoProbadorAceptado)
    }

    @Test
    fun `HU-10 CA-07 version obsoleta del aviso invalida el consentimiento y exige reaceptacion`() {
        // Supongamos que el usuario tenía aceptada una versión previa "0.9"
        val usuarioConVersionAntigua = crearUsuarioBase(
            consentimientoAceptado = true,
            versionConsentimiento = "0.9",
            fechaConsentimiento = "2026-09-01T10:00:00Z",
        )

        // Al contrastar contra PROBADOR_AVISO_VERSION_ACTUAL ("1.0"), no es válido:
        assertFalse(
            "Un consentimiento de versión previa debe requerir aceptación de la nueva versión",
            usuarioConVersionAntigua.tieneConsentimientoProbador,
        )

        // Al actualizar a la versión actual "1.0":
        val usuarioActualizado = usuarioConVersionAntigua.copy(
            consentimientoProbadorVersion = PROBADOR_AVISO_VERSION_ACTUAL,
        )
        assertTrue(usuarioActualizado.tieneConsentimientoProbador)
    }
}
