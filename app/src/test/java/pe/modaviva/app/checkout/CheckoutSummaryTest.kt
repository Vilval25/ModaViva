package pe.modaviva.app.checkout

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pe.modaviva.app.domain.model.BackendPing
import pe.modaviva.app.domain.model.CartItem
import pe.modaviva.app.domain.model.CuponAplicado
import pe.modaviva.app.domain.model.ItemPedidoCalculado
import pe.modaviva.app.domain.model.MedidasPrenda
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.domain.model.PrendaColor
import pe.modaviva.app.domain.model.ResultadoCalculoPedido
import pe.modaviva.app.domain.model.SolicitudCalculoPedido
import pe.modaviva.app.domain.model.Stock
import pe.modaviva.app.domain.model.TarifaEnvio
import pe.modaviva.app.domain.repository.BackendRepository
import pe.modaviva.app.domain.repository.CartRepository
import pe.modaviva.app.domain.repository.CatalogoRepository
import pe.modaviva.app.domain.repository.StockRepository
import pe.modaviva.app.ui.checkout.CheckoutSummaryViewModel
import pe.modaviva.app.ui.checkout.TipoEntregaUi
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Pruebas unitarias para HU-19 — Cálculo del pedido antes de pagar.
 * 
 * Criterios de Aceptación:
 * - CA-01: Revalidación de stock y precio de cada ítem antes de mostrar el total.
 * - CA-02: Total = subtotal − descuento + envío.
 * - CA-03: Envío según distrito, o S/ 0 si es recojo.
 * - CA-04: Cupones: motivo específico de rechazo (inexistente, vencido o monto mínimo no alcanzado).
 * - CA-05: Rechazo de cupones múltiples salvo si son combinables.
 * - CA-06: Notificación de ítem agotado o cambio de precio recalculando el total.
 */
class CheckoutSummaryTest {

