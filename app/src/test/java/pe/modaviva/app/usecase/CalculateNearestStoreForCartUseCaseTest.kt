package pe.modaviva.app.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.modaviva.app.domain.model.CartItem
import pe.modaviva.app.domain.model.DestinoEntrega
import pe.modaviva.app.domain.model.Stock
import pe.modaviva.app.domain.model.Tienda
import pe.modaviva.app.domain.model.TipoDestino
import pe.modaviva.app.domain.usecase.CalculateNearestStoreForCartUseCase

class CalculateNearestStoreForCartUseCaseTest {

    private val useCase = CalculateNearestStoreForCartUseCase()

    private val tiendas = listOf(
        Tienda(id = "T01", nombre = "Miraflores", lat = -12.1219, lng = -77.0305, distrito = "Miraflores"),
        Tienda(id = "T02", nombre = "San Isidro", lat = -12.0975, lng = -77.0365, distrito = "San Isidro"),
        Tienda(id = "T03", nombre = "Surco", lat = -12.1350, lng = -76.9920, distrito = "Santiago de Surco"),
    )

    private val destinoSanIsidro = DestinoEntrega(
        tipo = TipoDestino.UBICACION_ACTUAL_GPS,
        lat = -12.0980,
        lng = -77.0360,
        distrito = "San Isidro",
    )

    private fun item(varianteId: String, cantidad: Int = 1) = CartItem(
        varianteId = varianteId,
        prendaId = varianteId.substringBefore("_"),
        talla = varianteId.substringAfter("_"),
        colorNombre = "Negro",
        colorHex = "#000000",
        cantidad = cantidad,
        precioAlAgregar = 100.0,
    )

    @Test
    fun `prioriza y recomienda tienda disponible hoy sobre tienda mas cercana sin stock completo`() {
        val cart = listOf(item("BL-1001_M"), item("JN-1001_28"))
        val stocks = mapOf(
            "BL-1001_M" to Stock("BL-1001_M", total = 4, porTienda = mapOf("T01" to 2, "T02" to 2)),
            "JN-1001_28" to Stock("JN-1001_28", total = 3, porTienda = mapOf("T01" to 1, "T03" to 2)),
        )

        val resultado = useCase(cart, stocks, tiendas, destinoSanIsidro)

        assertTrue(resultado.hayStockSuficiente)
        assertEquals(3, resultado.tiendasElegibles.size)

        val primera = resultado.tiendasElegibles.first()
        assertEquals("T01", primera.tienda.id)
        assertTrue(primera.disponibleHoy)
        assertEquals("Disponible hoy", primera.plazoRecojoTexto)
        assertEquals("T01", resultado.tiendaRecomendada?.tienda?.id)

        val sanIsidro = resultado.tiendasElegibles.first { it.tienda.id == "T02" }
        assertFalse(sanIsidro.disponibleHoy)
        assertEquals("Disponible en 24–48 h", sanIsidro.plazoRecojoTexto)
    }

    @Test
    fun `prendas en distintas tiendas habilitan todas las sedes con plazo 24 a 48 h y recomiendan la mas cercana`() {
        val cart = listOf(item("BL-1001_M"), item("JN-1001_28"))
        val stocks = mapOf(
            "BL-1001_M" to Stock("BL-1001_M", total = 2, porTienda = mapOf("T01" to 2)),
            "JN-1001_28" to Stock("JN-1001_28", total = 2, porTienda = mapOf("T03" to 2)),
        )

        val resultado = useCase(cart, stocks, tiendas, destinoSanIsidro)

        assertTrue(resultado.hayStockSuficiente)
        assertEquals(3, resultado.tiendasElegibles.size)
        assertTrue(resultado.tiendasElegibles.all { !it.disponibleHoy })
        assertTrue(resultado.tiendasElegibles.all { it.plazoRecojoTexto == "Disponible en 24–48 h" })
        assertNotNull(resultado.tiendaRecomendada)
        assertEquals("T02", resultado.tiendaRecomendada?.tienda?.id)
    }

    @Test
    fun `sin stock global suficiente bloquea recojo y retorna lista vacia`() {
        val cart = listOf(item("BL-1001_M", cantidad = 5))
        val stocks = mapOf(
            "BL-1001_M" to Stock("BL-1001_M", total = 3, porTienda = mapOf("T01" to 2, "T02" to 1)),
        )

        val resultado = useCase(cart, stocks, tiendas, destinoSanIsidro)

        assertFalse(resultado.hayStockSuficiente)
        assertTrue(resultado.tiendasElegibles.isEmpty())
        assertNull(resultado.tiendaRecomendada)
    }
}

