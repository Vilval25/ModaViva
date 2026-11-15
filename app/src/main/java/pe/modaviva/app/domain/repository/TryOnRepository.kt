package pe.modaviva.app.domain.repository

import pe.modaviva.app.domain.model.ResultadoPruebaVirtual

interface TryOnRepository {
    suspend fun probarPrenda(
        prendaId: String,
        mock: Boolean = false,
        forzarRegeneracion: Boolean = false,
    ): Result<ResultadoPruebaVirtual>
}