    private val relojFijo = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneId.of("UTC"))

    private lateinit var fakeCartRepo: FakeCartRepository
    private lateinit var fakeCatalogoRepo: FakeCatalogoRepository
    private lateinit var fakeStockRepo: FakeStockRepository
    private lateinit var fakeBackendRepo: FakeBackendRepository

    @Before
    fun setUp() {
        fakeCartRepo = FakeCartRepository()
        fakeCatalogoRepo = FakeCatalogoRepository()
        fakeStockRepo = FakeStockRepository()
        fakeBackendRepo = FakeBackendRepository()
    }

    private fun crearViewModel(): CheckoutSummaryViewModel {
        return CheckoutSummaryViewModel(
            cartRepository = fakeCartRepo,
            catalogoRepository = fakeCatalogoRepo,
            stockRepository = fakeStockRepo,
            backendRepository = fakeBackendRepo,
            reloj = relojFijo,
        )
    }

    @Test
    fun `CA-01 y CA-02 resumen calcula subtotal, envio y total igual a subtotal menos descuento mas envio`() = runTest {
        val prenda = prendaBase(codigo = "BL-1001", precio = 100.0)
        fakeCatalogoRepo.prendasFlow.value = listOf(prenda)
        fakeStockRepo.stocksFlow.value = mapOf("BL-1001_M" to Stock("BL-1001_M", total = 10))
        fakeCartRepo.itemsFlow.value = listOf(
            CartItem(
                varianteId = "BL-1001_M",
                prendaId = "BL-1001",
                talla = "M",
                colorNombre = "Azul",
                colorHex = "#0000FF",
                cantidad = 2,
                precioAlAgregar = 100.0,
            )
        )

        val vm = crearViewModel()
        val state = vm.uiState.value

        assertEquals(200.0, state.subtotal, 0.01)
        assertEquals(0.0, state.descuentoTotal, 0.01)
        assertEquals(8.0, state.costoEnvio, 0.01) // Tarifa por defecto Miraflores
        // CA-02: total == subtotal - descuento + envio
        val totalEsperado = state.subtotal - state.descuentoTotal + state.costoEnvio
        assertEquals(totalEsperado, state.total, 0.01)
        assertEquals(208.0, state.total, 0.01)
    }

    @Test
    fun `CA-03 al seleccionar recojo en tienda el envio es 0 soles`() = runTest {
        val prenda = prendaBase(codigo = "BL-1001", precio = 100.0)
        fakeCatalogoRepo.prendasFlow.value = listOf(prenda)
        fakeStockRepo.stocksFlow.value = mapOf("BL-1001_M" to Stock("BL-1001_M", total = 10))
        fakeCartRepo.itemsFlow.value = listOf(
            CartItem(
                varianteId = "BL-1001_M",
                prendaId = "BL-1001",
                talla = "M",
                colorNombre = "Azul",
                colorHex = "#0000FF",
                cantidad = 1,
                precioAlAgregar = 100.0,
            )
        )

        val vm = crearViewModel()
        vm.onTipoEntregaChanged(TipoEntregaUi.RECOJO)

        val state = vm.uiState.value
        assertEquals(TipoEntregaUi.RECOJO, state.tipoEntrega)
        assertEquals(0.0, state.costoEnvio, 0.01)
        assertEquals(100.0, state.total, 0.01)
    }

    @Test
    fun `CA-04 cupon inexistente o con monto minimo no alcanzado muestra motivo especifico`() = runTest {
        val prenda = prendaBase(codigo = "BL-1001", precio = 30.0)
        fakeCatalogoRepo.prendasFlow.value = listOf(prenda)
        fakeStockRepo.stocksFlow.value = mapOf("BL-1001_M" to Stock("BL-1001_M", total = 10))
        fakeCartRepo.itemsFlow.value = listOf(
            CartItem(
                varianteId = "BL-1001_M",
                prendaId = "BL-1001",
                talla = "M",
                colorNombre = "Azul",
                colorHex = "#0000FF",
                cantidad = 1,
                precioAlAgregar = 30.0,
            )
        )

        val vm = crearViewModel()

        // 1. Cupón inexistente
        vm.onCuponInputChanged("CUPON_FALSO_123")
        vm.aplicarCupon()
        assertNotNull(vm.uiState.value.mensajeErrorCupon)
        assertTrue(vm.uiState.value.mensajeErrorCupon!!.contains("no existe", ignoreCase = true))

        // 2. Monto mínimo no alcanzado (MODA10 requiere min S/ 50, tenemos S/ 30)
        vm.onCuponInputChanged("MODA10")
        vm.aplicarCupon()
        assertNotNull(vm.uiState.value.mensajeErrorCupon)
        assertTrue(vm.uiState.value.mensajeErrorCupon!!.contains("mínima", ignoreCase = true))
        assertEquals(0.0, vm.uiState.value.descuentoTotal, 0.01)
    }

    @Test
    fun `CA-05 cupon valido aplica descuento y se rechaza un segundo cupon no combinable`() = runTest {
        val prenda = prendaBase(codigo = "BL-1001", precio = 100.0)
        fakeCatalogoRepo.prendasFlow.value = listOf(prenda)
        fakeStockRepo.stocksFlow.value = mapOf("BL-1001_M" to Stock("BL-1001_M", total = 10))
        fakeCartRepo.itemsFlow.value = listOf(
            CartItem(
                varianteId = "BL-1001_M",
                prendaId = "BL-1001",
                talla = "M",
                colorNombre = "Azul",
                colorHex = "#0000FF",
                cantidad = 1,
                precioAlAgregar = 100.0,
            )
        )

        val vm = crearViewModel()

        // Aplicar cupón válido del 10%
        vm.onCuponInputChanged("MODA10")
        vm.aplicarCupon()

        val stateConCupon = vm.uiState.value
        assertEquals(1, stateConCupon.cuponesAplicados.size)
        assertEquals(10.0, stateConCupon.descuentoTotal, 0.01)
        assertEquals(100.0 - 10.0 + 8.0, stateConCupon.total, 0.01)

        // Intentar aplicar un segundo cupón no combinable
        vm.onCuponInputChanged("BIENVENIDA")
        vm.aplicarCupon()

        val stateSegundo = vm.uiState.value
        assertNotNull(stateSegundo.mensajeErrorCupon)
        assertTrue(stateSegundo.mensajeErrorCupon!!.contains("combinar", ignoreCase = true))
    }

    @Test
    fun `CA-06 item agotado o con precio modificado notifica alerta y recalcula total`() = runTest {
        // La prenda costaba 120 al agregar, pero ahora vale 90.0, y otra se agotó
        val p1 = prendaBase(codigo = "BL-1001", precio = 90.0)
        val pAgotada = prendaBase(codigo = "BL-1002", precio = 50.0)

        fakeCatalogoRepo.prendasFlow.value = listOf(p1, pAgotada)
        fakeStockRepo.stocksFlow.value = mapOf(
            "BL-1001_M" to Stock("BL-1001_M", total = 5),
            "BL-1002_S" to Stock("BL-1002_S", total = 0), // Agotado
        )

        fakeCartRepo.itemsFlow.value = listOf(
            CartItem(
                varianteId = "BL-1001_M",
                prendaId = "BL-1001",
                talla = "M",
                colorNombre = "Rojo",
                colorHex = "#FF0000",
                cantidad = 1,
                precioAlAgregar = 120.0, // Cambio de precio
            ),
            CartItem(
                varianteId = "BL-1002_S",
                prendaId = "BL-1002",
                talla = "S",
                colorNombre = "Blanco",
                colorHex = "#FFFFFF",
                cantidad = 1,
                precioAlAgregar = 50.0,
            ),
        )

        val vm = crearViewModel()
        val state = vm.uiState.value

        // Debe alertar sobre el cambio de precio y sobre el agotado
        assertTrue(state.alertas.any { it.contains("cambió de S/ 120", ignoreCase = true) })
        assertTrue(state.alertas.any { it.contains("agotado", ignoreCase = true) })

        // El ítem agotado no debe sumar al total
        assertEquals(90.0, state.subtotal, 0.01)
        assertEquals(90.0 + 8.0, state.total, 0.01)
    }

    private fun prendaBase(codigo: String, precio: Double) = Prenda(
        codigo = codigo,
        nombre = "Prenda $codigo",
        descripcion = "Descripción",
        marca = "ModaViva",
        categoria = "blusas",
        subcategoria = "manga-larga",
        genero = "mujer",
        precio = precio,
        precioPromo = null,
        promoHasta = null,
        color = PrendaColor("azul", "Azul", "#0000FF"),
        tallas = listOf("S", "M", "L"),
        medidas = MedidasPrenda(emptyMap()),
        fotos = listOf("https://example.com/foto.jpg"),
        stockTotal = 10,
        creadaEn = Instant.now(),
        actualizadaEn = Instant.now(),
    )

    // --- Fakes para pruebas ---

    private class FakeCartRepository : CartRepository {
        val itemsFlow = MutableStateFlow<List<CartItem>>(emptyList())
        override val items: Flow<List<CartItem>> = itemsFlow
        override val totalItems: Flow<Int> = MutableStateFlow(0)
        override suspend fun agregarAlCarrito(prendaId: String, talla: String, colorNombre: String, colorHex: String, cantidad: Int) {}
        override suspend fun actualizarCantidad(varianteId: String, nuevaCantidad: Int) {}
        override suspend fun eliminarItem(varianteId: String) {}
        override suspend fun vaciar() {}
    }

    private class FakeCatalogoRepository : CatalogoRepository {
        val prendasFlow = MutableStateFlow<List<Prenda>>(emptyList())
        override val prendas: Flow<List<Prenda>> = prendasFlow
        override val categorias: Flow<List<pe.modaviva.app.domain.model.Categoria>> = flowOf(emptyList())
        override val portada: Flow<pe.modaviva.app.domain.model.Portada?> = flowOf(null)
        override suspend fun obtenerPrenda(codigo: String): Prenda? = prendasFlow.value.firstOrNull { it.codigo == codigo }
        override suspend fun refrescar() {}
    }

    private class FakeStockRepository : StockRepository {
        val stocksFlow = MutableStateFlow<Map<String, Stock>>(emptyMap())
        override fun observeStocks(): Flow<Map<String, Stock>> = stocksFlow
        override suspend fun obtenerStock(varianteId: String): Stock? = stocksFlow.value[varianteId]
        override suspend fun refrescarStock() {}
    }

    private class FakeBackendRepository : BackendRepository {
        override suspend fun ping(): Result<BackendPing> = Result.success(BackendPing("pong", "2026-10-15T12:00:00Z"))

        override suspend fun calcularPedido(solicitud: SolicitudCalculoPedido): Result<ResultadoCalculoPedido> {
            // Forzar fallback en unit test o retornar resultado directo
            return Result.failure(Exception("Prueba unitaria usando fallback local"))
        }

        override suspend fun obtenerTarifasEnvio(): Result<List<TarifaEnvio>> = Result.success(
            listOf(
                TarifaEnvio("miraflores", "Miraflores", true, 8.0, 2),
                TarifaEnvio("san-isidro", "San Isidro", true, 8.0, 2),
                TarifaEnvio("surco", "Santiago de Surco", true, 10.0, 3),
            )
        )
    }
}
