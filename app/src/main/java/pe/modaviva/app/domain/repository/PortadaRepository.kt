package pe.modaviva.app.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.modaviva.app.domain.model.Portada

/** Portada de la vitrina de Inicio, con copia local como el catálogo. */
interface PortadaRepository {

    /** Copia local; null si no hay portada activa. */
    val portada: Flow<Portada?>

    /** Mientras se recolecta, mantiene la copia local al día con el servidor. */
    fun sincronizarEnTiempoReal(): Flow<Unit>
}
