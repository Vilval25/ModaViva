package pe.modaviva.app.ui.product

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import pe.modaviva.app.worker.TryOnWorker
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import pe.modaviva.app.domain.model.DestinoEntrega
import pe.modaviva.app.domain.model.aDestinoEntrega
import pe.modaviva.app.domain.repository.BackendRepository
import pe.modaviva.app.domain.repository.DireccionRepository
import pe.modaviva.app.domain.usecase.CalculateDeliveryEstimateUseCase
import pe.modaviva.app.ui.format.formatearSoles
import pe.modaviva.app.ui.product.components.EnvioEstimado
import java.util.UUID
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
import pe.modaviva.app.domain.model.UsoProbadorHoy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import pe.modaviva.app.domain.usecase.RecommendSizeUseCase
import java.time.Instant
import javax.inject.Inject

sealed interface TryOnUiState {
    data object Idle : TryOnUiState
    data object Loading : TryOnUiState
    /** [accionEnCurso]: se está guardando o descartando el resultado (HU-12 CA-06). */
    data class Success(
        val resultado: ResultadoPruebaVirtual,
        val accionEnCurso: Boolean = false,
        /** HU-12 CA-03: segundos desde que el cliente tocó 'Probarme' hasta ver el resultado. */
        val segundosEspera: Double? = null,
    ) : TryOnUiState
    data class Error(val mensaje: String) : TryOnUiState
}

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
    catalogoRepository: CatalogoRepository,
    private val cartRepository: CartRepository,
    sessionRepository: SessionRepository,
    private val fittingConfigRepository: FittingConfigRepository,
    private val recommendSizeUseCase: RecommendSizeUseCase,
    private val tryOnRepository: TryOnRepository,
    private val direccionRepository: DireccionRepository,
    private val backendRepository: BackendRepository,
    private val calcularEntrega: CalculateDeliveryEstimateUseCase,
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context)
    private val codigoPrenda: String? = savedStateHandle["codigo"]

    /** Trabajo de generación que esta pantalla está siguiendo (HU-12 CA-04). */
    private var trabajoSeguido: UUID? = null
    private var inicioGeneracion: Long? = null

    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 1)
    /** Mensajes breves para el snackbar (guardada, descartada…). */
    val mensajes: SharedFlow<String> = _mensajes.asSharedFlow()

    val currentUser: StateFlow<UserProfile?> = sessionRepository.currentUser

    val disabledCategories: StateFlow<Set<String>> = fittingConfigRepository.disabledCategories

    private val prendas: Flow<List<Prenda>> = catalogoRepository.prendas

    val cartItemCount: StateFlow<Int> = cartRepository.totalItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _tryOnState = MutableStateFlow<TryOnUiState>(TryOnUiState.Idle)
    val tryOnState: StateFlow<TryOnUiState> = _tryOnState.asStateFlow()

    /** HU-12 CA-07: pruebas usadas hoy; se vuelve a leer al iniciar o cerrar sesión. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val usoProbadorHoy: StateFlow<UsoProbadorHoy?> = sessionRepository.currentUser
        .map { it?.uid?.ifBlank { null } }
        .distinctUntilChanged()
        .flatMapLatest { uid -> if (uid == null) flowOf(null) else tryOnRepository.observarUsoHoy() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * HU-07 CA-05: plazo de envío a domicilio. Con dirección principal registrada se calcula para
     * su distrito; si no, se muestra el rango general de Lima según las tarifas.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val envioEstimado: StateFlow<EnvioEstimado?> = combine(
        sessionRepository.currentUser
            .map { it?.uid?.ifBlank { null } }
            .distinctUntilChanged()
            .flatMapLatest { uid -> if (uid == null) flowOf(emptyList()) else direccionRepository.observarDirecciones() },
        flow { emit(backendRepository.obtenerTarifasEnvio().getOrDefault(emptyList())) },
    ) { direcciones, tarifas ->
        if (tarifas.isEmpty()) return@combine null
        val principal = direcciones.firstOrNull { it.esPrincipal } ?: direcciones.firstOrNull()
        if (principal != null) {
            val est = calcularEntrega(principal.aDestinoEntrega(), tarifas)
            if (est.tieneCobertura) {
                EnvioEstimado(
                    titulo = "Envío a ${principal.distrito}: ${est.rangoDiasTexto}",
                    detalle = "A tu dirección principal. Costo de envío: ${formatearSoles(est.costoEnvio)}.",
                )
            } else {
                EnvioEstimado(
                    titulo = "Envío a domicilio: ${est.rangoDiasTexto}",
                    detalle = est.mensajeInformativo ?: "Puedes elegir otra dirección o recojo en tienda al pagar.",
                )
            }
        } else {
            val plazos = tarifas.filter { it.cobertura }.map {
                calcularEntrega(DestinoEntrega(distrito = it.distrito, distritoSlug = it.id), tarifas)
            }.filter { it.tieneCobertura }
            if (plazos.isEmpty()) {
                null
            } else {
                EnvioEstimado(
                    titulo = "Envío a domicilio: ${plazos.minOf { it.plazoMinDias }} a ${plazos.maxOf { it.plazoMaxDias }} días hábiles",
                    detalle = "Según tu distrito en Lima Metropolitana. Registra una dirección para ver tu plazo exacto.",
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        codigoPrenda?.let { codigo ->
            seguirGeneracion(codigo)
            // HU-12 CA-04: al llegar desde el aviso 'Tu prueba virtual está lista' se abre el resultado.
            if (savedStateHandle.get<Boolean>("abrirPrueba") == true) abrirResultadoGuardado(codigo)
        }
    }

    /**
     * Sigue el trabajo de generación de esta prenda. Si al entrar ya hay uno en curso (el cliente
     * salió y volvió), se muestra el progreso y luego el resultado.
     */
    private fun seguirGeneracion(codigo: String) {
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(TryOnWorker.nombreUnico(codigo)).collect { infos ->
                if (trabajoSeguido == null) {
                    infos.firstOrNull { !it.state.isFinished }?.let { trabajoSeguido = it.id }
                }
                val info = infos.firstOrNull { it.id == trabajoSeguido } ?: return@collect
                _tryOnState.value = when (info.state) {
                    WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED -> TryOnUiState.Loading
                    WorkInfo.State.SUCCEEDED -> TryOnUiState.Success(
                        resultado = TryOnWorker.resultado(info),
                        segundosEspera = inicioGeneracion?.let { (System.currentTimeMillis() - it) / 1000.0 },
                    )
                    WorkInfo.State.FAILED -> TryOnUiState.Error(TryOnWorker.error(info))
                    WorkInfo.State.CANCELLED -> TryOnUiState.Idle
                }
                if (info.state.isFinished) {
                    trabajoSeguido = null
                    inicioGeneracion = null
                }
            }
        }
    }

    private fun abrirResultadoGuardado(codigo: String) {
        viewModelScope.launch {
            _tryOnState.value = TryOnUiState.Loading
            tryOnRepository.probarPrenda(prendaId = codigo, soloHistorial = true)
                .onSuccess { _tryOnState.value = TryOnUiState.Success(it) }
                .onFailure { _tryOnState.value = TryOnUiState.Error(it.message ?: "Esta prueba virtual ya no está disponible.") }
        }
    }

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
        prenda: Prenda,
        talla: String,
        forzarRegeneracion: Boolean = false,
    ) {
        if (_tryOnState.value is TryOnUiState.Loading) return
        // HU-12 CA-02 y CA-04: se envían talla y color, y la generación corre en WorkManager para
        // que siga aunque el cliente salga de la pantalla.
        val solicitud = TryOnWorker.solicitud(
            prendaId = prenda.codigo,
            nombre = prenda.nombre,
            talla = talla,
            color = prenda.color.nombre,
            forzar = forzarRegeneracion,
        )
        trabajoSeguido = solicitud.id
        inicioGeneracion = System.currentTimeMillis()
        _tryOnState.value = TryOnUiState.Loading
        workManager.enqueueUniqueWork(TryOnWorker.nombreUnico(prenda.codigo), ExistingWorkPolicy.KEEP, solicitud)
    }

    /** HU-12 CA-06: Guardar deja la imagen en el historial; Descartar la borra. */
    fun gestionarResultado(prendaCodigo: String, guardar: Boolean) {
        val actual = _tryOnState.value as? TryOnUiState.Success ?: return
        if (actual.accionEnCurso) return
        _tryOnState.value = actual.copy(accionEnCurso = true)
        viewModelScope.launch {
            tryOnRepository.gestionarResultado(prendaCodigo, guardar)
                .onSuccess {
                    if (guardar) {
                        _tryOnState.value = actual.copy(resultado = actual.resultado.copy(guardada = true), accionEnCurso = false)
                        _mensajes.tryEmit("Prueba guardada en tu historial.")
                    } else {
                        _tryOnState.value = TryOnUiState.Idle
                        _mensajes.tryEmit("Prueba descartada. La imagen no se guardó.")
                    }
                }
                .onFailure { e ->
                    _tryOnState.value = actual
                    _mensajes.tryEmit(e.message ?: "No se pudo completar la acción.")
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
