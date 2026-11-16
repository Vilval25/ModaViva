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
        soloHistorial: Boolean = false,
    ): Result<ResultadoPruebaVirtual>

    /**
     * HU-12 CA-06: Guardar deja la imagen en el historial; Descartar la borra de Storage,
     * de la caché y del historial.
     */
    suspend fun gestionarResultado(prendaId: String, guardar: Boolean): Result<Unit>

    /** Uso del probador de hoy; null si todavía no generó ninguna prueba. */
    fun observarUsoHoy(): Flow<UsoProbadorHoy?>
}
