package pe.modaviva.app.favorites

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.modaviva.app.domain.model.ColorPrenda
import pe.modaviva.app.domain.model.Favorito
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.domain.model.PrendaRetirada
import pe.modaviva.app.ui.favorites.armarFavoritos
import java.time.Instant

/** Lista de Favoritos cruzada con el catálogo (HU-08 CA-02 y CA-03). */
class ListaFavoritosTest {

    private val ahora = Instant.parse("2026-10-12T12:00:00Z")

    private fun prenda(codigo: String, promo: Double? = null, stock: Int = 5) = Prenda(
        codigo = codigo, nombre = "Prenda $codigo", marca = "Marca", categoria = "c", subcategoria = "s",
        genero = "mujer", precio = 100.0, precioPromo = promo, promoHasta = null,
        color = ColorPrenda("negro", "Negro", "#000000"), tallas = listOf("M"), fotos = listOf("f-$codigo"),
        stockTotal = stock, creadaEn = Instant.EPOCH, actualizadaEn = null,
    )

    private fun favorito(codigo: String, segundos: Long) = Favorito(codigo, Instant.ofEpochSecond(segundos))

    @Test
    fun `respeta el orden de los favoritos`() {
        val favoritos = listOf(favorito("B", 20), favorito("A", 10))
        val lista = armarFavoritos(favoritos, listOf(prenda("A"), prenda("B")), emptyMap(), ahora)
        assertEquals(listOf("B", "A"), lista.map { it.codigo })
    }

    @Test
    fun `muestra el precio vigente y el original si hay promocion`() {
        val item = armarFavoritos(listOf(favorito("A", 1)), listOf(prenda("A", promo = 80.0)), emptyMap(), ahora).single()
        assertEquals("S/ 80.00", item.precio)
        assertEquals("S/ 100.00", item.precioOriginal)
        assertTrue(item.disponible)
    }

    @Test
    fun `marca la prenda agotada`() {
        val item = armarFavoritos(listOf(favorito("A", 1)), listOf(prenda("A", stock = 0)), emptyMap(), ahora).single()
        assertTrue(item.agotada)
        assertTrue(item.disponible)
    }

    @Test
    fun `la prenda que ya no esta publicada queda como no disponible`() {
        val retiradas = mapOf("X" to PrendaRetirada("Abrigo", "Joseph", "foto-x"))
        val item = armarFavoritos(listOf(favorito("X", 1)), listOf(prenda("A")), retiradas, ahora).single()
        assertFalse(item.disponible)
        assertNull(item.precio)
        assertEquals("Abrigo", item.nombre)
        assertEquals("foto-x", item.foto)
    }

    @Test
    fun `sin datos de la retirada el nombre queda vacio`() {
        val item = armarFavoritos(listOf(favorito("X", 1)), emptyList(), emptyMap(), ahora).single()
        assertFalse(item.disponible)
        assertEquals("", item.nombre)
        assertNull(item.marca)
    }
}
