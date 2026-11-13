package pe.modaviva.app.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.modaviva.app.data.repository.CheckoutSessionRepository
import pe.modaviva.app.domain.model.DireccionEntrega
import pe.modaviva.app.domain.model.Tienda
import pe.modaviva.app.domain.model.aDestinoEntrega
import pe.modaviva.app.domain.repository.DireccionRepository
import pe.modaviva.app.domain.repository.SessionRepository
import pe.modaviva.app.domain.repository.TiendaRepository
import javax.inject.Inject

data class CheckoutDeliveryUiState(
    val esDelivery: Boolean = true,
    val destinatario: String = "",
    val telefono: String = "",
    val direccionTexto: String = "",
    val referencia: String = "",
    val distrito: String = "",
    val tiendaRecojo: Tienda? = null,
    val direccionesRegistradas: List<DireccionEntrega> = emptyList(),
    val direccionSeleccionadaId: String? = null,
    val error: String? = null,
)

@HiltViewModel
class CheckoutDeliveryViewModel @Inject constructor(
    private val checkoutSessionRepository: CheckoutSessionRepository,
    private val direccionRepository: DireccionRepository,
    private val tiendaRepository: TiendaRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutDeliveryUiState())
    val uiState: StateFlow<CheckoutDeliveryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val tipo = checkoutSessionRepository.tipoEntrega.value
            val esDelivery = tipo.equals("delivery", ignoreCase = true)
            val destino = checkoutSessionRepository.destinoActual.value
            val tiendaId = checkoutSessionRepository.tiendaRecojoId.value
            val profile = sessionRepository.currentUser.value
            val nombrePorDefecto = profile?.let { "${it.nombres} ${it.apellidos}".trim() } ?: ""
            val telPorDefecto = profile?.telefono ?: ""

            val destGuardado = checkoutSessionRepository.destinatario.value
            val telGuardado = checkoutSessionRepository.telefonoContacto.value

            _uiState.update {
                it.copy(
                    esDelivery = esDelivery,
                    destinatario = if (destGuardado.isNotBlank()) destGuardado else nombrePorDefecto,
                    telefono = if (telGuardado.isNotBlank()) telGuardado else telPorDefecto,
                    direccionTexto = destino?.direccion.orEmpty(),
                    referencia = destino?.referencia.orEmpty(),
                    distrito = destino?.distrito.orEmpty(),
                )
            }

            if (!esDelivery && tiendaId != null) {
                tiendaRepository.observarTiendas().collect { tiendas ->
                    val tienda = tiendas.firstOrNull { it.id == tiendaId }
                    _uiState.update { it.copy(tiendaRecojo = tienda) }
                }
            }
        }

        viewModelScope.launch {
            direccionRepository.observarDirecciones().collect { dirs ->
                val destinoActual = checkoutSessionRepository.destinoActual.value
                val matched = dirs.firstOrNull { it.direccion == destinoActual?.direccion && it.distrito == destinoActual?.distrito }
                    ?: dirs.firstOrNull { it.esPrincipal }
                    ?: dirs.firstOrNull()

                _uiState.update { current ->
                    current.copy(
                        direccionesRegistradas = dirs,
                        direccionSeleccionadaId = matched?.id,
                        direccionTexto = current.direccionTexto.ifBlank { matched?.direccion.orEmpty() },
                        referencia = current.referencia.ifBlank { matched?.referencia.orEmpty() },
                        distrito = current.distrito.ifBlank { matched?.distrito.orEmpty() },
                        destinatario = current.destinatario.ifBlank { matched?.destinatario.orEmpty() },
                        telefono = current.telefono.ifBlank { matched?.telefono.orEmpty() },
                    )
                }
            }
        }
    }

    fun onSeleccionarDireccionRegistrada(dir: DireccionEntrega) {
        _uiState.update {
            it.copy(
                direccionSeleccionadaId = dir.id,
                direccionTexto = dir.direccion,
                referencia = dir.referencia.orEmpty(),
                distrito = dir.distrito,
                destinatario = if (dir.destinatario.isNotBlank()) dir.destinatario else it.destinatario,
                telefono = if (dir.telefono.isNotBlank()) dir.telefono else it.telefono,
                error = null,
            )
        }
        checkoutSessionRepository.actualizarDestino(dir.aDestinoEntrega())
    }

    fun onDestinatarioChanged(v: String) {
        _uiState.update { it.copy(destinatario = v, error = null) }
    }

    fun onTelefonoChanged(v: String) {
        _uiState.update { it.copy(telefono = v, error = null) }
    }

    fun onDireccionTextoChanged(v: String) {
        _uiState.update { it.copy(direccionTexto = v, error = null) }
    }

    fun onReferenciaChanged(v: String) {
        _uiState.update { it.copy(referencia = v) }
    }

    fun validarYContinuar(onContinuar: () -> Unit) {
        val s = _uiState.value
        if (s.destinatario.trim().length < 3) {
            _uiState.update { it.copy(error = "Ingresa el nombre completo de quien recibe o recoge.") }
            return
        }
        if (s.telefono.filter { it.isDigit() }.length < 9) {
            _uiState.update { it.copy(error = "Ingresa un número de celular de contacto válido (9 dígitos).") }
            return
        }
        if (s.esDelivery && s.direccionTexto.trim().length < 5) {
            _uiState.update { it.copy(error = "Ingresa la dirección exacta de entrega.") }
            return
        }

        // Guardar datos en CheckoutSessionRepository
        val destinoPrevio = checkoutSessionRepository.destinoActual.value
        val destinoActualizado = (destinoPrevio ?: pe.modaviva.app.domain.model.DestinoEntrega()).copy(
            direccion = s.direccionTexto.trim(),
            referencia = s.referencia.trim().ifBlank { null },
            distrito = s.distrito.ifBlank { destinoPrevio?.distrito.orEmpty() },
        )
        checkoutSessionRepository.actualizarDestino(destinoActualizado)
        checkoutSessionRepository.actualizarDatosContacto(s.destinatario.trim(), s.telefono.trim())

        onContinuar()
    }
}

