package pe.modaviva.app.catalogo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import pe.modaviva.app.data.remote.PrendaMapper
import java.util.Date

/** HU-04 CA-04: una ficha incompleta no se muestra aunque esté publicada. */
class PrendaMapperTest {

    private fun fichaCompleta(): MutableMap<String, Any?> = mutableMapOf(
        "codigo" to "JN-1001",
        "nombre" to "Jean skinny",
        "marca" to "Chloé",
        "categoria" to "jeans",
        "subcategoria" to "skinny",
        "precio" to 179.9,
        "precioPromo" to null,
        "promoHasta" to null,
        "color" to mapOf("slug" to "azul-indigo", "nombre" to "Azul índigo", "hex" to "#2E3FBF"),
        "tallas" to listOf("26", "27"),
        "fotos" to listOf("https://ejemplo/1.jpg"),
        "medidas" to mapOf(
            "26" to mapOf("cintura" to 72),
            "27" to mapOf("cintura" to 74),
        ),
        "stockTotal" to 12L,
        "creadaEn" to Date(0),
    )

    private fun mapear(data: Map<String, Any?>) = PrendaMapper.desdeFirestore("JN-1001", data)

    @Test
    fun `una ficha completa se convierte en prenda`() {
        val prenda = mapear(fichaCompleta())
        assertNotNull(prenda)
        assertEquals("JN-1001", prenda!!.codigo)
        assertEquals(179.9, prenda.precio, 0.0)
        assertEquals("Azul índigo", prenda.color.nombre)
        assertEquals(12, prenda.stockTotal)
    }

    @Test
    fun `sin codigo no se muestra`() {
        assertNull(mapear(fichaCompleta().apply { remove("codigo") }))
        assertNull(mapear(fichaCompleta().apply { put("codigo", " ") }))
    }

    @Test
    fun `sin precio valido no se muestra`() {
        assertNull(mapear(fichaCompleta().apply { remove("precio") }))
        assertNull(mapear(fichaCompleta().apply { put("precio", 0) }))
    }

    @Test
    fun `sin tallas no se muestra`() {
        assertNull(mapear(fichaCompleta().apply { put("tallas", emptyList<String>()) }))
    }

    @Test
    fun `sin color no se muestra`() {
        assertNull(mapear(fichaCompleta().apply { remove("color") }))
        assertNull(mapear(fichaCompleta().apply { put("color", mapOf("slug" to "rojo")) }))
    }

    @Test
    fun `sin fotos no se muestra`() {
        assertNull(mapear(fichaCompleta().apply { put("fotos", emptyList<String>()) }))
    }

    @Test
    fun `sin medidas de alguna talla no se muestra`() {
        assertNull(mapear(fichaCompleta().apply { remove("medidas") }))
        assertNull(mapear(fichaCompleta().apply { put("medidas", mapOf("26" to mapOf("cintura" to 72))) }))
    }

    @Test
    fun `un documento cuyo id no coincide con el codigo se descarta`() {
        assertNull(PrendaMapper.desdeFirestore("OTRO", fichaCompleta()))
    }
}
