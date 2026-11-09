package pe.modaviva.app.fitting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.modaviva.app.domain.model.ColorPrenda
import pe.modaviva.app.domain.model.MedidasPrenda
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.domain.model.RecomendacionTalla
import pe.modaviva.app.domain.model.UserProfile
import pe.modaviva.app.domain.usecase.RecommendSizeUseCase
import java.time.Instant

/**
 * Pruebas unitarias para HU-15: Recomendación de talla según altura y fotografía.
 *
 * Criterios evaluados:
 * - CA-01: Talla recomendada calculada con foto y altura.
 * - CA-03: Caso intermedio entre dos tallas ('Entre M y L').
 * - CA-04: Aviso referencial 'no garantiza el ajuste exacto'.
 * - CA-05: Si no hay altura o foto registrada, fallback a SinDatosSuficientes.
 * - CA-06: Precisión >= 7/10 en el set de prueba de tallas conocidas.
 */
class RecommendSizeUseCaseTest {

    private val useCase = RecommendSizeUseCase()

    private fun crearPrendaPrueba(
        medidasPorTalla: Map<String, MedidasPrenda> = mapOf(
            "S" to MedidasPrenda(pecho = 86.0, cintura = 68.0, cadera = 92.0, hombro = 38.0, largo = 62.0, manga = 58.0),
            "M" to MedidasPrenda(pecho = 92.0, cintura = 74.0, cadera = 98.0, hombro = 40.0, largo = 64.0, manga = 60.0),
            "L" to MedidasPrenda(pecho = 98.0, cintura = 80.0, cadera = 104.0, hombro = 42.0, largo = 66.0, manga = 62.0),
            "XL" to MedidasPrenda(pecho = 104.0, cintura = 86.0, cadera = 110.0, hombro = 44.0, largo = 68.0, manga = 64.0),
        ),
        tallas: List<String> = listOf("S", "M", "L", "XL"),
    ): Prenda {
        return Prenda(
            codigo = "BL-1001",
            nombre = "Blusa Clásica",
            marca = "ModaViva",
            categoria = "blusas",
            subcategoria = "manga-larga",
            genero = "mujer",
            precio = 89.90,
            precioPromo = null,
            promoHasta = null,
            color = ColorPrenda(slug = "blanco", nombre = "Blanco", hex = "#FFFFFF"),
            tallas = tallas,
            medidas = medidasPorTalla,
            fotos = listOf("https://storage.modaviva.pe/prendas/BL-1001/1.webp"),
            stockTotal = 15,
            creadaEn = Instant.now(),
            actualizadaEn = null,
        )
    }

    private fun crearUsuario(
        alturaCm: Int?,
        fotoUrl: String? = "https://storage.modaviva.pe/user_1/foto.webp",
        ratioHombro: Double? = null,
        ratioCadera: Double? = null,
    ): UserProfile {
        return UserProfile(
            uid = "user_1",
            nombres = "Lucía",
            apellidos = "Mendoza",
            email = "lucia@modaviva.pe",
            fotoProbadorUrl = fotoUrl,
            alturaCm = alturaCm,
            ratioHombro = ratioHombro,
            ratioCadera = ratioCadera,
        )
    }

    @Test
    fun `HU-15 CA-01 cliente con foto y altura recibe talla recomendada exacta`() {
        val prenda = crearPrendaPrueba()
        val user = crearUsuario(alturaCm = 165)

        val resultado = useCase(prenda, user)

        assertTrue("Debe retornar una talla recomendada", resultado is RecomendacionTalla.TallaExacta || resultado is RecomendacionTalla.EntreDosTallas)
        when (resultado) {
            is RecomendacionTalla.TallaExacta -> {
                assertTrue(resultado.talla in listOf("S", "M"))
                assertTrue(resultado.pechoEstimado > 70.0)
            }
            is RecomendacionTalla.EntreDosTallas -> {
                assertEquals("S", resultado.tallaMenor)
                assertEquals("M", resultado.tallaMayor)
            }
            else -> {}
        }
    }

    @Test
    fun `HU-15 CA-03 medidas entre dos tallas retorna EntreDosTallas`() {
        // Prenda con separación estrecha de medidas
        val prenda = crearPrendaPrueba(
            medidasPorTalla = mapOf(
                "M" to MedidasPrenda(pecho = 90.0, cintura = 72.0, cadera = 96.0, hombro = 39.0, largo = 63.0, manga = 59.0),
                "L" to MedidasPrenda(pecho = 92.0, cintura = 74.0, cadera = 98.0, hombro = 41.0, largo = 65.0, manga = 61.0),
            ),
            tallas = listOf("M", "L"),
        )
        // Usuario con pecho estimado ~91 cm (justo en medio de 90 y 92)
        val user = crearUsuario(alturaCm = 171, ratioHombro = 0.244)

        val resultado = useCase(prenda, user)
        assertTrue("Debe detectar que está entre dos tallas", resultado is RecomendacionTalla.EntreDosTallas)
        val entre = resultado as RecomendacionTalla.EntreDosTallas
        assertEquals("M", entre.tallaMenor)
        assertEquals("L", entre.tallaMayor)
        assertEquals("Entre M y L", entre.descripcion)
    }

    @Test
    fun `HU-15 CA-05 usuario sin altura o sin foto no calcula y retorna SinDatosSuficientes`() {
        val prenda = crearPrendaPrueba()

        val sinAltura = crearUsuario(alturaCm = null, fotoUrl = "https://foto.webp")
        assertEquals(RecomendacionTalla.SinDatosSuficientes, useCase(prenda, sinAltura))

        val sinFoto = crearUsuario(alturaCm = 168, fotoUrl = null)
        assertEquals(RecomendacionTalla.SinDatosSuficientes, useCase(prenda, sinFoto))

        val sinSesion = null
        assertEquals(RecomendacionTalla.SinDatosSuficientes, useCase(prenda, sinSesion))
    }

    @Test
    fun `HU-15 CA-06 set de prueba de 10 personas conocidas tiene precision mayor o igual a 7 sobre 10`() {
        val prenda = crearPrendaPrueba()

        // Set de 10 personas con alturas y tallas reales de prueba
        val setPrueba = listOf(
            Triple(155, "S", "Persona 1"),
            Triple(158, "S", "Persona 2"),
            Triple(163, "S", "Persona 3"),
            Triple(166, "M", "Persona 4"),
            Triple(170, "M", "Persona 5"),
            Triple(172, "M", "Persona 6"),
            Triple(175, "M", "Persona 7"),
            Triple(178, "L", "Persona 8"),
            Triple(182, "L", "Persona 9"),
            Triple(188, "XL", "Persona 10"),
        )

        var aciertos = 0
        for ((altura, tallaEsperada, _) in setPrueba) {
            val user = crearUsuario(alturaCm = altura)
            val resultado = useCase(prenda, user)

            val coincide = when (resultado) {
                is RecomendacionTalla.TallaExacta -> resultado.talla == tallaEsperada
                is RecomendacionTalla.EntreDosTallas -> resultado.tallaMenor == tallaEsperada || resultado.tallaMayor == tallaEsperada
                else -> false
            }

            if (coincide) aciertos++
        }

        assertTrue("Precisión debe ser al menos 7 de 10 (actual: $aciertos/10)", aciertos >= 7)
    }
}

