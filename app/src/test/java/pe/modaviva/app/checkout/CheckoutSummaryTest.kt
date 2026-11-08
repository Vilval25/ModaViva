package pe.modaviva.app.checkout

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pe.modaviva.app.domain.model.BackendPing
import pe.modaviva.app.domain.model.CartItem
import pe.modaviva.app.data.repository.CheckoutSessionRepository
import pe.modaviva.app.domain.model.ColorPrenda
import pe.modaviva.app.domain.model.DestinoEntrega
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.domain.model.ResultadoCalculoPedido
import pe.modaviva.app.domain.model.SolicitudCalculoPedido
import pe.modaviva.app.domain.model.Stock
import pe.modaviva.app.domain.model.TarifaEnvio
import pe.modaviva.app.domain.model.TipoDestino
import pe.modaviva.app.domain.repository.BackendRepository
import pe.modaviva.app.domain.repository.CartRepository
import pe.modaviva.app.domain.repository.CatalogoRepository
import pe.modaviva.app.domain.repository.StockRepository
import pe.modaviva.app.ui.checkout.CheckoutSummaryViewModel
import pe.modaviva.app.ui.checkout.TipoEntregaUi
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

import pe.modaviva.app.domain.model.Tienda
import pe.modaviva.app.domain.repository.TiendaRepository
import pe.modaviva.app.domain.usecase.CalculateNearestStoreForCartUseCase


