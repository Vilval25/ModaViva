package pe.modaviva.app.ui.cart

import pe.modaviva.app.domain.model.CartItem

data class CartItemUi(
    val item: CartItem,
    val titulo: String,
    val imagenUrl: String,
    val precioUnitario: Double,
    val subtotal: Double,
    val maxStock: Int = 99,
    val avisoStock: String? = null,
    val cambioDePrecio: Boolean = false,
    val precioAnterior: Double = 0.0,
    val estaAgotado: Boolean = false,
)

data class CartUiState(
    val items: List<CartItemUi> = emptyList(),
    val totalArticulos: Int = 0,
    val subtotalTotal: Double = 0.0,
    val estaCargando: Boolean = true,
)
