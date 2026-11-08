package pe.modaviva.app.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.modaviva.app.domain.model.DestinoEntrega
import pe.modaviva.app.domain.model.Stock
import pe.modaviva.app.domain.model.Tienda
import pe.modaviva.app.domain.model.TipoDestino
import pe.modaviva.app.domain.usecase.CalculateNearestStoreUseCase

class CalculateNearestStoreUseCaseTest {

    private val useCase = CalculateNearestStoreUseCase()

    private val tiendasMuestra = listOf(
        Tienda(id = "T01", nombre = "Tienda Miraflores", lat = -12.1219, lng = -77.0305, distrito = "Miraflores"),
        Tienda(id = "T02", nombre = "Tienda San Isidro", lat = -12.0975, lng = -77.0365, distrito = "San Isidro"),
        Tienda(id = "T03", nombre = "Tienda Surco", lat = -12.1350, lng = -76.9920, distrito = "Santiago de Surco"),
        Tienda(id = "T04", nombre = "Tienda Sin Coordenadas", lat = null, lng = null, distrito = "Lima"),
    )

    @Test
    fun `1 una tienda con stock y coordenadas validas`() {
        val destino = DestinoEntrega(
            tipo = TipoDestino.UBICACION_ACTUAL_GPS,
            lat = -12.1200,
            lng = -77.0300,
            distrito = "Miraflores"
        )
        val stock = Stock(
            prendaId = "P01",
            talla = "M",
            porTienda = mapOf("T01" to 5, "T02" to 0),
            total = 5
        )

        val resultado = useCase(destino, stock, tiendasMuestra)

        assertEquals(1, resultado.tiendasConStock.size)
        assertNotNull(resultado.tiendaRecomendada)
        assertEquals("T01", resultado.tiendaRecomendada!!.tienda.id)
    }

    @Test
    fun `2 varias tiendas con stock seleccionando la mas cercana`() {
        val destino = DestinoEntrega(
            tipo = TipoDestino.UBICACION_ACTUAL_GPS,
            lat = -12.1000,
            lng = -77.0350,
            distrito = "San Isidro"
        )
        // T02 (San Isidro) está más cerca de (-12.1000, -77.0350) que T01 (Miraflores)
        val stock = Stock(
            prendaId = "P01",
            talla = "M",
            porTienda = mapOf("T01" to 3, "T02" to 2),
            total = 5
        )

        val resultado = useCase(destino, stock, tiendasMuestra)

        assertEquals(2, resultado.tiendasConStock.size)
        assertNotNull(resultado.tiendaRecomendada)
        assertEquals("T02", resultado.tiendaRecomendada!!.tienda.id)
    }

    @Test
    fun `3 tienda sin stock excluida`() {
        val destino = DestinoEntrega(
            tipo = TipoDestino.UBICACION_ACTUAL_GPS,
            lat = -12.1000,
            lng = -77.0350,
            distrito = "San Isidro"
        )
        val stock = Stock(
            prendaId = "P01",
            talla = "M",
            porTienda = mapOf("T01" to 3, "T02" to 0, "T03" to 5),
            total = 8
        )

        val resultado = useCase(destino, stock, tiendasMuestra)

        // T02 tiene stock 0, por lo que no debe estar en tiendasConStock
        assertEquals(2, resultado.tiendasConStock.size)
        assertTrue(resultado.tiendasConStock.none { it.tienda.id == "T02" })
    }

    @Test
    fun `4 destino sin coordenadas indica que requiere coordenadas`() {
        val destino = DestinoEntrega(
            tipo = TipoDestino.OTRA_DIRECCION,
            lat = null,
            lng = null,
            distrito = "Miraflores"
        )
        val stock = Stock(
            prendaId = "P01",
            talla = "M",
            porTienda = mapOf("T01" to 3, "T02" to 2),
            total = 5
        )

        val resultado = useCase(destino, stock, tiendasMuestra)

        assertEquals(2, resultado.tiendasConStock.size)
        assertNull(resultado.tiendaRecomendada)
        assertTrue(resultado.requiereCoordenadasParaDistancia)
    }

    @Test
    fun `5 tiendas sin coordenadas excluidas del calculo de distancia`() {
        val destino = DestinoEntrega(
            tipo = TipoDestino.UBICACION_ACTUAL_GPS,
            lat = -12.1000,
            lng = -77.0350,
            distrito = "Lima"
        )
        val stock = Stock(
            prendaId = "P01",
            talla = "M",
            porTienda = mapOf("T04" to 4),
            total = 4
        )

        val resultado = useCase(destino, stock, tiendasMuestra)

        assertEquals(1, resultado.tiendasConStock.size)
        assertNull(resultado.tiendaRecomendada)
        assertNull(resultado.tiendasConStock.first().distanciaKm)
    }

    @Test
    fun `6 ninguna tienda con stock retorna resultado vacio`() {
        val destino = DestinoEntrega(
            tipo = TipoDestino.UBICACION_ACTUAL_GPS,
            lat = -12.1000,
            lng = -77.0350,
            distrito = "San Isidro"
        )
        val stock = Stock(
            prendaId = "P01",
            talla = "M",
            porTienda = mapOf("T01" to 0, "T02" to 0),
            total = 0
        )

        val resultado = useCase(destino, stock, tiendasMuestra)

        assertTrue(resultado.tiendasConStock.isEmpty())
        assertNull(resultado.tiendaRecomendada)
    }

    @Test
    fun `7 cambio de talla actualiza la tienda recomendada`() {
        val destino = DestinoEntrega(
            tipo = TipoDestino.UBICACION_ACTUAL_GPS,
            lat = -12.1000,
            lng = -77.0350,
            distrito = "San Isidro"
        )
        // Stock talla S: solo T01
        val stockS = Stock(prendaId = "P01", talla = "S", porTienda = mapOf("T01" to 2), total = 2)
        // Stock talla M: solo T02
        val stockM = Stock(prendaId = "P01", talla = "M", porTienda = mapOf("T02" to 3), total = 3)

        val resS = useCase(destino, stockS, tiendasMuestra)
        assertEquals("T01", resS.tiendaRecomendada?.tienda?.id)

        val resM = useCase(destino, stockM, tiendasMuestra)
        assertEquals("T02", resM.tiendaRecomendada?.tienda?.id)
    }
}
