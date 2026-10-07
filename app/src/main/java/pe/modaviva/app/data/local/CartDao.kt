package pe.modaviva.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a datos del carrito local en Room (HU-09).
 */
@Dao
interface CartDao {

    @Query("SELECT * FROM carrito ORDER BY agregadoEnMillis DESC")
    fun observarCarrito(): Flow<List<CartEntity>>

    @Query("SELECT * FROM carrito WHERE varianteId = :varianteId")
    suspend fun obtenerPorVarianteId(varianteId: String): CartEntity?

    @Query("SELECT * FROM carrito")
    suspend fun obtenerTodos(): List<CartEntity>

    @Query("SELECT COALESCE(SUM(cantidad), 0) FROM carrito")
    fun observarTotalItems(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizar(entity: CartEntity)

    /**
     * CA-02: Si la misma prenda con la misma talla y color ya está en el carrito,
     * cuando la agrego otra vez, entonces se suma la cantidad en lugar de crear otra línea.
     */
    @Transaction
    suspend fun agregarOIncrementar(entity: CartEntity) {
        val existente = obtenerPorVarianteId(entity.varianteId)
        if (existente != null) {
            insertarOActualizar(
                existente.copy(
                    cantidad = existente.cantidad + entity.cantidad,
                    agregadoEnMillis = entity.agregadoEnMillis,
                )
            )
        } else {
            insertarOActualizar(entity)
        }
    }

    @Query("UPDATE carrito SET cantidad = :cantidad WHERE varianteId = :varianteId")
    suspend fun actualizarCantidad(varianteId: String, cantidad: Int)

    @Query("DELETE FROM carrito WHERE varianteId = :varianteId")
    suspend fun eliminarItem(varianteId: String)

    @Query("DELETE FROM carrito")
    suspend fun vaciarCarrito()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarItems(entities: List<CartEntity>)
}
