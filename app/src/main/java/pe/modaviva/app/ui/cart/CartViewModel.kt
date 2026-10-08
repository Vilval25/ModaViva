package pe.modaviva.app.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.modaviva.app.domain.model.CartItem
import pe.modaviva.app.domain.repository.CartRepository
import pe.modaviva.app.domain.repository.CatalogoRepository
import pe.modaviva.app.domain.repository.StockRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    catalogoRepository: CatalogoRepository,
    stockRepository: StockRepository,
    private val reloj: Clock,
) : ViewModel() {

    val uiState: StateFlow<CartUiState> = combine(
        cartRepository.items,
        catalogoRepository.prendas,
        stockRepository.observeStocks(),
    ) { items, prendas, stocks ->
        val ahora = Instant.now(reloj)
        val prendasMap = prendas.associateBy { it.codigo }

        val itemsUi = items.map { item ->
            val prenda = prendasMap[item.prendaId]
            val precioVigente = prenda?.precioVigente(ahora) ?: 0.0
            val maxStock = stocks[item.varianteId]?.total ?: prenda?.stockTotal ?: 0
            val estaAgotado = (maxStock <= 0) || (prenda != null && prenda.agotada)
            val subtotal = if (estaAgotado) 0.0 else precioVigente * item.cantidad
            val cambioDePrecio = item.precioAlAgregar > 0.0 && kotlin.math.abs(item.precioAlAgregar - precioVigente) > 0.01

            CartItemUi(
                varianteId = item.varianteId,
                prendaId = item.prendaId,
                nombre = prenda?.nombre ?: "Prenda ${item.prendaId}",
                foto = prenda?.fotos?.firstOrNull().orEmpty(),
                talla = item.talla,
                colorNombre = item.colorNombre.ifBlank { prenda?.color?.nombre.orEmpty() },
                colorHex = item.colorHex.ifBlank { prenda?.color?.hex.orEmpty() },
                cantidad = item.cantidad,
                precioUnitario = precioVigente,
                precioAlAgregar = if (item.precioAlAgregar > 0.0) item.precioAlAgregar else precioVigente,
                cambioDePrecio = cambioDePrecio,
                subtotal = subtotal,
                maxStock = maxStock,
                estaAgotado = estaAgotado,
                originalItem = item,
            )
        }

        // CA-06: Los ítems agotados no se suman al subtotal y el subtotal usa el precio actual
        val itemsDisponibles = itemsUi.filter { !it.estaAgotado }
        val totalPrendas = itemsDisponibles.sumOf { it.cantidad }
        val subtotalTotal = itemsDisponibles.sumOf { it.subtotal }

        CartUiState(
            cargando = false,
            items = itemsUi,
            totalPrendas = totalPrendas,
            subtotal = subtotalTotal,
            hayItemsAgotados = itemsUi.any { it.estaAgotado },
            hayCambioDePrecio = itemsUi.any { it.cambioDePrecio },
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CartUiState(cargando = true),
    )

    fun modificarCantidad(varianteId: String, nuevaCantidad: Int) {
        viewModelScope.launch {
            cartRepository.actualizarCantidad(varianteId, nuevaCantidad)
        }
    }

    fun eliminarItem(varianteId: String) {
        viewModelScope.launch {
            cartRepository.eliminarItem(varianteId)
        }
    }

    fun restaurarItem(item: CartItem) {
        viewModelScope.launch {
            cartRepository.agregarAlCarrito(
                prendaId = item.prendaId,
                talla = item.talla,
                colorNombre = item.colorNombre,
                colorHex = item.colorHex,
                cantidad = item.cantidad,
            )
        }
    }
}
