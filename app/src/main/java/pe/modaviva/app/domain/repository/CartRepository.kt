package pe.modaviva.app.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.modaviva.app.domain.model.CartItem

/**
 * Repositorio del carrito de compras (HU-09).
 */
interface CartRepository {

    /** Lista de ítems en el carrito en tiempo real. */
    val items: Flow<List<CartItem>>

    /** Cantidad total de prendas en el carrito (para la insignia). */
    val totalItems: Flow<Int>

    /**
     * Agrega una prenda con su talla, color y cantidad.
     * Si la misma prenda y talla ya existe, suma la cantidad (CA-02).
     */
    suspend fun agregarAlCarrito(
        prendaId: String,
        talla: String,
        colorNombre: String,
        colorHex: String,
        cantidad: Int,
        precioAlAgregar: Double = 0.0,
    )

    suspend fun actualizarCantidad(varianteId: String, cantidad: Int)

    suspend fun eliminarItem(varianteId: String)

    suspend fun vaciarCarrito()

    /**
     * CA-05 y CA-07: Sincroniza y fusiona el carrito local con el de la cuenta en Firestore.
     */
    suspend fun sincronizarConCuenta(uid: String)
}
