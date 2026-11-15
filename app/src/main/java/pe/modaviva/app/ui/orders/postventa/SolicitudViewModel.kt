package pe.modaviva.app.ui.orders.postventa

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.modaviva.app.domain.model.ItemPedido
import pe.modaviva.app.domain.model.NuevaSolicitud
import pe.modaviva.app.domain.model.Pedido
import pe.modaviva.app.domain.model.SolicitudPostventa
import pe.modaviva.app.domain.model.SolicitudPostventa.Companion.MAX_FOTOS
import pe.modaviva.app.domain.model.SolicitudPostventa.Companion.MIN_DETALLE_OTRO
import pe.modaviva.app.domain.model.SolicitudPostventa.Companion.MOTIVO_OTRO
import pe.modaviva.app.domain.model.TipoSolicitud
import pe.modaviva.app.domain.repository.OrderRepository
import pe.modaviva.app.domain.repository.SolicitudRepository
import javax.inject.Inject

data class SolicitudUiState(
    val cargando: Boolean = true,
    val pedido: Pedido? = null,
    /** Prendas que ya tienen una solicitud en este pedido (no se pueden volver a pedir). */
    val prendasConSolicitud: Set<String> = emptySet(),
    val itemSeleccionado: ItemPedido? = null,
    val tipo: TipoSolicitud? = null,
    val motivo: String? = null,
    val detalleOtro: String = "",
    val fotos: List<Uri> = emptyList(),
    val enviando: Boolean = false,
    val progreso: Float = 0f,
    val error: String? = null,
    val avisoFotos: String? = null,
    val registrada: SolicitudPostventa? = null,
) {
    val faltaDetalleOtro: Boolean
        get() = motivo == MOTIVO_OTRO && detalleOtro.trim().length < MIN_DETALLE_OTRO

    /** CA-02 y CA-03: prenda, tipo y motivo obligatorios; 'Otro' exige el detalle. */
    val puedeEnviar: Boolean
        get() = itemSeleccionado != null && tipo != null && motivo != null && !faltaDetalleOtro && !enviando

    val fotosRestantes: Int
        get() = MAX_FOTOS - fotos.size

    /** Pedido entregado hace 7 días o menos (CA-01). */
    val dentroDelPlazo: Boolean
        get() = pedido?.puedeSolicitarCambioODevolucion() == true
}

/** HU-24 — Solicitud de cambio o devolución (CA-02 a CA-06). */
@HiltViewModel
class SolicitudViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    orderRepository: OrderRepository,
    private val solicitudRepository: SolicitudRepository,
) : ViewModel() {

    private val pedidoId: String = checkNotNull(savedStateHandle["pedidoId"])

    private val _uiState = MutableStateFlow(SolicitudUiState())
    val uiState: StateFlow<SolicitudUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                orderRepository.observarPedidoPorId(pedidoId),
                solicitudRepository.observarSolicitudesDePedido(pedidoId),
            ) { pedido, solicitudes -> pedido to solicitudes }
                .collect { (pedido, solicitudes) ->
                    val usadas = solicitudes.map { claveItem(it.prendaCodigo, it.talla) }.toSet()
                    _uiState.update { s ->
                        val disponibles = pedido?.items.orEmpty().filter { claveItem(it.codigo, it.talla) !in usadas }
                        s.copy(
                            cargando = false,
                            pedido = pedido,
                            prendasConSolicitud = usadas,
                            // Con una sola prenda disponible se elige sola.
                            itemSeleccionado = s.itemSeleccionado ?: disponibles.singleOrNull(),
                        )
                    }
                }
        }
    }

    fun claveItem(codigo: String, talla: String) = "${codigo}_$talla"

    fun onItem(item: ItemPedido) = _uiState.update { it.copy(itemSeleccionado = item, error = null) }
    fun onTipo(tipo: TipoSolicitud) = _uiState.update { it.copy(tipo = tipo, error = null) }
    fun onMotivo(motivo: String) = _uiState.update { it.copy(motivo = motivo, error = null) }
    fun onDetalleOtro(v: String) = _uiState.update { it.copy(detalleOtro = v.take(300), error = null) }

    /** CA-04: se agregan fotos hasta llegar a 3; si ya hay 3 se avisa el máximo. */
    fun agregarFotos(nuevas: List<Uri>) {
        if (nuevas.isEmpty()) return
        _uiState.update { s ->
            val caben = nuevas.take(s.fotosRestantes)
            s.copy(
                fotos = s.fotos + caben,
                avisoFotos = if (caben.size < nuevas.size) avisoMaximo() else null,
            )
        }
    }

    fun avisarMaximoFotos() = _uiState.update { it.copy(avisoFotos = avisoMaximo()) }

    fun quitarFoto(uri: Uri) = _uiState.update { it.copy(fotos = it.fotos - uri, avisoFotos = null) }

    fun avisoMostrado() = _uiState.update { it.copy(avisoFotos = null) }

    private fun avisoMaximo() = "Puedes adjuntar como máximo $MAX_FOTOS fotos."

    fun enviar() {
        val s = _uiState.value
        val pedido = s.pedido ?: return
        val item = s.itemSeleccionado
        val tipo = s.tipo
        val motivo = s.motivo
        if (item == null || tipo == null || motivo == null) {
            _uiState.update { it.copy(error = "Elige la prenda, el tipo de solicitud y el motivo.") }
            return
        }
        if (s.faltaDetalleOtro) {
            _uiState.update { it.copy(error = "Describe el motivo (mínimo $MIN_DETALLE_OTRO caracteres).") }
            return
        }
        if (s.enviando) return

        _uiState.update { it.copy(enviando = true, progreso = 0f, error = null) }
        viewModelScope.launch {
            solicitudRepository.registrar(
                nueva = NuevaSolicitud(
                    pedido = pedido,
                    item = item,
                    tipo = tipo,
                    motivo = motivo,
                    detalleOtro = if (motivo == MOTIVO_OTRO) s.detalleOtro else null,
                ),
                fotos = s.fotos,
                onProgreso = { p -> _uiState.update { it.copy(progreso = p) } },
            ).onSuccess { sol ->
                _uiState.update { it.copy(enviando = false, registrada = sol) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(enviando = false, error = e.message ?: "No se pudo registrar la solicitud. Intenta de nuevo.")
                }
            }
        }
    }
}
