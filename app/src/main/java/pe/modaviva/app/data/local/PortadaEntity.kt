package pe.modaviva.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pe.modaviva.app.domain.model.Portada

/** Copia local de la portada de Inicio: una sola fila (id = "portada"). */
@Entity(tableName = "portada")
data class PortadaEntity(
    @PrimaryKey val id: String,
    val etiqueta: String,
    val titulo: String,
    val imagenUrl: String,
    val accion: String,
    val categoria: String,
)

fun PortadaEntity.aDominio() = Portada(
    etiqueta = etiqueta,
    titulo = titulo,
    imagenUrl = imagenUrl,
    accion = accion,
    categoria = categoria,
)

fun Portada.aEntidad() = PortadaEntity(
    id = PortadaDao.ID,
    etiqueta = etiqueta,
    titulo = titulo,
    imagenUrl = imagenUrl,
    accion = accion,
    categoria = categoria,
)

@Dao
interface PortadaDao {

    @Query("SELECT * FROM portada WHERE id = :id")
    fun observar(id: String = ID): Flow<PortadaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(portada: PortadaEntity)

    @Query("DELETE FROM portada")
    suspend fun borrar()

    companion object {
        const val ID = "portada"
    }
}
