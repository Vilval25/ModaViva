package pe.modaviva.app.orders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.modaviva.app.data.seed.PedidosSeedLoader
import pe.modaviva.app.domain.model.EstadoPedido
import pe.modaviva.app.domain.model.TarjetaGuardada
import java.io.File

class PedidosSeedAndModelTest {

    private fun leerJsonSemillaDeAssets(): String {
        val posiblesRutas = listOf(
            File("src/main/assets/seed/pedidos_semilla.json"),
            File("app/src/main/assets/seed/pedidos_semilla.json"),
        )
        val archivo = posiblesRutas.firstOrNull { it.exists() }
            ?: error("No se encontró pedidos_semilla.json en assets")
        return archivo.readText(Charsets.UTF_8)
    }

    @Test
    fun `parsea correctamente el archivo JSON semilla con tarjetas y pedidos en todos los estados`() {
        val ahora = 1_760_000_000_000L
        val json = leerJsonSemillaDeAssets()

        val resultado = PedidosSeedLoader.parsearJsonSemilla(
            jsonRaw = json,
            uidCliente = "uid-test-01",
            nombreCliente = "Edson Vila",
            ahoraMillis = ahora,
        )

        assertEquals(2, resultado.tarjetas.size)
        assertEquals("Visa", resultado.tarjetas[0].marca)
        assertEquals("4242", resultado.tarjetas[0].ultimos4)
        assertEquals("EDSON VILA", resultado.tarjetas[0].titular)

        assertEquals(5, resultado.pedidos.size)
        val estados = resultado.pedidos.map { it.estado }.toSet()
        assertTrue(estados.contains(EstadoPedido.ENTREGADO))
        assertTrue(estados.contains(EstadoPedido.DESPACHADO))
        assertTrue(estados.contains(EstadoPedido.EN_PREPARACION))
        assertTrue(estados.contains(EstadoPedido.PENDIENTE))
        assertTrue(estados.contains(EstadoPedido.ANULADO))
    }

    @Test
    fun `pedido ENTREGADO hace 48 horas permite cambio o devolucion pero pasados 8 dias ya no`() {
        val ahora = 1_760_000_000_000L
        val json = leerJsonSemillaDeAssets()
        val pedidos = PedidosSeedLoader.parsearJsonSemilla(
            jsonRaw = json,
            uidCliente = "uid-test-01",
            ahoraMillis = ahora,
        ).pedidos

        val entregado = pedidos.first { it.estado == EstadoPedido.ENTREGADO }
        assertTrue(entregado.puedeSolicitarCambioODevolucion(ahora))

        val ochoDiasDespues = ahora + (8L * 24L * 3_600_000L)
        assertFalse(entregado.puedeSolicitarCambioODevolucion(ochoDiasDespues))
    }

    @Test
    fun `pedido PENDIENTE calcula cuenta regresiva de 26 minutos y vence a los 30 minutos`() {
        val ahora = 1_760_000_000_000L
        val json = leerJsonSemillaDeAssets()
        val pedidos = PedidosSeedLoader.parsearJsonSemilla(
            jsonRaw = json,
            uidCliente = "uid-test-01",
            ahoraMillis = ahora,
        ).pedidos

        val pendiente = pedidos.first { it.estado == EstadoPedido.PENDIENTE }
        assertEquals(26L * 60L, pendiente.segundosRestantesPago(ahora))
        assertFalse(pendiente.haVencidoReserva(ahora))

        val mediaHoraDespues = ahora + (30L * 60_000L)
        assertEquals(0L, pendiente.segundosRestantesPago(mediaHoraDespues))
        assertTrue(pendiente.haVencidoReserva(mediaHoraDespues))
    }

    @Test
    fun `tokenizacion sandbox guarda solo marca y ultimos 4 digitos y rechaza tarjetas terminadas en 0000`() {
        val exito = TarjetaGuardada.validarYTokenizarSandbox(
            numeroTarjeta = "4557 8801 2345 9876",
            vencimiento = "08/29",
            cvv = "123",
            titular = "Edson Vila",
            tipo = "CREDITO",
        )
        assertTrue(exito.isSuccess)
        val tarjeta = exito.getOrNull()
        assertNotNull(tarjeta)
        assertEquals("Visa", tarjeta!!.marca)
        assertEquals("9876", tarjeta.ultimos4)
        assertTrue(tarjeta.tokenPasarela.startsWith("tok_sandbox_visa_9876"))

        val rechazo = TarjetaGuardada.validarYTokenizarSandbox(
            numeroTarjeta = "5412 7500 1234 0000",
            vencimiento = "08/29",
            cvv = "123",
            titular = "Edson Vila",
            tipo = "CREDITO",
        )
        assertTrue(rechazo.isFailure)
    }
}

