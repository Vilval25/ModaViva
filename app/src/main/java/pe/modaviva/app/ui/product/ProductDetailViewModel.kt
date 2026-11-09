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
import pe.modaviva.app.data.location.LocationService
import pe.modaviva.app.data.location.ResultadoUbicacion
import pe.modaviva.app.data.repository.CheckoutSessionRepository
import pe.modaviva.app.domain.model.DestinoEntrega
import pe.modaviva.app.domain.model.EstimacionEntrega
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.domain.model.Stock
import pe.modaviva.app.domain.model.TarifaEnvio
import pe.modaviva.app.domain.model.Tienda
import pe.modaviva.app.domain.model.TipoDestino
import pe.modaviva.app.domain.model.UserProfile
import pe.modaviva.app.domain.repository.BackendRepository
import pe.modaviva.app.domain.repository.CartRepository
import pe.modaviva.app.domain.repository.CatalogoRepository
import pe.modaviva.app.domain.repository.DireccionRepository
import pe.modaviva.app.domain.repository.FittingConfigRepository
import pe.modaviva.app.domain.repository.SessionRepository
import pe.modaviva.app.domain.repository.StockRepository
import pe.modaviva.app.domain.repository.TiendaRepository
import pe.modaviva.app.domain.model.DireccionEntrega
import pe.modaviva.app.domain.usecase.CalculateDeliveryEstimateUseCase
import pe.modaviva.app.domain.usecase.CalculateNearestStoreUseCase
import pe.modaviva.app.domain.usecase.ResultadoTiendaCercana
import pe.modaviva.app.domain.model.RecomendacionTalla
import pe.modaviva.app.domain.usecase.RecommendSizeUseCase
import java.time.Instant
import java.text.Normalizer
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    catalogoRepository: CatalogoRepository,
    private val cartRepository: CartRepository,
    sessionRepository: SessionRepository,
    private val fittingConfigRepository: FittingConfigRepository,
    val locationService: LocationService,
    private val backendRepository: BackendRepository,
    private val calculateDeliveryEstimateUseCase: CalculateDeliveryEstimateUseCase,
    private val stockRepository: StockRepository,
    private val tiendaRepository: TiendaRepository,
    private val calculateNearestStoreUseCase: CalculateNearestStoreUseCase,
    private val checkoutSessionRepository: CheckoutSessionRepository,
    private val direccionRepository: DireccionRepository,
    private val recommendSizeUseCase: RecommendSizeUseCase,
) : ViewModel() {

    val currentUser: StateFlow<UserProfile?> = sessionRepository.currentUser

    val disabledCategories: StateFlow<Set<String>> = fittingConfigRepository.disabledCategories

    private val prendas: Flow<List<Prenda>> = catalogoRepository.prendas

    val cartItemCount: StateFlow<Int> = cartRepository.totalItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val direccionesGuardadas: StateFlow<List<DireccionEntrega>> = direccionRepository.observarDirecciones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _destinoEntrega = MutableStateFlow<DestinoEntrega?>(null)
    val destinoEntrega: StateFlow<DestinoEntrega?> = _destinoEntrega.asStateFlow()

    private val _ubicacionEstado = MutableStateFlow<ResultadoUbicacion?>(null)
    val ubicacionEstado: StateFlow<ResultadoUbicacion?> = _ubicacionEstado.asStateFlow()

    private val _estimacionEntrega = MutableStateFlow<EstimacionEntrega?>(null)
    val estimacionEntrega: StateFlow<EstimacionEntrega?> = _estimacionEntrega.asStateFlow()

    private val _tarifas = MutableStateFlow<List<TarifaEnvio>>(emptyList())
    val tarifasDisponibles: StateFlow<List<TarifaEnvio>> = _tarifas.asStateFlow()

    private val _cargandoTarifas = MutableStateFlow(false)
    val cargandoTarifas: StateFlow<Boolean> = _cargandoTarifas.asStateFlow()

    private val _errorTarifas = MutableStateFlow<String?>(null)
    val errorTarifas: StateFlow<String?> = _errorTarifas.asStateFlow()

    private val _tallaSeleccionada = MutableStateFlow<String?>(null)
    val tallaSeleccionada: StateFlow<String?> = _tallaSeleccionada.asStateFlow()

    private val _stockActual = MutableStateFlow<Stock?>(null)
    val stockActual: StateFlow<Stock?> = _stockActual.asStateFlow()

    val tiendas: StateFlow<List<Tienda>> = tiendaRepository.observarTiendas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _resultadoTiendaCercana = MutableStateFlow<ResultadoTiendaCercana>(ResultadoTiendaCercana())
    val resultadoTiendaCercana: StateFlow<ResultadoTiendaCercana> = _resultadoTiendaCercana.asStateFlow()

    init {
        cargarTarifas()

        val destinoPrevio = checkoutSessionRepository.destinoActual.value
        if (destinoPrevio != null && destinoPrevio.estaCompleto) {
            _destinoEntrega.value = destinoPrevio
            actualizarDestino(destinoPrevio)
        }

        viewModelScope.launch {
            tiendaRepository.observarTiendas().collect { _ ->
                recalcularTiendaCercana()
            }
        }
    }

    fun calcularRecomendacionTalla(prenda: Prenda): RecomendacionTalla {
        return recommendSizeUseCase(prenda, currentUser.value)
    }

    fun cargarTarifas() {
        viewModelScope.launch {
            _cargandoTarifas.value = true
            _errorTarifas.value = null
            val resultado = backendRepository.obtenerTarifasEnvio()
            resultado.onSuccess { lista ->
                val unicas = lista.distinctBy { it.id.lowercase().trim() }
                _tarifas.value = unicas
                _cargandoTarifas.value = false
            }.onFailure { e ->
                _cargandoTarifas.value = false
                _errorTarifas.value = e.localizedMessage ?: "Error al obtener las tarifas de envío"
            }
        }
    }

    fun seleccionarTalla(prendaId: String, talla: String?) {
        _tallaSeleccionada.value = talla
        if (talla == null) {
            _stockActual.value = null
            _resultadoTiendaCercana.value = ResultadoTiendaCercana()
            return
        }
        viewModelScope.launch {
            stockRepository.observeStock(prendaId, talla).collect { stock ->
                _stockActual.value = stock
                recalcularTiendaCercana()
            }
        }
    }

    fun actualizarDestino(destino: DestinoEntrega) {
        _destinoEntrega.value = destino
        checkoutSessionRepository.actualizarDestino(destino)
        viewModelScope.launch {
            var tarifas = _tarifas.value
            if (tarifas.isEmpty()) {
                tarifas = backendRepository.obtenerTarifasEnvio().getOrDefault(emptyList())
                _tarifas.value = tarifas
            }
            val estimacion = calculateDeliveryEstimateUseCase(destino, tarifas)
            _estimacionEntrega.value = estimacion
            recalcularTiendaCercana()
        }
    }

    private fun recalcularTiendaCercana() {
        val destino = _destinoEntrega.value
        val stock = _stockActual.value
        val listaTiendas = tiendas.value
        _resultadoTiendaCercana.value = calculateNearestStoreUseCase(destino, stock, listaTiendas)
    }

    fun obtenerUbicacionGps() {
        viewModelScope.launch {
            _ubicacionEstado.value = ResultadoUbicacion.Cargando
            val resultado = locationService.obtenerUbicacionActual()
            _ubicacionEstado.value = resultado
            if (resultado is ResultadoUbicacion.Exito) {
                val destinoActual = _destinoEntrega.value ?: DestinoEntrega()
                val nuevoDestino = destinoActual.copy(
                    tipo = TipoDestino.UBICACION_ACTUAL_GPS,
                    lat = resultado.lat,
                    lng = resultado.lng,
                )
                actualizarDestino(nuevoDestino)
            } else if (resultado is ResultadoUbicacion.GpsDesactivado || resultado is ResultadoUbicacion.PermisoDenegado || resultado is ResultadoUbicacion.NoDisponible) {
                _estimacionEntrega.value = EstimacionEntrega(
                    tieneCobertura = false,
                    tarifaRegistrada = false,
                    costoEnvio = 0.0,
                    plazoMinDias = 0,
                    plazoMaxDias = 0,
                    mensajeInformativo = "No se pudo obtener la ubicación GPS. Selecciona tu distrito manualmente.",
                )
            }
        }
    }

    private fun normalizar(texto: String): String {
        val sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return sinAcentos
            .lowercase()
            .trim()
            .replace(" ", "-")
            .replace("_", "-")
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
