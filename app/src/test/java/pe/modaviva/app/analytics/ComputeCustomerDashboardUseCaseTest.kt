package pe.modaviva.app.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.modaviva.app.domain.model.ColorPrenda
import pe.modaviva.app.domain.model.EstadoPedido
import pe.modaviva.app.domain.model.ItemPedido
import pe.modaviva.app.domain.model.Pedido
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.domain.model.PruebaVirtualRegistro
import pe.modaviva.app.domain.usecase.ComputeCustomerDashboardUseCase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ComputeCustomerDashboardUseCaseTest {

    private val useCase = ComputeCustomerDashboardUseCase()
    private val zona = ZoneId.of("America/Lima")

    private fun epoch(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atTime(12, 0).atZone(zona).toInstant().toEpochMilli()

    private fun prendaBase(codigo: String, precio: Double, precioPromo: Double? = null) = Prenda(
        codigo = codigo,
        nombre = "Prenda $codigo",
        marca = "ModaViva",
        categoria = "casacas-y-abrigos",
        subcategoria = "casacas",
        genero = "mujer",
        precio = precio,
        precioPromo = precioPromo,
        promoHasta = null,
        color = ColorPrenda("negro", "Negro", "#000000"),
        tallas = listOf("S", "M"),
        fotos = emptyList(),
        stockTotal = 5,
        creadaEn = Instant.now(),
        actualizadaEn = null,
    )

    private fun pedidoBase(
        id: String,
        estado: EstadoPedido,
        creadoEnMillis: Long,
        total: Double,
        descuentoTotal: Double = 0.0,
        items: List<ItemPedido>,
    ) = Pedido(
        id = id,
        numeroPedido = id.uppercase(),
        uidCliente = "uid-1",
        estado = estado,
        tipoEntrega = "delivery",
        items = items,
        subtotal = items.sumOf { it.subtotal },
        descuentoTotal = descuentoTotal,
        total = total,
        creadoEnMillis = creadoEnMillis,
        actualizadoEnMillis = creadoEnMillis,
    )

    @Test
    fun `CA-01 excluye pedidos anulados y pendientes y agrupa monto por mes`() {
        val pedidos = listOf(
            pedidoBase(
                id = "p1",
                estado = EstadoPedido.ENTREGADO,
                creadoEnMillis = epoch(2026, 9, 10),
                total = 200.0,
                items = listOf(ItemPedido("A1", "A1", "A1", "", "M", 1, 200.0, 200.0)),
            ),
            pedidoBase(
                id = "p2",
                estado = EstadoPedido.ANULADO,
                creadoEnMillis = epoch(2026, 9, 15),
                total = 500.0,
                items = listOf(ItemPedido("A2", "A2", "A2", "", "M", 1, 500.0, 500.0)),
            ),
            pedidoBase(
                id = "p3",
                estado = EstadoPedido.PENDIENTE,
                creadoEnMillis = epoch(2026, 10, 1),
                total = 150.0,
                items = listOf(ItemPedido("A3", "A3", "A3", "", "M", 1, 150.0, 150.0)),
            ),
            pedidoBase(
                id = "p4",
                estado = EstadoPedido.DESPACHADO,
                creadoEnMillis = epoch(2026, 10, 5),
                total = 320.0,
                items = listOf(ItemPedido("A4", "A4", "A4", "", "M", 1, 320.0, 320.0)),
            ),
        )

        val resumen = useCase(pedidos, emptyMap(), emptyList(), null, null, zona)

        assertFalse(resumen.sinDatosEnRango)
        assertEquals(2, resumen.totalPedidosAprobados)
        assertEquals(1, resumen.pedidosAnuladosExcluidos)
        assertEquals(520.0, resumen.totalGastado, 0.01)
        assertEquals(listOf("Sep", "Oct"), resumen.meses.map { it.etiquetaMes })
        assertEquals(200.0, resumen.meses[0].montoComprado, 0.01)
        assertEquals(320.0, resumen.meses[1].montoComprado, 0.01)
    }

    @Test
    fun `CA-02 compara prendas probadas frente a compradas`() {
        val pedidos = listOf(
            pedidoBase(
                id = "p1",
                estado = EstadoPedido.ENTREGADO,
                creadoEnMillis = epoch(2026, 10, 8),
                total = 389.90,
                items = listOf(
                    ItemPedido("CS-1002", "CS-1002", "Casaca", "", "S", 1, 389.90, 389.90, usoProbadorVirtual = true),
                ),
            ),
        )
        val pruebas = listOf(
            PruebaVirtualRegistro("CS-1002", "Casaca", "casacas", epoch(2026, 10, 7), comprada = true),
            PruebaVirtualRegistro("AB-1001", "Abrigo", "abrigos", epoch(2026, 10, 9), comprada = false),
        )

        val resumen = useCase(pedidos, emptyMap(), pruebas, null, null, zona)

        assertEquals(2, resumen.totalPrendasProbadas)
        assertEquals(1, resumen.totalPrendasCompradasConPrueba)
        assertEquals(50, resumen.porcentajeConversionProbador)
    }

    @Test
    fun `CA-03 suma ahorro por precio promocional y cupones aplicados`() {
        val catalogo = mapOf(
            "CH-1002" to prendaBase("CH-1002", precio = 459.90, precioPromo = 369.90),
        )
        val pedidos = listOf(
            pedidoBase(
                id = "p1",
                estado = EstadoPedido.APROBADO,
                creadoEnMillis = epoch(2026, 10, 12),
                total = 339.90,
                descuentoTotal = 30.0,
                items = listOf(
                    ItemPedido("CH-1002", "CH-1002", "Chompa", "", "S", 1, 369.90, 369.90),
                ),
            ),
        )

        val resumen = useCase(pedidos, catalogo, emptyList(), null, null, zona)

        assertEquals(90.0, resumen.ahorroPorPromociones, 0.01)
        assertEquals(30.0, resumen.ahorroPorCupones, 0.01)
        assertEquals(120.0, resumen.totalAhorrado, 0.01)
    }

    @Test
    fun `CA-04 y CA-05 filtran por rango y detectan estado vacio`() {
        val pedidos = listOf(
            pedidoBase(
                id = "p1",
                estado = EstadoPedido.ENTREGADO,
                creadoEnMillis = epoch(2026, 8, 20),
                total = 150.0,
                items = listOf(ItemPedido("A1", "A1", "A1", "", "M", 1, 150.0, 150.0)),
            ),
        )

        val fueraDeRango = useCase(
            pedidos = pedidos,
            catalogoPorCodigo = emptyMap(),
            pruebasVirtuales = emptyList(),
            desdeMillis = epoch(2026, 10, 1),
            hastaMillis = epoch(2026, 11, 1),
            zoneId = zona,
        )

        assertTrue(fueraDeRango.sinDatosEnRango)
        assertEquals(0, fueraDeRango.totalPedidosAprobados)
    }
}

