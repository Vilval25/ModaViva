package pe.modaviva.app.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.modaviva.app.domain.model.DatosTarjetaNueva
import pe.modaviva.app.domain.model.EstadoPedido
import pe.modaviva.app.domain.model.Pedido
import pe.modaviva.app.domain.model.ResultadoPago
import pe.modaviva.app.domain.model.ResultadoRegistroTarjeta
import pe.modaviva.app.domain.model.SolicitudPago
import pe.modaviva.app.domain.model.TarjetaGuardada
import pe.modaviva.app.domain.model.TipoMedioPago

interface OrderRepository {
    fun observarPedidos(): Flow<List<Pedido>>
    fun observarPedidoPorId(pedidoId: String): Flow<Pedido?>
    fun observarTarjetasGuardadas(): Flow<List<TarjetaGuardada>>

    /**
     * HU-21: cobra en la pasarela sandbox y crea el pedido en el servidor (Cloud Function
     * `crearPedido`). Un rechazo de la pasarela llega como [ResultadoPago.Rechazado], no como error.
     */
    suspend fun pagarPedido(solicitud: SolicitudPago): Result<ResultadoPago>

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

    /**
     * HU-21 CA-04, CA-05: envía la tarjeta a la pasarela del servidor (Cloud Function
     * `registrarTarjeta`). Si la pasarela la rechaza, no se guarda y llega el motivo.
     */
    suspend fun registrarTarjeta(datos: DatosTarjetaNueva): Result<ResultadoRegistroTarjeta>
    suspend fun establecerTarjetaPredeterminada(tarjetaId: String): Result<Unit>
    suspend fun eliminarTarjeta(tarjetaId: String): Result<Unit>

    /**
     * Siembra los pedidos y tarjetas de demostración desde `assets/seed/pedidos_semilla.json`
     * para el cliente autenticado actual. Retorna la cantidad de pedidos sembrados.
     */
    suspend fun cargarDatosSemilla(): Result<Int>
}

