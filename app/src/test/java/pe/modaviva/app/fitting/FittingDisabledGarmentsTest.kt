package pe.modaviva.app.fitting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.modaviva.app.data.repository.FirebaseFittingConfigRepository
import pe.modaviva.app.domain.model.ColorPrenda
import pe.modaviva.app.domain.model.Prenda
import java.time.Instant

/**
 * Pruebas unitarias para HU-16: Prendas no habilitadas para el probador virtual.
 *
 * Criterios de Aceptación:
 * - CA-01: Dado que una prenda pertenece a una categoría no habilitada, cuando abro su ficha,
 *   entonces no aparece 'Probarme' y veo un mensaje breve con la política.
 * - CA-02: Dado que veo ese mensaje, cuando toco 'Ver tabla de medidas', entonces se abre la tabla de medidas.
 * - CA-03: Dado que se agrega o quita una categoría en Remote Config, cuando pasan como máximo 15 min,
 *   entonces la app aplica el cambio sin publicar una nueva versión.
 * - CA-04: Dado que una prenda no habilitada aparece en catálogo, búsqueda, favoritos o carrito,
 *   entonces no hay acceso al probador. Las prendas habilitadas (Polos, Vestidos, Casacas) sí permiten probarse.
 * - CA-05: Rechazo seguro tanto en cliente como en servidor para solicitudes fuera de política.
 */
class FittingDisabledGarmentsTest {

    private fun crearPrendaPrueba(
        codigo: String,
        categoria: String,
        subcategoria: String = "",
        nombre: String = "Prenda de prueba",
    ): Prenda {
        return Prenda(
            codigo = codigo,
            nombre = nombre,
            descripcion = "Descripción de prueba",
            marca = "ModaViva",
            categoria = categoria,
            subcategoria = subcategoria,
            genero = "Mujer",
            precio = 99.90,
            precioPromo = null,
            promoHasta = null,
            color = ColorPrenda(slug = "negro", nombre = "Negro", hex = "#000000"),
            tallas = listOf("S", "M", "L"),
            fotos = listOf("https://ejemplo.com/foto.jpg"),
            stockTotal = 10,
            creadaEn = Instant.now(),
            actualizadaEn = null,
        )
    }

    @Test
    fun `HU-16 CA-01 normalizacion maneja tildes mayusculas y separadores`() {
        assertEquals("ropa-interior", FirebaseFittingConfigRepository.normalizar("Ropa Interior"))
        assertEquals("lenceria", FirebaseFittingConfigRepository.normalizar("Lencería"))
        assertEquals("trajes-de-bano", FirebaseFittingConfigRepository.normalizar("TRAJES_DE_BAÑO"))
        assertEquals("ropa-de-bano", FirebaseFittingConfigRepository.normalizar("Ropa de Baño"))
        assertEquals("calzado", FirebaseFittingConfigRepository.normalizar("  Calzado  "))
    }

    @Test
    fun `HU-16 CA-01 parseCategories procesa multiples delimitadores`() {
        val raw = "ropa-interior, lenceria;\ntrajes-de-bano;calzado"
        val parsed = FirebaseFittingConfigRepository.parseCategories(raw)

        assertTrue(parsed.contains("ropa-interior"))
        assertTrue(parsed.contains("lenceria"))
        assertTrue(parsed.contains("trajes-de-bano"))
        assertTrue(parsed.contains("calzado"))
        assertEquals(4, parsed.size)
    }

    @Test
    fun `HU-16 CA-01 prendas con categorias restringidas no estan habilitadas para el probador`() {
        val prendasRestringidas = listOf(
            crearPrendaPrueba("INT-01", categoria = "Ropa Interior"),
            crearPrendaPrueba("LEN-02", categoria = "Lencería"),
            crearPrendaPrueba("SWIM-01", categoria = "Trajes de baño"),
            crearPrendaPrueba("SWIM-02", categoria = "Ropa de baño", subcategoria = "Bikinis"),
            crearPrendaPrueba("ACC-01", categoria = "Accesorios"),
            crearPrendaPrueba("CAL-01", categoria = "Calzado"),
        )

        val setRestringido = FirebaseFittingConfigRepository.DEFAULT_DISABLED_CATEGORIES

        prendasRestringidas.forEach { prenda ->
            val catNorm = FirebaseFittingConfigRepository.normalizar(prenda.categoria)
            val subNorm = prenda.subcategoria?.let { FirebaseFittingConfigRepository.normalizar(it) }

            val estaDeshabilitada = setRestringido.contains(catNorm) ||
                (subNorm != null && setRestringido.contains(subNorm)) ||
                setRestringido.any { dis -> catNorm.contains(dis) || (subNorm != null && subNorm.contains(dis)) }

            assertTrue("Prenda ${prenda.codigo} (${prenda.categoria}) debería estar inhabilitada para el probador", estaDeshabilitada)
        }
    }

    @Test
    fun `HU-16 CA-04 prendas comunes (polos, vestidos, casacas, pantalones) si estan habilitadas`() {
        val prendasHabilitadas = listOf(
            crearPrendaPrueba("POL-01", categoria = "Polos"),
            crearPrendaPrueba("VES-01", categoria = "Vestidos"),
            crearPrendaPrueba("CAS-01", categoria = "Casacas", subcategoria = "Abrigos"),
            crearPrendaPrueba("PAN-01", categoria = "Pantalones", subcategoria = "Jeans"),
            crearPrendaPrueba("CAM-01", categoria = "Camisas"),
        )

        val setRestringido = FirebaseFittingConfigRepository.DEFAULT_DISABLED_CATEGORIES

        prendasHabilitadas.forEach { prenda ->
            val catNorm = FirebaseFittingConfigRepository.normalizar(prenda.categoria)
            val subNorm = prenda.subcategoria?.let { FirebaseFittingConfigRepository.normalizar(it) }

            val estaDeshabilitada = setRestringido.contains(catNorm) ||
                (subNorm != null && setRestringido.contains(subNorm)) ||
                setRestringido.any { dis -> catNorm.contains(dis) || (subNorm != null && subNorm.contains(dis)) }

            assertFalse("Prenda ${prenda.codigo} (${prenda.categoria}) debería estar HABILITADA", estaDeshabilitada)
        }
    }

    @Test
    fun `HU-16 CA-03 intervalo de refresco de Remote Config es de 15 minutos (900s)`() {
        assertEquals(900L, FirebaseFittingConfigRepository.FETCH_INTERVAL_SECONDS)
        assertEquals("probador_categorias_deshabilitadas", FirebaseFittingConfigRepository.KEY_DISABLED_CATEGORIES)
    }

    @Test
    fun `HU-16 CA-03 actualizacion dinamica de categorias restringe nuevas categorias sin recompilar`() {
        val prendaBermuda = crearPrendaPrueba("BER-01", categoria = "Bermudas")

        // Inicialmente con defaults, bermudas está habilitada
        val configInicial = FirebaseFittingConfigRepository.DEFAULT_DISABLED_CATEGORIES
        val catNorm = FirebaseFittingConfigRepository.normalizar(prendaBermuda.categoria)
        assertFalse(configInicial.contains(catNorm))

        // Al recibir actualización de Remote Config con "bermudas" agregada
        val nuevaConfigStr = "${FirebaseFittingConfigRepository.DEFAULT_DISABLED_STRING},bermudas"
        val configActualizada = FirebaseFittingConfigRepository.parseCategories(nuevaConfigStr)

        assertTrue(configActualizada.contains("bermudas"))
        assertTrue("Bermudas ahora está restringida dinámicamente", configActualizada.contains(catNorm))
    }
}
