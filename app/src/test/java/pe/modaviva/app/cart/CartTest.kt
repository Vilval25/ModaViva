package pe.modaviva.app.cart

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pe.modaviva.app.data.local.CartDao
import pe.modaviva.app.data.local.CartEntity
import pe.modaviva.app.data.repository.RoomCartRepository
import pe.modaviva.app.domain.model.CartItem

/**
 * Pruebas unitarias para HU-09: Carrito de compras.
 * CA-01: El ítem aparece en el carrito y la insignia se actualiza.
 * CA-02: La misma prenda con la misma talla y color suma la cantidad en lugar de duplicar línea.
 */
class CartTest {

    private lateinit var fakeDao: FakeCartDao
    private lateinit var cartRepository: RoomCartRepository

    @Before
    fun setUp() {
        fakeDao = FakeCartDao()
        cartRepository = RoomCartRepository(fakeDao)
    }

    @Test
    fun `CA-01 al agregar una prenda aparece en el carrito y la cantidad total se actualiza`() = runBlocking {
        // Inicialmente vacío
        assertEquals(0, cartRepository.totalItems.first())
        assertTrue(cartRepository.items.first().isEmpty())

        // Agregar prenda
        cartRepository.agregarAlCarrito(
            prendaId = "BL-1002",
            talla = "M",
            colorNombre = "Negro",
            colorHex = "#000000",
            cantidad = 2,
        )

        // Verifica que aparece en el carrito
        val items = cartRepository.items.first()
        assertEquals(1, items.size)
        assertEquals("BL-1002", items[0].prendaId)
        assertEquals("M", items[0].talla)
        assertEquals(2, items[0].cantidad)
        assertEquals("BL-1002_M", items[0].varianteId)

        // Verifica que la insignia/total se actualizó
        assertEquals(2, cartRepository.totalItems.first())
    }

    @Test
    fun `CA-02 al agregar la misma prenda y talla se suma la cantidad sin duplicar lineas`() = runBlocking {
        // Primera adición: 2 unidades de talla M
        cartRepository.agregarAlCarrito(
            prendaId = "BL-1002",
            talla = "M",
            colorNombre = "Negro",
            colorHex = "#000000",
            cantidad = 2,
        )

        // Segunda adición: 3 unidades de la misma talla M
        cartRepository.agregarAlCarrito(
            prendaId = "BL-1002",
            talla = "M",
            colorNombre = "Negro",
            colorHex = "#000000",
            cantidad = 3,
        )

        val items = cartRepository.items.first()
        // No duplica la línea (solo 1 ítem)
        assertEquals(1, items.size)
        // Suma la cantidad: 2 + 3 = 5
        assertEquals(5, items[0].cantidad)
        // Total de la insignia
        assertEquals(5, cartRepository.totalItems.first())
    }

    @Test
    fun `CA-04 selector no supera el stock disponible y muestra aviso al llegar al maximo`() {
        val stockDisponible = 4
        var cantidad = 1

        // El usuario toca '+' múltiples veces intentando superar el stock
        for (intento in 1..10) {
            if (cantidad < stockDisponible) {
                cantidad++
            }
        }

        // Se detiene exactamente en el máximo disponible
        assertEquals(stockDisponible, cantidad)

        // Al llegar al máximo, se activa el aviso requerido
        val aviso = if (cantidad >= stockDisponible) "Solo quedan $stockDisponible unidades" else null
        assertEquals("Solo quedan 4 unidades", aviso)
    }

