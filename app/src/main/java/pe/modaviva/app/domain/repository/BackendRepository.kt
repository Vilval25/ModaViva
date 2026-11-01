package pe.modaviva.app.domain.repository

import pe.modaviva.app.domain.model.BackendPing
import pe.modaviva.app.domain.model.ResultadoCalculoPedido
import pe.modaviva.app.domain.model.SolicitudCalculoPedido
import pe.modaviva.app.domain.model.TarifaEnvio

interface BackendRepository {

    /** Prueba de humo del backend (HT-02 CA-05): llama a la Cloud Function `ping`. */
    suspend fun ping(): Result<BackendPing>

    /** HU-19: Revalida stock, precios, cupones y calcula el total del pedido con la Cloud Function. */
    suspend fun calcularPedido(solicitud: SolicitudCalculoPedido): Result<ResultadoCalculoPedido>

    /** HU-19 / HU-20: Obtiene la lista de distritos y tarifas de envío disponibles. */
    suspend fun obtenerTarifasEnvio(): Result<List<TarifaEnvio>>
}
