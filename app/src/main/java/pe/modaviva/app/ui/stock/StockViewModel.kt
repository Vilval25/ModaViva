package pe.modaviva.app.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.modaviva.app.domain.model.Stock
import pe.modaviva.app.domain.model.Tienda
import pe.modaviva.app.domain.repository.StockRepository
import pe.modaviva.app.domain.repository.TiendaRepository

@HiltViewModel
class StockViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    tiendaRepository: TiendaRepository,
) : ViewModel() {

    private val _stock = MutableStateFlow<Stock?>(null)
    val stock: StateFlow<Stock?> = _stock.asStateFlow()

    /** Suscripción única al catálogo de tiendas físicas gestionada por el ViewModel. */
    val tiendas: StateFlow<List<Tienda>> = tiendaRepository.observarTiendas()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Suscripción única al mapa de stock en tiempo real gestionada por el ViewModel. */
    val stocksMap: StateFlow<Map<String, Stock>> = stockRepository.observeStocks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap(),
        )

    private var prendaEscuchada: String? = null

    /**
     * HU-05 CA-03 y CA-05: mientras la ficha está abierta, el stock de todas sus tallas llega en
     * tiempo real y se guarda en la copia local, que alimenta [stocksMap].
     */
    fun escucharPrenda(prendaId: String) {
        if (prendaEscuchada == prendaId) return
        prendaEscuchada = prendaId
        viewModelScope.launch {
            stockRepository.observeStocksDePrenda(prendaId).collect { }
        }
    }

    fun observeStock(
        prendaId: String,
        talla: String,
    ) {
        viewModelScope.launch {
            stockRepository
                .observeStock(prendaId, talla)
                .collect { stock ->
                    _stock.value = stock
                }
        }
    }
}