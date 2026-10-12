package pe.modaviva.app.ui.home

import pe.modaviva.app.domain.model.Prenda
import java.time.Instant
import java.time.LocalDate
import kotlin.random.Random

/** Prendas de cada carrusel de la vitrina de Inicio, en orden de aparición. */
data class SeccionesVitrina(
    val enOferta: List<Prenda>,
    val loNuevo: List<Prenda>,
    val teGusta: List<Prenda>,
)

/** Máximo de prendas en En Oferta y Te podría gustar. */
const val MAX_POR_CARRUSEL = 10

/**
 * Reparte el catálogo (ya ordenado de la más nueva a la más antigua) entre
 * los carruseles de Inicio:
 *
 * - **En Oferta:** con promoción vigente y stock.
 * - **Lo Nuevo:** todo el catálogo publicado, de lo más reciente a lo más
 *   antiguo. No se recorta: así toda prenda publicada aparece en Inicio
 *   (HU-04 CA-03) hasta que exista el listado completo de HU-06.
 * - **Te podría gustar:** provisional hasta las recomendaciones de HU-26.
 *   Prendas con stock que no están en En Oferta, en un orden que cambia cada
 *   día pero es el mismo durante el día (no salta al volver a Inicio).
 */
fun seccionesVitrina(prendas: List<Prenda>, ahora: Instant, hoy: LocalDate): SeccionesVitrina {
    val disponibles = prendas.filter { !it.agotada }
    val enOferta = disponibles.filter { it.enPromocion(ahora) }.take(MAX_POR_CARRUSEL)
    val codigosOferta = enOferta.mapTo(HashSet()) { it.codigo }
    val teGusta = disponibles
        .filterNot { it.codigo in codigosOferta }
        .ifEmpty { disponibles }
        .shuffled(Random(hoy.toEpochDay()))
        .take(MAX_POR_CARRUSEL)
    return SeccionesVitrina(enOferta = enOferta, loNuevo = prendas, teGusta = teGusta)
}
