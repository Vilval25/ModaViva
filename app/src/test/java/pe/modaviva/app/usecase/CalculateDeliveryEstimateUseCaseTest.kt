package pe.modaviva.app.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.modaviva.app.domain.model.DestinoEntrega
import pe.modaviva.app.domain.model.TarifaEnvio
import pe.modaviva.app.domain.model.TipoDestino
import pe.modaviva.app.domain.usecase.CalculateDeliveryEstimateUseCase

class CalculateDeliveryEstimateUseCaseTest {

    private val useCase = CalculateDeliveryEstimateUseCase()

    private val tarifasMuestra = listOf(
        TarifaEnvio(
            id = "miraflores",
            distrito = "Miraflores",
            cobertura = true,
            costo = 8.0,
            plazoDias = 2,
        ),
        TarifaEnvio(
            id = "jesus-maria",
            distrito = "Jesús María",
            cobertura = true,
            costo = 9.0,
            plazoDias = 2,
        ),
        TarifaEnvio(
            id = "surco",
            distrito = "Santiago de Surco",
            cobertura = true,
            costo = 10.0,
            plazoDias = 3,
        ),
        TarifaEnvio(
            id = "distrito-sin-cobertura",
            distrito = "Distrito Lejano",
            cobertura = false,
            costo = 0.0,
            plazoDias = 0,
        ),
    )

    @Test
    fun `distrito con tarifa y cobertura calcula plazo con margen de mas 2 a mas 3 dias`() {
        val destino = DestinoEntrega(
            tipo = TipoDestino.OTRA_DIRECCION,
            direccion = "Av. Larco 123",
            distrito = "Miraflores",
            distritoSlug = "miraflores",
        )

        val resultado = useCase(destino, tarifasMuestra)

        assertTrue(resultado.tieneCobertura)
        assertTrue(resultado.tarifaRegistrada)
        assertEquals(8.0, resultado.costoEnvio, 0.01)
        // plazoBase = 2 -> min = 2+2 = 4, max = 2+3 = 5
        assertEquals(4, resultado.plazoMinDias)
        assertEquals(5, resultado.plazoMaxDias)
        assertEquals("4 a 5 días hábiles", resultado.rangoDiasTexto)
        assertNull(resultado.mensajeInformativo)
    }

    @Test
    fun `distrito con plazo base de 3 dias produce rango de 5 a 6 dias habiles`() {
        val destino = DestinoEntrega(
            tipo = TipoDestino.DIRECCION_REGISTRADA,
            direccion = "Av. Benavides 456",
            distrito = "Santiago de Surco",
            distritoSlug = "surco",
        )

        val resultado = useCase(destino, tarifasMuestra)

        assertTrue(resultado.tieneCobertura)
        assertEquals(10.0, resultado.costoEnvio, 0.01)
        // plazoBase = 3 -> min = 3+2 = 5, max = 3+3 = 6
        assertEquals(5, resultado.plazoMinDias)
        assertEquals(6, resultado.plazoMaxDias)
        assertEquals("5 a 6 días hábiles", resultado.rangoDiasTexto)
    }

    @Test
    fun `distrito con tarifa pero sin cobertura informa la falta de cobertura`() {
        val destino = DestinoEntrega(
            tipo = TipoDestino.OTRA_DIRECCION,
            distrito = "Distrito Lejano",
            distritoSlug = "distrito-sin-cobertura",
        )

        val resultado = useCase(destino, tarifasMuestra)

        assertFalse(resultado.tieneCobertura)
        assertTrue(resultado.tarifaRegistrada)
        assertEquals(0.0, resultado.costoEnvio, 0.01)
        assertNotNull(resultado.mensajeInformativo)
        assertTrue(resultado.mensajeInformativo!!.contains("Sin cobertura de entrega", ignoreCase = true))
        assertEquals("Sin cobertura de entrega a domicilio", resultado.rangoDiasTexto)
    }

    @Test
    fun `distrito sin tarifa registrada informa la falta de tarifa sin inventar costos`() {
        val destino = DestinoEntrega(
            tipo = TipoDestino.OTRA_DIRECCION,
            distrito = "Iquitos",
            distritoSlug = "iquitos",
        )

        val resultado = useCase(destino, tarifasMuestra)

        assertFalse(resultado.tieneCobertura)
        assertFalse(resultado.tarifaRegistrada)
        assertEquals(0.0, resultado.costoEnvio, 0.01)
        assertNotNull(resultado.mensajeInformativo)
        assertTrue(resultado.mensajeInformativo!!.contains("Sin tarifa registrada", ignoreCase = true))
        assertEquals("Sin tarifa registrada para este distrito", resultado.rangoDiasTexto)
    }

    @Test
    fun `distrito vacio solicita al usuario seleccionar o confirmar distrito`() {
        val destino = DestinoEntrega(
            tipo = TipoDestino.UBICACION_ACTUAL_GPS,
            distrito = "",
            distritoSlug = "",
        )

        val resultado = useCase(destino, tarifasMuestra)

        assertFalse(resultado.tieneCobertura)
        assertFalse(resultado.tarifaRegistrada)
        assertNotNull(resultado.mensajeInformativo)
        assertTrue(resultado.mensajeInformativo!!.contains("Selecciona o confirma", ignoreCase = true))
    }

    @Test
    fun `normalizacion ignora mayusculas minusculas tildes guiones y espacios`() {
        val destino = DestinoEntrega(
            tipo = TipoDestino.OTRA_DIRECCION,
            distrito = "   JESÚS_MARÍA  ",
            distritoSlug = "",
        )

        val resultado = useCase(destino, tarifasMuestra)

        assertTrue(resultado.tieneCobertura)
        assertTrue(resultado.tarifaRegistrada)
        assertEquals(9.0, resultado.costoEnvio, 0.01)
        assertEquals(4, resultado.plazoMinDias)
        assertEquals(5, resultado.plazoMaxDias)
    }
}
