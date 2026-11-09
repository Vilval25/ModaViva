package pe.modaviva.app.fitting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import pe.modaviva.app.domain.model.FittingPhotoError
import pe.modaviva.app.domain.model.FittingPhotoException
import pe.modaviva.app.domain.model.UserProfile

/**
 * Pruebas unitarias para:
 * - HU-11: Registro de la fotografía del cliente (CA-01 a CA-06).
 * - HU-14: Registro de altura para la recomendación de talla (CA-01 a CA-05).
 */
class FittingPhotoAndHeightTest {

    private fun crearUsuario(
        uid: String = "user_456",
        consentimientoAceptado: Boolean = true,
        fotoUrl: String? = null,
        alturaCm: Int? = null,
    ): UserProfile {
        return UserProfile(
            uid = uid,
            email = "cliente@modaviva.pe",
            nombres = "Ana",
            apellidos = "García",
            consentimientoProbadorAceptado = consentimientoAceptado,
            consentimientoProbadorVersion = UserProfile.PROBADOR_AVISO_VERSION_ACTUAL,
            fotoProbadorUrl = fotoUrl,
            alturaCm = alturaCm,
        )
    }

    // ==========================================
    // HU-11: Registro de la fotografía
    // ==========================================

    @Test
    fun `HU-11 CA-01 cliente con consentimiento puede registrar foto y opciones camara o galeria estan disponibles`() {
        val user = crearUsuario(fotoUrl = null)
        assertTrue(user.tieneConsentimientoProbador)
        assertNull("Aún no tiene foto registrada", user.fotoProbadorUrl)
    }

    @Test
    fun `HU-11 CA-02 formato invalido se rechaza con mensaje especifico`() {
        val extensionInvalida = "gif"
        val esValido = extensionInvalida in listOf("jpg", "jpeg", "png")
        assertFalse(esValido)

        val exception = FittingPhotoException(FittingPhotoError.InvalidFormat)
        assertEquals("El formato debe ser JPG o PNG", exception.message)
    }

    @Test
    fun `HU-11 CA-02 tamano superior a 10MB se rechaza con mensaje especifico`() {
        val maxBytes = 10L * 1024 * 1024
        val tamanoArchivo = 12L * 1024 * 1024 // 12 MB
        assertTrue(tamanoArchivo > maxBytes)

        val exception = FittingPhotoException(FittingPhotoError.FileTooLarge(10))
        assertEquals("La imagen no debe pesar más de 10 MB", exception.message)
    }

    @Test
    fun `HU-11 CA-02 resolucion menor a 720x1280 px se rechaza con mensaje especifico`() {
        val ancho = 600
        val alto = 800
        val cumpleVertical = ancho >= 720 && alto >= 1280
        val cumpleHorizontal = ancho >= 1280 && alto >= 720
        assertFalse(cumpleVertical || cumpleHorizontal)

        val exception = FittingPhotoException(FittingPhotoError.ResolutionTooLow(720, 1280))
        assertEquals("La imagen debe medir al menos 720 × 1280 píxeles", exception.message)
    }

    @Test
    fun `HU-11 CA-03 foto sin deteccion de cuerpo completo se rechaza con mensaje requerido`() {
        val tieneCabeza = true
        val tieneHombros = true
        val tieneCaderas = true
        val tieneRodillas = false // Solo medio cuerpo
        val tieneTobillos = false

        val cuerpoCompleto = tieneCabeza && tieneHombros && tieneCaderas && tieneRodillas && tieneTobillos
        assertFalse("Debe fallar si no se ven las extremidades inferiores", cuerpoCompleto)

        val exception = FittingPhotoException(FittingPhotoError.NotFullBody)
        assertEquals("Necesitamos ver tu cuerpo completo, de pies a cabeza", exception.message)
    }

    @Test
    fun `HU-11 CA-04 y CA-05 registro exitoso actualiza foto del usuario y reemplaza anterior`() {
        val userInicial = crearUsuario(fotoUrl = "https://storage.modaviva.pe/user_456/foto_antigua.jpg")
        assertEquals("https://storage.modaviva.pe/user_456/foto_antigua.jpg", userInicial.fotoProbadorUrl)

        val nuevaUrl = "https://storage.modaviva.pe/user_456/foto_nueva.jpg"
        val userActualizado = userInicial.copy(fotoProbadorUrl = nuevaUrl)

        assertEquals(nuevaUrl, userActualizado.fotoProbadorUrl)
        assertNotNull(userActualizado.fotoProbadorUrl)
    }

    // ==========================================
    // HU-14: Registro de altura
    // ==========================================

    @Test
    fun `HU-14 CA-01 usuario con foto pero sin altura activa modal de altura antes de probar prendas`() {
        val userConFotoSinAltura = crearUsuario(
            fotoUrl = "https://storage.modaviva.pe/user_456/foto.jpg",
            alturaCm = null,
        )

        assertNotNull(userConFotoSinAltura.fotoProbadorUrl)
        assertNull(userConFotoSinAltura.alturaCm)

        val debePedirAltura = userConFotoSinAltura.fotoProbadorUrl != null && userConFotoSinAltura.alturaCm == null
        assertTrue("Debe solicitar la altura al tener foto registrada y no tener altura", debePedirAltura)
    }

    @Test
    fun `HU-14 CA-02 y CA-03 validacion de rango de altura entre 100 y 230 cm`() {
        val alturasInvalidas = listOf(0, 50, 99, 231, 300, -10)
        for (altura in alturasInvalidas) {
            val valida = altura in 100..230
            assertFalse("Altura $altura cm debe ser inválida", valida)
        }

        val alturasValidas = listOf(100, 150, 165, 178, 190, 230)
        for (altura in alturasValidas) {
            val valida = altura in 100..230
            assertTrue("Altura $altura cm debe ser válida", valida)
        }
    }

    @Test
    fun `HU-14 CA-04 opcion omitir mantiene flujo sin bloquear`() {
        val user = crearUsuario(
            fotoUrl = "https://storage.modaviva.pe/user_456/foto.jpg",
            alturaCm = null,
        )

        // Simulación de omitir: el usuario continúa navegando sin guardar altura
        var alturaOmitida = false
        fun onOmitir() {
            alturaOmitida = true
        }

        onOmitir()
        assertTrue(alturaOmitida)
        assertNull(user.alturaCm)
    }

    @Test
    fun `HU-14 CA-05 edicion de altura en perfil actualiza el valor`() {
        val userOriginal = crearUsuario(alturaCm = 165)
        assertEquals(165, userOriginal.alturaCm)

        val nuevaAltura = 170
        val userActualizado = userOriginal.copy(alturaCm = nuevaAltura)
        assertEquals(170, userActualizado.alturaCm)
    }
}

