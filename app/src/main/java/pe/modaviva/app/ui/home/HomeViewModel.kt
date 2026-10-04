package pe.modaviva.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.modaviva.app.data.network.MonitorDeConexion
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.domain.repository.CatalogoRepository
import pe.modaviva.app.ui.format.formatearSoles
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val catalogo: CatalogoRepository,
    conexion: MonitorDeConexion,
    private val reloj: Clock,
) : ViewModel() {

    private val reintentos = MutableStateFlow(0)
    private val refrescando = MutableStateFlow(false)

    private val _mensaje = MutableStateFlow<MensajeHome?>(null)
    val mensaje: StateFlow<MensajeHome?> = _mensaje.asStateFlow()

    /**
     * Escucha del catálogo en el servidor. Solo corre mientras Inicio está en
     * pantalla (ver stateIn) y se reinicia al reintentar tras un error.
     */
    private val sincronizacion: Flow<Sincronizacion> = reintentos.flatMapLatest {
        catalogo.sincronizarEnTiempoReal()
            .map { Sincronizacion.AL_DIA }
            .onStart { emit(Sincronizacion.CONECTANDO) }
            .catch { emit(Sincronizacion.ERROR) }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        catalogo.prendas,
        catalogo.ultimaActualizacion,
        conexion.conectado,
        sincronizacion,
        refrescando,
    ) { prendas, ultimaActualizacion, conectado, sincronizacion, refrescando ->
        val ahora = Instant.now(reloj)
        val tarjetas = prendas.map { it.aUi(ahora) }
        HomeUiState(
            contenido = when {
                prendas.isNotEmpty() -> ContenidoHome.CATALOGO
                !conectado -> ContenidoHome.SIN_CONEXION
                sincronizacion == Sincronizacion.ERROR -> ContenidoHome.ERROR
                sincronizacion == Sincronizacion.AL_DIA -> ContenidoHome.VACIO
                else -> ContenidoHome.CARGANDO
            },
            prendas = tarjetas,
            destacados = prendas
                .filter { it.enPromocion(ahora) && !it.agotada }
                .map { it.aUi(ahora) },
            sinConexion = !conectado,
            ultimaActualizacion = ultimaActualizacion,
            refrescando = refrescando,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Pull-to-refresh (HU-04 CA-05). */
    fun refrescar() {
        if (refrescando.value) return
        viewModelScope.launch {
            refrescando.value = true
            catalogo.refrescar()
                .onSuccess { reintentos.value++ }
                .onFailure { _mensaje.value = MensajeHome.NO_SE_PUDO_ACTUALIZAR }
            refrescando.value = false
        }
    }

    fun onMensajeMostrado() {
        _mensaje.value = null
    }

    private fun Prenda.aUi(ahora: Instant) = PrendaUi(
        codigo = codigo,
        nombre = nombre,
        marca = marca,
        foto = fotos.first(),
        precio = formatearSoles(precioVigente(ahora)),
        precioOriginal = if (enPromocion(ahora)) formatearSoles(precio) else null,
        agotada = agotada,
    )

    private enum class Sincronizacion { CONECTANDO, AL_DIA, ERROR }
}
