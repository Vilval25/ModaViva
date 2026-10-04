package pe.modaviva.app.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.modaviva.app.domain.model.Stock

interface StockRepository {

    fun observeStock(
        prendaId: String,
        talla: String,
    ): Flow<Stock?>
}