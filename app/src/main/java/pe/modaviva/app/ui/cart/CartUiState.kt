package pe.modaviva.app.ui.cart

import pe.modaviva.app.domain.model.CartItem

data class CartItemUi(
    val varianteId: String,
    val prendaId: String,
    val nombre: String,
    val foto: String,
    val talla: String,
    val colorNombre: String,
    val colorHex: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double,
    val maxStock: Int,
    val precioAlAgregar: Double = 0.0,
    val cambioDePrecio: Boolean = false,
    val estaAgotado: Boolean = false,
    val originalItem: CartItem,
)

data class CartUiState(
    val cargando: Boolean = true,
    val items: List<CartItemUi> = emptyList(),
    val totalPrendas: Int = 0,
    val subtotal: Double = 0.0,
    val hayItemsAgotados: Boolean = false,
    val hayCambioDePrecio: Boolean = false,
)
