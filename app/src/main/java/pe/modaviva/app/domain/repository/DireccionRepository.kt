package pe.modaviva.app.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.modaviva.app.domain.model.DireccionEntrega

interface DireccionRepository {
    fun observarDirecciones(): Flow<List<DireccionEntrega>>
    suspend fun guardarDireccion(direccion: DireccionEntrega): Result<Unit>
    suspend fun eliminarDireccion(direccionId: String): Result<Unit>
    suspend fun establecerPrincipal(direccionId: String): Result<Unit>
}
