package pe.modaviva.app.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.domain.model.RecomendacionTalla
import pe.modaviva.app.domain.model.ResultadoPruebaVirtual
import pe.modaviva.app.domain.model.UserProfile
import pe.modaviva.app.domain.repository.CartRepository
import pe.modaviva.app.domain.repository.CatalogoRepository
import pe.modaviva.app.domain.repository.FittingConfigRepository
import pe.modaviva.app.domain.repository.SessionRepository
import pe.modaviva.app.domain.repository.TryOnRepository
import pe.modaviva.app.domain.usecase.RecommendSizeUseCase
import java.time.Instant
import javax.inject.Inject

sealed interface TryOnUiState {
    data object Idle : TryOnUiState
    data object Loading : TryOnUiState
    data class Success(val resultado: ResultadoPruebaVirtual) : TryOnUiState
    data class Error(val mensaje: String) : TryOnUiState
}

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    catalogoRepository: CatalogoRepository,
    private val cartRepository: CartRepository,
    sessionRepository: SessionRepository,
    private val fittingConfigRepository: FittingConfigRepository,
    private val recommendSizeUseCase: RecommendSizeUseCase,
    private val tryOnRepository: TryOnRepository,
) : ViewModel() {

    val currentUser: StateFlow<UserProfile?> = sessionRepository.currentUser

    val disabledCategories: StateFlow<Set<String>> = fittingConfigRepository.disabledCategories

    private val prendas: Flow<List<Prenda>> = catalogoRepository.prendas

    val cartItemCount: StateFlow<Int> = cartRepository.totalItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _tryOnState = MutableStateFlow<TryOnUiState>(TryOnUiState.Idle)
    val tryOnState: StateFlow<TryOnUiState> = _tryOnState.asStateFlow()

    fun calcularRecomendacionTalla(prenda: Prenda): RecomendacionTalla {
        return recommendSizeUseCase(prenda, currentUser.value)
    }

    fun obtenerPrenda(codigo: String): Flow<Prenda?> {
        return prendas.map { lista ->
            lista.firstOrNull { prenda ->
                prenda.codigo == codigo
            }
        }
    }

    /**
     * HU-16 CA-01 y CA-04: Verifica si la prenda puede probarse en el probador virtual.
     */
    fun isPrendaHabilitadaParaProbador(prenda: Prenda): Boolean {
        return fittingConfigRepository.isPrendaHabilitadaParaProbador(prenda)
    }

    /**
     * Ejecuta la prueba virtual con Genlook (reutiliza caché en backend si ya fue generada).
     */
    fun iniciarPruebaVirtual(
        prendaCodigo: String,
        forzarRegeneracion: Boolean = false,
    ) {
        if (_tryOnState.value is TryOnUiState.Loading) return
        viewModelScope.launch {
            _tryOnState.value = TryOnUiState.Loading
            tryOnRepository.probarPrenda(
                prendaId = prendaCodigo,
                mock = false,
                forzarRegeneracion = forzarRegeneracion,
            ).onSuccess { resultado ->
                _tryOnState.value = TryOnUiState.Success(resultado)
            }.onFailure { error ->
                _tryOnState.value = TryOnUiState.Error(
                    error.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "No se pudo generar la prueba virtual. Inténtalo nuevamente."
                )
            }
        }
    }

    fun cerrarPruebaVirtual() {
        _tryOnState.value = TryOnUiState.Idle
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
