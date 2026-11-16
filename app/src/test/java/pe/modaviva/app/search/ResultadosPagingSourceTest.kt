package pe.modaviva.app.search

import androidx.paging.PagingSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.modaviva.app.domain.model.ColorPrenda
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.ui.search.ResultadosPagingSource
import java.time.Instant

/** HU-06 CA-05: los resultados se cargan de 20 en 20 y la paginación termina al final de la lista. */
class ResultadosPagingSourceTest {

    private fun prendas(cantidad: Int) = (1..cantidad).map { i ->
        Prenda(
            codigo = "CT-$i", nombre = "Prenda $i", marca = "Marca", categoria = "polos",
            subcategoria = "camisetas", genero = "mujer", precio = 50.0, precioPromo = null,
            promoHasta = null, color = ColorPrenda("negro", "Negro", "#000000"),
            tallas = listOf("M"), fotos = listOf("f"), stockTotal = 3,
            creadaEn = Instant.EPOCH, actualizadaEn = null,
        )
    }

    private suspend fun cargar(fuente: ResultadosPagingSource, clave: Int?) =
        fuente.load(
            if (clave == null) {
                PagingSource.LoadParams.Refresh(null, 20, false)
            } else {
                PagingSource.LoadParams.Append(clave, 20, false)
            },
        ) as PagingSource.LoadResult.Page

    @Test
    fun `la primera pagina trae 20 y apunta a la siguiente`() = runTest {
        val fuente = ResultadosPagingSource(prendas(45), esperaEntrePaginasMs = 0)
        val pagina = cargar(fuente, null)
        assertEquals(20, pagina.data.size)
        assertEquals("CT-1", pagina.data.first().codigo)
        assertNull(pagina.prevKey)
        assertEquals(20, pagina.nextKey)
    }

    @Test
    fun `la ultima pagina trae el resto y no tiene siguiente`() = runTest {
        val fuente = ResultadosPagingSource(prendas(45), esperaEntrePaginasMs = 0)
        val tercera = cargar(fuente, 40)
        assertEquals(5, tercera.data.size)
        assertEquals("CT-41", tercera.data.first().codigo)
        assertNull(tercera.nextKey)
    }

    @Test
    fun `sin resultados devuelve una pagina vacia`() = runTest {
        val pagina = cargar(ResultadosPagingSource(emptyList(), esperaEntrePaginasMs = 0), null)
        assertEquals(0, pagina.data.size)
        assertNull(pagina.nextKey)
    }
}
