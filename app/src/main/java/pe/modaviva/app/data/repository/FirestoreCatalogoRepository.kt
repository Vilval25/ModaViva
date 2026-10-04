package pe.modaviva.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.modaviva.app.data.local.CatalogoDao
import pe.modaviva.app.data.local.aDominio
import pe.modaviva.app.data.local.aEntidad
import pe.modaviva.app.data.remote.CatalogoRemoteDataSource
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.domain.repository.CatalogoRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Catálogo de Firestore con copia local en Room (HU-04). */
@Singleton
class FirestoreCatalogoRepository @Inject constructor(
    private val remoto: CatalogoRemoteDataSource,
    private val dao: CatalogoDao,
    private val reloj: Clock,
) : CatalogoRepository {

    override val prendas: Flow<List<Prenda>> =
        dao.observarPrendas().map { lista -> lista.map { it.aDominio() } }

    override val ultimaActualizacion: Flow<Instant?> =
        dao.observarSincronizacion().map { millis -> millis?.let(Instant::ofEpochMilli) }

    override fun sincronizarEnTiempoReal(): Flow<Unit> =
        remoto.escuchar().map { guardar(it) }

    override suspend fun refrescar(): Result<Unit> = runCatching { guardar(remoto.obtener()) }

    private suspend fun guardar(prendas: List<Prenda>) {
        dao.reemplazarCatalogo(prendas.map { it.aEntidad() }, reloj.millis())
    }
}
