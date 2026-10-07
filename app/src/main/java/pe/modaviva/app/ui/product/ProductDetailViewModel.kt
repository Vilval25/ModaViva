package pe.modaviva.app.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.domain.repository.CartRepository
import pe.modaviva.app.domain.repository.CatalogoRepository
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    catalogoRepository: CatalogoRepository,
    private val cartRepository: CartRepository,
) : ViewModel() {

    private val prendas: Flow<List<Prenda>> = catalogoRepository.prendas

    val cartItemCount: StateFlow<Int> = cartRepository.totalItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun obtenerPrenda(codigo: String): Flow<Prenda?> {
        return prendas.map { lista ->
            lista.firstOrNull { prenda ->
                prenda.codigo == codigo
            }
        }
    }

    /**
     * CA-01 y CA-02: Agrega al carrito con talla, color y cantidad elegidos.
     * Si la prenda ya está en el carrito con la misma variante, suma la cantidad.
     */
    fun agregarAlCarrito(
        prenda: Prenda,
        talla: String,
        cantidad: Int,
        onCompletado: () -> Unit = {},
    ) {
        viewModelScope.launch {
            cartRepository.agregarAlCarrito(
                prendaId = prenda.codigo,
                talla = talla,
                colorNombre = prenda.color.nombre,
                colorHex = prenda.color.hex,
                cantidad = cantidad,
                precioAlAgregar = prenda.precioVigente(Instant.now()),
            )
            onCompletado()
        }
    }
}