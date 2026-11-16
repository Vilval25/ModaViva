package pe.modaviva.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.modaviva.app.domain.model.MensajeChat
import pe.modaviva.app.domain.repository.ChatAssistantRepository
import java.util.UUID
import javax.inject.Inject

private val BIENVENIDA = MensajeChat(
    id = "welcome",
    texto = "¡Hola! Soy el asistente de ModaViva. Puedo ayudarte con tallas, envíos, cambios y devoluciones, " +
        "pagos y las prendas del catálogo.",
    esUsuario = false,
)

data class ChatAssistantUiState(
    val mensajes: List<MensajeChat> = listOf(BIENVENIDA),
    val cargandoHistorial: Boolean = true,
    val enviando: Boolean = false,
    /** HU-27 CA-05: id del mensaje que no se pudo enviar y su motivo; se puede reintentar. */
    val mensajeFallidoId: String? = null,
    val error: String? = null,
) {
    val hayConversacion: Boolean get() = mensajes.any { it.id != BIENVENIDA.id }
}

/** Mensaje del cliente que aún no queda guardado en la conversación. */
private data class Pendiente(
    val mensaje: MensajeChat,
    val prendaCodigo: String?,
    val respuestaProvisional: MensajeChat? = null,
    val fallo: String? = null,
)

@HiltViewModel
class ChatAssistantViewModel @Inject constructor(
    private val chatRepository: ChatAssistantRepository,
) : ViewModel() {

    private val pendiente = MutableStateFlow<Pendiente?>(null)
    private val enviando = MutableStateFlow(false)
    private var guardados: List<MensajeChat> = emptyList()

    /** HU-27 CA-04: la conversación guardada se carga al volver al chat. */
    val uiState: StateFlow<ChatAssistantUiState> = combine(
        chatRepository.observarConversacion().onStart { emit(emptyList()) },
        pendiente,
        enviando,
    ) { lista, pend, enviandoAhora ->
        guardados = lista
        // Cuando la pregunta y su respuesta ya quedaron guardadas (son el último par de la
        // conversación), el pendiente se descarta.
        val vigente = pend?.takeUnless {
            it.fallo == null && it.respuestaProvisional != null &&
                lista.lastOrNull()?.esUsuario == false &&
                lista.lastOrNull { m -> m.esUsuario }?.texto == it.mensaje.texto
        }
        if (vigente == null && pend != null) pendiente.value = null
        ChatAssistantUiState(
            mensajes = listOf(BIENVENIDA) + lista + listOfNotNull(vigente?.mensaje, vigente?.respuestaProvisional),
            cargandoHistorial = false,
            enviando = enviandoAhora,
            mensajeFallidoId = vigente?.takeIf { it.fallo != null }?.mensaje?.id,
            error = vigente?.fallo,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatAssistantUiState())

    fun enviarMensaje(textoRaw: String, prendaCodigo: String? = null) {
        val texto = textoRaw.trim()
        if (texto.isEmpty() || enviando.value) return
        // Un mensaje fallido sin reintentar se reemplaza por el nuevo.
        val mensaje = MensajeChat(id = UUID.randomUUID().toString(), texto = texto, esUsuario = true)
        enviar(Pendiente(mensaje, prendaCodigo))
    }

    /** HU-27 CA-05: vuelve a enviar el mensaje que falló. */
    fun reintentar() {
        val actual = pendiente.value ?: return
        if (actual.fallo == null || enviando.value) return
        enviar(actual.copy(fallo = null))
    }

    private fun enviar(p: Pendiente) {
        pendiente.value = p
        enviando.value = true
        viewModelScope.launch {
            chatRepository.enviarMensaje(
                mensaje = p.mensaje.texto,
                historial = guardados,
                prendaCodigo = p.prendaCodigo,
            ).onSuccess { resp ->
                // La respuesta se muestra al instante; la versión guardada llega por Firestore.
                pendiente.update { actual ->
                    actual?.copy(
                        respuestaProvisional = MensajeChat(
                            id = "respuesta-${p.mensaje.id}",
                            texto = resp.respuesta,
                            esUsuario = false,
                            prendasSugeridas = resp.prendasSugeridas,
                        ),
                    )
                }
            }.onFailure { err ->
                pendiente.update { actual ->
                    actual?.copy(
                        fallo = err.localizedMessage?.takeIf { it.isNotBlank() }
                            ?: "No se pudo conectar con el asistente.",
                    )
                }
            }
            enviando.value = false
        }
    }

    /** Borra la conversación guardada y empieza una nueva. */
    fun nuevaConversacion() {
        pendiente.value = null
        viewModelScope.launch { chatRepository.borrarConversacion() }
    }
}
