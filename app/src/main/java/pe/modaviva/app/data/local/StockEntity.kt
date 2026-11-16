package pe.modaviva.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import pe.modaviva.app.domain.model.Stock

/**
 * Copia local de stock/{varianteId} (HU-05 CA-03). Se refresca cada 15 min con StockSyncWorker
 * (solo lo que cambió) y al instante para la prenda que se está viendo.
 */
@Entity(tableName = "stock", indices = [Index("prendaId")])
data class StockEntity(
    @PrimaryKey val varianteId: String,
    val prendaId: String,
    val talla: String,
    /** Stock por tienda codificado como "T01:3;T02:0". */
    val porTienda: String,
    val total: Int,
    val actualizadoEnMillis: Long,
)

/** Suma del stock de todas las tallas de una prenda. */
data class TotalPrenda(
    val prendaId: String,
    val total: Int,
)

fun StockEntity.aDominio() = Stock(
    prendaId = prendaId,
    talla = talla,
    porTienda = porTienda.split(";").filter { it.contains(":") }.associate {
        val (tienda, cantidad) = it.split(":", limit = 2)
        tienda to (cantidad.toIntOrNull() ?: 0)
    },
    total = total,
)

fun Stock.aEntidad(varianteId: String, actualizadoEnMillis: Long) = StockEntity(
    varianteId = varianteId,
    prendaId = prendaId,
    talla = talla,
    porTienda = porTienda.entries.sortedBy { it.key }.joinToString(";") { "${it.key}:${it.value}" },
    total = total,
    actualizadoEnMillis = actualizadoEnMillis,
)

@Dao
interface StockDao {

    @Query("SELECT * FROM stock")
    fun observarTodos(): Flow<List<StockEntity>>

    @Query("SELECT prendaId, SUM(total) AS total FROM stock GROUP BY prendaId")
    fun observarTotalesPorPrenda(): Flow<List<TotalPrenda>>

    /** Fecha del cambio más reciente guardado; la sincronización pide solo lo posterior. */
    @Query("SELECT MAX(actualizadoEnMillis) FROM stock")
    suspend fun ultimaActualizacion(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(stock: List<StockEntity>)

    @Query("DELETE FROM stock")
    suspend fun borrarTodos()

    @Transaction
    suspend fun reemplazarTodos(stock: List<StockEntity>) {
        borrarTodos()
        guardarTodos(stock)
    }
}
