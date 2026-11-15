package pe.modaviva.app.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.modaviva.app.domain.model.PruebaVirtualRegistro

interface AnalyticsRepository {
    fun observarPruebasVirtuales(): Flow<List<PruebaVirtualRegistro>>
}

