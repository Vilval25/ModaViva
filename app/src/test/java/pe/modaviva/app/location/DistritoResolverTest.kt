package pe.modaviva.app.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.modaviva.app.domain.model.DireccionEntrega
import pe.modaviva.app.domain.model.DistritoResolver
import pe.modaviva.app.domain.model.Stock
import pe.modaviva.app.domain.model.Tienda
import pe.modaviva.app.domain.model.aDestinoEntrega
import pe.modaviva.app.domain.usecase.CalculateNearestStoreUseCase

class DistritoResolverTest {

    private val tiendasMuestra = listOf(
        Tienda(id = "T01", nombre = "Tienda Miraflores", lat = -12.1219, lng = -77.0305, distrito = "Miraflores"),
        Tienda(id = "T02", nombre = "Tienda San Isidro", lat = -12.0975, lng = -77.0365, distrito = "San Isidro"),
        Tienda(id = "T03", nombre = "Tienda Surco", lat = -12.1350, lng = -76.9920, distrito = "Santiago de Surco"),
    )

    @Test
    fun `deteccion por texto con direccion completa de Geocoder`() {
        val componentes = listOf("Lima", "Provincia de Lima", "Av. Benavides 1234, Santiago de Surco 15038, Perú")
        val detectado = DistritoResolver.resolverPorTexto(componentes)

        assertNotNull(detectado)
        assertEquals("surco", detectado!!.id)
        assertEquals("Santiago de Surco", detectado.distrito)
    }

    @Test
    fun `deteccion por alias o urbanizacion popular`() {
        // En Surco, Geocoder suele retornar la urbanización "Chama" o "Monterrico"
        val detectadoChama = DistritoResolver.resolverPorTexto(listOf("Chama", "Lima"))
        assertNotNull(detectadoChama)
        assertEquals("Santiago de Surco", detectadoChama!!.distrito)

        // En San Miguel, a veces retorna "Maranga"
        val detectadoMaranga = DistritoResolver.resolverPorTexto(listOf("Maranga"))
        assertNotNull(detectadoMaranga)
        assertEquals("San Miguel", detectadoMaranga!!.distrito)
    }

    @Test
    fun `fallback por coordenadas GPS cuando Geocoder no tiene texto de distrito`() {
        // Coordenadas en Miraflores (-12.1220, -77.0300) sin nombres textuales
        val detectadoGps = DistritoResolver.resolverTarifa(
            componentes = listOf("Lima"),
            direccionCompleta = "Perú",
            lat = -12.1220,
            lng = -77.0300,
        )

        assertNotNull(detectadoGps)
        assertEquals("miraflores", detectadoGps!!.id)
        assertEquals("Miraflores", detectadoGps.distrito)
        assertEquals(-12.1220, detectadoGps.lat!!, 0.0001)
    }

    @Test
    fun `direccion de domicilio sin coordenadas hereda coordenadas de su distrito y mapea tienda mas cercana`() {
        // Domicilio registrado en Santiago de Surco sin GPS manual previo
        val direccionDomicilio = DireccionEntrega(
            id = "DIR-01",
            alias = "Casa",
            destinatario = "Juan Pérez",
            telefono = "999888777",
            direccion = "Av. Benavides 2040",
            distrito = "Santiago de Surco",
            distritoSlug = "surco",
            lat = null,
            lng = null,
            esPrincipal = true,
        )

        val destino = direccionDomicilio.aDestinoEntrega()

        // Verifica que se le asignaron coordenadas del distrito
        assertNotNull(destino.lat)
        assertNotNull(destino.lng)
        assertEquals(-12.1350, destino.lat!!, 0.0001)
        assertEquals(-76.9920, destino.lng!!, 0.0001)

        // Evalúa la tienda más cercana con stock disponible
        val useCase = CalculateNearestStoreUseCase()
        val stock = Stock(
            prendaId = "P01",
            talla = "M",
            porTienda = mapOf("T01" to 5, "T02" to 3, "T03" to 2),
            total = 10,
        )

        val resultado = useCase(destino, stock, tiendasMuestra)

        assertNotNull(resultado.tiendaRecomendada)
        // La tienda más cercana al domicilio en Surco debe ser T03 (Tienda Surco)
        assertEquals("T03", resultado.tiendaRecomendada!!.tienda.id)
        assertTrue(resultado.tiendaRecomendada!!.distanciaKm!! < 1.0)
    }
}
