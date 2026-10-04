package pe.modaviva.app.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.modaviva.app.domain.model.Prenda
import java.time.Instant

/**
 * Catálogo publicado (HU-04). La pantalla siempre lee la copia local, que se
 * mantiene al día con el servidor; así funciona igual con o sin conexión.
 */
interface CatalogoRepository {

    /** Copia local del catálogo, de la prenda más nueva a la más antigua. */
    val prendas: Flow<List<Prenda>>

    /** Momento de la última copia recibida del servidor; null si nunca hubo una. */
    val ultimaActualizacion: Flow<Instant?>

    /**
     * Mientras se recolecta, escucha el catálogo en el servidor y actualiza la
     * copia local con cada cambio (una prenda despublicada desaparece al
     * instante, HU-04 CA-05). Emite cada vez que guarda una versión nueva.
     */
    fun sincronizarEnTiempoReal(): Flow<Unit>

    /** Pide el catálogo al servidor y reemplaza la copia local (pull-to-refresh). */
    suspend fun refrescar(): Result<Unit>
}
