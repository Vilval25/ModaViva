package pe.modaviva.app.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.domain.repository.CatalogoRepository

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    catalogoRepository: CatalogoRepository,
) : ViewModel() {

    private val prendas: Flow<List<Prenda>> = catalogoRepository.prendas

    fun obtenerPrenda(codigo: String): Flow<Prenda?> {
        return prendas.map { lista ->
            lista.firstOrNull { prenda ->
                prenda.codigo == codigo
            }
        }
    }
}