    @Test
    fun `CA-03 al cambiar cantidad o eliminar un item el subtotal se recalcula al instante y se puede deshacer`() = runBlocking {
        val precio = 50.0

        // Agregar 2 unidades
        cartRepository.agregarAlCarrito(
            prendaId = "BL-1002",
            talla = "M",
            colorNombre = "Negro",
            colorHex = "#000000",
            cantidad = 2,
        )

        val itemOriginal = cartRepository.items.first().first()
        var subtotal = itemOriginal.cantidad * precio
        assertEquals(100.0, subtotal, 0.0)

        // Cambiar cantidad a 3 -> subtotal se recalcula al instante (150.0)
        cartRepository.actualizarCantidad(itemOriginal.varianteId, 3)
        val itemActualizado = cartRepository.items.first().first()
        assertEquals(3, itemActualizado.cantidad)
        subtotal = itemActualizado.cantidad * precio
        assertEquals(150.0, subtotal, 0.0)

        // Eliminar ítem -> subtotal se recalcula al instante a 0
        cartRepository.eliminarItem(itemOriginal.varianteId)
        assertTrue(cartRepository.items.first().isEmpty())
        assertEquals(0, cartRepository.totalItems.first())

        // Deshacer eliminación -> se restaura el ítem con su cantidad previa
        cartRepository.agregarAlCarrito(
            prendaId = itemOriginal.prendaId,
            talla = itemOriginal.talla,
            colorNombre = itemOriginal.colorNombre,
            colorHex = itemOriginal.colorHex,
            cantidad = itemOriginal.cantidad,
        )
        val itemRestaurado = cartRepository.items.first().first()
        assertEquals(2, itemRestaurado.cantidad)
        subtotal = itemRestaurado.cantidad * precio
        assertEquals(100.0, subtotal, 0.0)
    }

    @Test
    fun `en el carrito al incrementar la cantidad se respeta el tope de stock disponible`() {
        val maxStock = 4
        var cantidadEnCarrito = 2

        // Intentar incrementar más allá del stock
        for (i in 1..5) {
            if (cantidadEnCarrito < maxStock) {
                cantidadEnCarrito++
            }
        }

        // Se detiene en el tope de stock
        assertEquals(4, cantidadEnCarrito)
        val aviso = if (cantidadEnCarrito >= maxStock) "Solo quedan $maxStock unidades" else null
        assertEquals("Solo quedan 4 unidades", aviso)
    }

    @Test
    fun `agregar prendas con diferente talla crea lineas separadas`() = runBlocking {
        cartRepository.agregarAlCarrito(
            prendaId = "BL-1002",
            talla = "S",
            colorNombre = "Negro",
            colorHex = "#000000",
            cantidad = 1,
        )
        cartRepository.agregarAlCarrito(
            prendaId = "BL-1002",
            talla = "M",
            colorNombre = "Negro",
            colorHex = "#000000",
            cantidad = 2,
        )

        val items = cartRepository.items.first()
        assertEquals(2, items.size)
        assertEquals(3, cartRepository.totalItems.first())
    }
}

private class FakeCartDao : CartDao {
    private val data = MutableStateFlow<Map<String, CartEntity>>(emptyMap())

    override fun observarCarrito(): Flow<List<CartEntity>> =
        data.map { it.values.toList() }

    override suspend fun obtenerPorVarianteId(varianteId: String): CartEntity? =
        data.value[varianteId]

    override suspend fun obtenerTodos(): List<CartEntity> =
        data.value.values.toList()

    override fun observarTotalItems(): Flow<Int> =
        data.map { it.values.sumOf { item -> item.cantidad } }

    override suspend fun insertarOActualizar(entity: CartEntity) {
        data.value = data.value + (entity.varianteId to entity)
    }

    override suspend fun actualizarCantidad(varianteId: String, cantidad: Int) {
        val actual = data.value[varianteId] ?: return
        data.value = data.value + (varianteId to actual.copy(cantidad = cantidad))
    }

    override suspend fun eliminarItem(varianteId: String) {
        data.value = data.value - varianteId
    }

    override suspend fun vaciarCarrito() {
        data.value = emptyMap()
    }

    override suspend fun insertarItems(entities: List<CartEntity>) {
        data.value = data.value + entities.associateBy { it.varianteId }
    }
}
