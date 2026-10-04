package pe.modaviva.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogoDao {

    @Query("SELECT * FROM prendas ORDER BY creadaEnMillis DESC")
    fun observarPrendas(): Flow<List<PrendaEntity>>

    /**
     * El servidor devuelve siempre el catálogo completo, así que se reemplaza
     * entero: una prenda que ya no está publicada desaparece de la copia local.
     */
    @Transaction
    suspend fun reemplazarCatalogo(prendas: List<PrendaEntity>, sincronizadoEnMillis: Long) {
        borrarPrendas()
        insertarPrendas(prendas)
        guardarSincronizacion(SincronizacionEntity(CATALOGO, sincronizadoEnMillis))
    }

    @Query("SELECT millis FROM sincronizacion WHERE clave = :clave")
    fun observarSincronizacion(clave: String = CATALOGO): Flow<Long?>

    @Query("DELETE FROM prendas")
    suspend fun borrarPrendas()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarPrendas(prendas: List<PrendaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarSincronizacion(sincronizacion: SincronizacionEntity)

    companion object {
        const val CATALOGO = "catalogo"
    }
}

/** Cuándo se recibió por última vez cada conjunto de datos del servidor. */
@Entity(tableName = "sincronizacion")
data class SincronizacionEntity(
    @PrimaryKey val clave: String,
    val millis: Long,
)
