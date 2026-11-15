package pe.modaviva.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.modaviva.app.domain.model.MensajeChat
import pe.modaviva.app.domain.repository.ChatAssistantRepository
import java.util.UUID
import javax.inject.Inject

data class ChatAssistantUiState(
    val mensajes: List<MensajeChat> = listOf(
        MensajeChat(
            id = "welcome",
            texto = "¡Hola! Soy el asistente de ModaViva. Puedo recomendarte outfits, ayudarte con tu talla, consultar precios o ver cobertura de envío en Lima.",
            esUsuario = false,
        ),
    ),
    val enviando: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ChatAssistantViewModel @Inject constructor(
    private val chatRepository: ChatAssistantRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatAssistantUiState())
    val uiState: StateFlow<ChatAssistantUiState> = _uiState.asStateFlow()

    fun enviarMensaje(textoRaw: String, prendaCodigo: String? = null) {
        val texto = textoRaw.trim()
        if (texto.isEmpty() || _uiState.value.enviando) return

        val historialPrevio = _uiState.value.mensajes.filter { it.id != "welcome" }
        val mensajeUsuario = MensajeChat(
            id = UUID.randomUUID().toString(),
            texto = texto,
            esUsuario = true,
        )

        _uiState.update {
            it.copy(
                mensajes = it.mensajes + mensajeUsuario,
                enviando = true,
                error = null,
            )
        }

        viewModelScope.launch {
            chatRepository.enviarMensaje(
                mensaje = texto,
                historial = historialPrevio,
                prendaCodigo = prendaCodigo,
            ).onSuccess { resp ->
                val mensajeAsistente = MensajeChat(
                    id = UUID.randomUUID().toString(),
                    texto = resp.respuesta,
                    esUsuario = false,
                    prendasSugeridas = resp.prendasSugeridas,
                )
                _uiState.update {
                    it.copy(
                        mensajes = it.mensajes + mensajeAsistente,
                        enviando = false,
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        enviando = false,
                        error = err.localizedMessage?.takeIf { msg -> msg.isNotBlank() }
                            ?: "No se pudo conectar con el asistente. Inténtalo de nuevo.",
                    )
                }
            }
        }
    }

    fun limpiarError() {
        _uiState.update { it.copy(error = null) }
    }
}

