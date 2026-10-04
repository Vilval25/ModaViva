package pe.modaviva.app.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.modaviva.app.domain.model.Stock
import pe.modaviva.app.domain.repository.StockRepository

@HiltViewModel
class StockViewModel @Inject constructor(
    private val stockRepository: StockRepository,
) : ViewModel() {

    private val _stock = MutableStateFlow<Stock?>(null)
    val stock: StateFlow<Stock?> = _stock.asStateFlow()

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