package pe.modaviva.app.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.modaviva.app.domain.model.ResultadoPruebaVirtual
import pe.modaviva.app.domain.model.UsoProbadorHoy

interface TryOnRepository {
    suspend fun probarPrenda(
        prendaId: String,
        talla: String = "",
        color: String = "",
        mock: Boolean = false,
        forzarRegeneracion: Boolean = false,
    ): Result<ResultadoPruebaVirtual>

    /** Uso del probador de hoy; null si todavía no generó ninguna prueba. */
    fun observarUsoHoy(): Flow<UsoProbadorHoy?>
}
