package pe.modaviva.app.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.modaviva.app.domain.model.EstadoPedido
import pe.modaviva.app.domain.model.Pedido
import pe.modaviva.app.domain.model.SolicitudPostventa
import pe.modaviva.app.domain.model.TarjetaGuardada
import pe.modaviva.app.domain.model.TipoMedioPago

interface OrderRepository {
    fun observarPedidos(): Flow<List<Pedido>>
    fun observarPedidoPorId(pedidoId: String): Flow<Pedido?>
    fun observarTarjetasGuardadas(): Flow<List<TarjetaGuardada>>

    suspend fun crearPedido(
        pedido: Pedido,
        idempotencyKey: String? = null,
    ): Result<Pedido>

    suspend fun confirmarPagoPedidoPendiente(
        pedidoId: String,
        medioPago: TipoMedioPago,
        resumenMedioPago: String,
    ): Result<Pedido>

    suspend fun anularPedidoVencido(pedidoId: String): Result<Pedido>

    suspend fun avanzarEstadoDemo(
        pedidoId: String,
        nuevoEstado: EstadoPedido,
    ): Result<Pedido>

    suspend fun registrarSolicitudPostventa(
        pedidoId: String,
        solicitud: SolicitudPostventa,
    ): Result<SolicitudPostventa>

    suspend fun guardarTarjeta(tarjeta: TarjetaGuardada): Result<TarjetaGuardada>
    suspend fun eliminarTarjeta(tarjetaId: String): Result<Unit>

    /**
     * Siembra los pedidos y tarjetas de demostración desde `assets/seed/pedidos_semilla.json`
     * para el cliente autenticado actual. Retorna la cantidad de pedidos sembrados.
     */
    suspend fun cargarDatosSemilla(): Result<Int>
}

