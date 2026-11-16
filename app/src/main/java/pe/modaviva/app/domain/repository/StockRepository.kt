package pe.modaviva.app.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.modaviva.app.domain.model.Stock

interface StockRepository {

    /** Stock de una talla en tiempo real (la que el cliente eligió en la ficha). */
    fun observeStock(
        prendaId: String,
        talla: String,
    ): Flow<Stock?>

    /** Todo el stock desde la copia local (HU-05 CA-03: se refresca como máximo cada 15 min). */
    fun observeStocks(): Flow<Map<String, Stock>>

    /**
     * Stock de todas las tallas de una prenda en tiempo real, para la ficha que se está viendo
     * (HU-05 CA-03 y CA-05: el cambio se ve de inmediato). Por defecto filtra la copia local.
     */
    fun observeStocksDePrenda(prendaId: String): Flow<Map<String, Stock>> =
        observeStocks().map { mapa -> mapa.filterValues { it.prendaId == prendaId } }

    /** Trae del servidor el stock que cambió desde la última sincronización. Devuelve cuántos. */
    suspend fun sincronizar(): Result<Int> = Result.success(0)
}
