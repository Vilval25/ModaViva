package pe.modaviva.app.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.modaviva.app.domain.model.Tienda

interface TiendaRepository {
    fun observarTiendas(): Flow<List<Tienda>>
}
