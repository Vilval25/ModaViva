package pe.modaviva.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.modaviva.app.data.local.PortadaDao
import pe.modaviva.app.data.local.aDominio
import pe.modaviva.app.data.local.aEntidad
import pe.modaviva.app.data.remote.PortadaRemoteDataSource
import pe.modaviva.app.domain.model.Portada
import pe.modaviva.app.domain.repository.PortadaRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Portada de Firestore con copia local en Room, igual que el catálogo. */
@Singleton
class FirestorePortadaRepository @Inject constructor(
    private val remoto: PortadaRemoteDataSource,
    private val dao: PortadaDao,
) : PortadaRepository {

    override val portada: Flow<Portada?> = dao.observar().map { it?.aDominio() }

    override fun sincronizarEnTiempoReal(): Flow<Unit> = remoto.escuchar().map { portada ->
        if (portada == null) dao.borrar() else dao.guardar(portada.aEntidad())
    }
}
