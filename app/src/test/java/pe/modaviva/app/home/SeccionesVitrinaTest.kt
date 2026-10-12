package pe.modaviva.app.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.modaviva.app.domain.model.ColorPrenda
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.ui.home.MAX_POR_CARRUSEL
import pe.modaviva.app.ui.home.seccionesVitrina
import java.time.Instant
import java.time.LocalDate

/** Carruseles de la vitrina de Inicio (HU-04 CA-10). */
class SeccionesVitrinaTest {

    private val ahora = Instant.parse("2026-10-12T12:00:00Z")
    private val hoy = LocalDate.parse("2026-10-12")

    private fun prenda(codigo: String, oferta: Boolean = false, stock: Int = 5) = Prenda(
        codigo = codigo, nombre = codigo, marca = "Marca", categoria = "c", subcategoria = "s",
        genero = "mujer", precio = 100.0, precioPromo = if (oferta) 80.0 else null, promoHasta = null,
        color = ColorPrenda("negro", "Negro", "#000000"), tallas = listOf("M"), fotos = listOf("f"),
        stockTotal = stock, creadaEn = Instant.EPOCH, actualizadaEn = null,
    )

    @Test
    fun `en oferta solo muestra promociones con stock`() {
        val prendas = listOf(prenda("A", oferta = true), prenda("B", oferta = true, stock = 0), prenda("C"))
        assertEquals(listOf("A"), seccionesVitrina(prendas, ahora, hoy).enOferta.map { it.codigo })
    }

    @Test
    fun `lo nuevo conserva todo el catalogo en su orden`() {
        val prendas = List(15) { prenda("P$it", stock = if (it == 3) 0 else 5) }
        assertEquals(prendas, seccionesVitrina(prendas, ahora, hoy).loNuevo)
    }

    @Test
    fun `te podria gustar excluye ofertas y agotadas`() {
        val prendas = listOf(prenda("A", oferta = true), prenda("B", stock = 0), prenda("C"), prenda("D"))
        val codigos = seccionesVitrina(prendas, ahora, hoy).teGusta.map { it.codigo }.toSet()
        assertEquals(setOf("C", "D"), codigos)
    }

    @Test
    fun `te podria gustar no cambia durante el dia pero si entre dias`() {
        val prendas = List(30) { prenda("P$it") }
        val hoy1 = seccionesVitrina(prendas, ahora, hoy).teGusta
        val hoy2 = seccionesVitrina(prendas, ahora, hoy).teGusta
        val manana = seccionesVitrina(prendas, ahora, hoy.plusDays(1)).teGusta
        assertEquals(hoy1, hoy2)
        assertNotEquals(hoy1, manana)
        assertEquals(MAX_POR_CARRUSEL, hoy1.size)
    }

    @Test
    fun `si todo esta en oferta te podria gustar usa las disponibles`() {
        val prendas = listOf(prenda("A", oferta = true), prenda("B", oferta = true))
        val secciones = seccionesVitrina(prendas, ahora, hoy)
        assertTrue(secciones.teGusta.isNotEmpty())
        assertFalse(secciones.teGusta.any { it.agotada })
    }
}
