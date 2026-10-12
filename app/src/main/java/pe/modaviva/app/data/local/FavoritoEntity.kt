package pe.modaviva.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import pe.modaviva.app.domain.model.Favorito
import java.time.Instant

/** Copia local de clientes/{uid}/favoritos de la cuenta con sesión (HU-08). */
@Entity(tableName = "favoritos")
data class FavoritoEntity(
    @PrimaryKey val prendaId: String,
    val agregadoEnMillis: Long,
)

fun FavoritoEntity.aDominio() = Favorito(prendaId, Instant.ofEpochMilli(agregadoEnMillis))

fun Favorito.aEntidad() = FavoritoEntity(prendaId, agregadoEn.toEpochMilli())

@Dao
interface FavoritoDao {

    @Query("SELECT * FROM favoritos ORDER BY agregadoEnMillis DESC")
    fun observar(): Flow<List<FavoritoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(favorito: FavoritoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(favoritos: List<FavoritoEntity>)

    @Query("DELETE FROM favoritos WHERE prendaId = :prendaId")
    suspend fun borrar(prendaId: String)

    @Query("DELETE FROM favoritos")
    suspend fun borrarTodos()

    /** Deja la tabla igual a la lista del servidor. */
    @Transaction
    suspend fun reemplazarTodos(favoritos: List<FavoritoEntity>) {
        borrarTodos()
        guardarTodos(favoritos)
    }
}
