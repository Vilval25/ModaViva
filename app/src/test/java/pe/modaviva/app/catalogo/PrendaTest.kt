package pe.modaviva.app.catalogo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.modaviva.app.domain.model.ColorPrenda
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.ui.format.formatearSoles
import java.time.Instant

/** HU-04 CA-06: la tarjeta muestra el precio vigente. */
class PrendaTest {

    private val hoy = Instant.parse("2026-10-05T12:00:00Z")

    private fun prenda(precioPromo: Double? = null, promoHasta: Instant? = null, stock: Int = 5) = Prenda(
        codigo = "CT-1002", nombre = "Camiseta", marca = "Khaite", categoria = "polos-y-camisetas",
        subcategoria = "camisetas", genero = "mujer", precio = 79.9, precioPromo = precioPromo,
        promoHasta = promoHasta, color = ColorPrenda("mostaza", "Mostaza", "#C9A227"),
        tallas = listOf("S"), fotos = listOf("f"), stockTotal = stock,
        creadaEn = Instant.EPOCH, actualizadaEn = null,
    )

    @Test
    fun `sin promocion se cobra el precio regular`() {
        assertFalse(prenda().enPromocion(hoy))
        assertEquals(79.9, prenda().precioVigente(hoy), 0.0)
    }

    @Test
    fun `con promocion vigente se cobra el precio promocional`() {
        val p = prenda(precioPromo = 59.9, promoHasta = Instant.parse("2026-12-31T23:59:59Z"))
        assertTrue(p.enPromocion(hoy))
        assertEquals(59.9, p.precioVigente(hoy), 0.0)
    }

    @Test
    fun `una promocion vencida ya no aplica`() {
        val p = prenda(precioPromo = 59.9, promoHasta = Instant.parse("2026-09-30T23:59:59Z"))
        assertFalse(p.enPromocion(hoy))
        assertEquals(79.9, p.precioVigente(hoy), 0.0)
    }

    @Test
    fun `una promocion sin fecha de fin sigue vigente`() {
        assertTrue(prenda(precioPromo = 59.9).enPromocion(hoy))
    }

    @Test
    fun `sin stock la prenda esta agotada`() {
        assertTrue(prenda(stock = 0).agotada)
        assertFalse(prenda(stock = 1).agotada)
    }

    @Test
    fun `los precios se muestran en soles con dos decimales`() {
        assertEquals("S/ 79.90", formatearSoles(79.9))
        assertEquals("S/ 549.90", formatearSoles(549.9))
    }
}