/**
 * Pruebas unitarias para HU-19 — Cálculo del pedido antes de pagar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutSummaryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val relojFijo = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneId.of("UTC"))

    private lateinit var fakeCartRepo: FakeCartRepository
    private lateinit var fakeCatalogoRepo: FakeCatalogoRepository
    private lateinit var fakeStockRepo: FakeStockRepository
    private lateinit var fakeBackendRepo: FakeBackendRepository

    private val sessionRepo = CheckoutSessionRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeCartRepo = FakeCartRepository()
        fakeCatalogoRepo = FakeCatalogoRepository()
        fakeStockRepo = FakeStockRepository()
        fakeBackendRepo = FakeBackendRepository()
        sessionRepo.limpiarDestino()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(): CheckoutSummaryViewModel {
        return CheckoutSummaryViewModel(
            cartRepository = fakeCartRepo,
            catalogoRepository = fakeCatalogoRepo,
            stockRepository = fakeStockRepo,
            backendRepository = fakeBackendRepo,
            checkoutSessionRepository = sessionRepo,
            tiendaRepository = FakeTiendaRepository(),
            calculateNearestStoreForCartUseCase = CalculateNearestStoreForCartUseCase(),
            reloj = relojFijo,
        )
    }

    @Test
    fun `CA-01 y CA-02 resumen calcula subtotal, envio y total igual a subtotal menos descuento mas envio`() = runBlocking {
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
        val totalEsperado = state.subtotal - state.descuentoTotal + state.costoEnvio
        assertEquals(totalEsperado, state.total, 0.01)
        assertEquals(208.0, state.total, 0.01)
    }

    @Test
    fun `CA-03 al seleccionar recojo en tienda el envio es 0 soles`() = runBlocking {
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
    fun `CA-04 cupon inexistente o con monto minimo no alcanzado muestra motivo especifico`() = runBlocking {
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

        vm.onCuponInputChanged("CUPON_FALSO_123")
        vm.aplicarCupon()
        assertNotNull(vm.uiState.value.mensajeErrorCupon)
        assertTrue(vm.uiState.value.mensajeErrorCupon!!.contains("no existe", ignoreCase = true))

        vm.onCuponInputChanged("MODA10")
        vm.aplicarCupon()
        assertNotNull(vm.uiState.value.mensajeErrorCupon)
        assertTrue(vm.uiState.value.mensajeErrorCupon!!.contains("mínima", ignoreCase = true))
        assertEquals(0.0, vm.uiState.value.descuentoTotal, 0.01)
    }

    @Test
    fun `CA-05 cupon valido aplica descuento y se rechaza un segundo cupon no combinable`() = runBlocking {
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

        vm.onCuponInputChanged("MODA10")
        vm.aplicarCupon()

        val stateConCupon = vm.uiState.value
        assertEquals(1, stateConCupon.cuponesAplicados.size)
        assertEquals(10.0, stateConCupon.descuentoTotal, 0.01)
        assertEquals(100.0 - 10.0 + 8.0, stateConCupon.total, 0.01)

        vm.onCuponInputChanged("BIENVENIDA")
        vm.aplicarCupon()

        val stateSegundo = vm.uiState.value
        assertNotNull(stateSegundo.mensajeErrorCupon)
        assertTrue(stateSegundo.mensajeErrorCupon!!.contains("combinar", ignoreCase = true))
    }

    @Test
    fun `CA-06 item agotado o con precio modificado notifica alerta y recalcula total`() = runBlocking {
        val p1 = prendaBase(codigo = "BL-1001", precio = 90.0)
        val pAgotada = prendaBase(codigo = "BL-1002", precio = 50.0)

        fakeCatalogoRepo.prendasFlow.value = listOf(p1, pAgotada)
        fakeStockRepo.stocksFlow.value = mapOf(
            "BL-1001_M" to Stock("BL-1001_M", total = 5),
            "BL-1002_S" to Stock("BL-1002_S", total = 0),
        )

        fakeCartRepo.itemsFlow.value = listOf(
            CartItem(
                varianteId = "BL-1001_M",
                prendaId = "BL-1001",
                talla = "M",
                colorNombre = "Rojo",
                colorHex = "#FF0000",
                cantidad = 1,
                precioAlAgregar = 120.0,
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

        assertTrue(state.alertas.any { it.contains("cambió de S/ 120", ignoreCase = true) })
        assertTrue(state.alertas.any { it.contains("agotado", ignoreCase = true) })

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
        color = ColorPrenda("azul", "Azul", "#0000FF"),
        tallas = listOf("S", "M", "L"),
        medidas = emptyMap(),
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
        override suspend fun agregarAlCarrito(prendaId: String, talla: String, colorNombre: String, colorHex: String, cantidad: Int, precioAlAgregar: Double) {}
        override suspend fun actualizarCantidad(varianteId: String, nuevaCantidad: Int) {}
        override suspend fun eliminarItem(varianteId: String) {}
        override suspend fun vaciarCarrito() {}
        override suspend fun sincronizarConCuenta(uid: String) {}
    }

    private class FakeCatalogoRepository : CatalogoRepository {
        val prendasFlow = MutableStateFlow<List<Prenda>>(emptyList())
        override val prendas: Flow<List<Prenda>> = prendasFlow
        override val ultimaActualizacion: Flow<Instant?> = flowOf(null)
        override fun sincronizarEnTiempoReal(): Flow<Unit> = flowOf(Unit)
        override suspend fun refrescar(): Result<Unit> = Result.success(Unit)
    }

    private class FakeStockRepository : StockRepository {
        val stocksFlow = MutableStateFlow<Map<String, Stock>>(emptyMap())
        override fun observeStock(prendaId: String, talla: String): Flow<Stock?> = flowOf(stocksFlow.value["${prendaId}_${talla}"])
        override fun observeStocks(): Flow<Map<String, Stock>> = stocksFlow
    }

    private class FakeBackendRepository : BackendRepository {
        override suspend fun ping(): Result<BackendPing> = Result.success(BackendPing("pong", "2026-10-15T12:00:00Z"))

        override suspend fun calcularPedido(solicitud: SolicitudCalculoPedido): Result<ResultadoCalculoPedido> {
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

    private class FakeTiendaRepository : TiendaRepository {
        override fun observarTiendas(): Flow<List<Tienda>> = flowOf(emptyList())
    }

    @Test
    fun `checkout recupera el destino seleccionado previamente en la sesion`() = runBlocking {
        sessionRepo.actualizarDestino(
            DestinoEntrega(
                tipo = TipoDestino.OTRA_DIRECCION,
                distrito = "San Isidro",
                distritoSlug = "san-isidro",
            )
        )

        val vm = crearViewModel()
        kotlinx.coroutines.delay(100L)

        assertEquals("San Isidro", vm.uiState.value.distritoSeleccionado?.distrito)
    }
}
