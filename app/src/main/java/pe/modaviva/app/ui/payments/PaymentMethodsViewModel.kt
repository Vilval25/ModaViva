package pe.modaviva.app.ui.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.modaviva.app.domain.model.DatosTarjetaNueva
import pe.modaviva.app.domain.model.ResultadoRegistroTarjeta
import pe.modaviva.app.domain.model.TarjetaGuardada
import pe.modaviva.app.domain.repository.OrderRepository
import javax.inject.Inject

/** Formulario 'Agregar tarjeta'. Los datos viven solo en memoria hasta enviarse a la pasarela. */
data class FormularioTarjeta(
    val numero: String = "",
    val vencimiento: String = "",
    val cvv: String = "",
    val titular: String = "",
    val esDebito: Boolean = false,
    val enviando: Boolean = false,
    /** Motivo de rechazo de la pasarela o error de validación (CA-05). */
    val error: String? = null,
)

data class PaymentMethodsUiState(
    val cargando: Boolean = true,
    val tarjetas: List<TarjetaGuardada> = emptyList(),
    val formulario: FormularioTarjeta? = null,
    val mensaje: String? = null,
)

/** HU-21 — Perfil > Métodos de pago (CA-04, CA-05, CA-06). */
@HiltViewModel
class PaymentMethodsViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentMethodsUiState())
    val uiState: StateFlow<PaymentMethodsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            orderRepository.observarTarjetasGuardadas().collect { tarjetas ->
                _uiState.update { it.copy(cargando = false, tarjetas = tarjetas) }
            }
        }
    }

    fun abrirFormulario() {
        _uiState.update { it.copy(formulario = FormularioTarjeta()) }
    }

    fun cerrarFormulario() {
        if (_uiState.value.formulario?.enviando == true) return
        _uiState.update { it.copy(formulario = null) }
    }

    private fun editar(cambio: (FormularioTarjeta) -> FormularioTarjeta) {
        _uiState.update { s -> s.formulario?.let { s.copy(formulario = cambio(it).copy(error = null)) } ?: s }
    }

    fun onNumero(v: String) = editar { it.copy(numero = v.filter(Char::isDigit).take(16)) }
    fun onVencimiento(v: String) = editar { it.copy(vencimiento = formatearVencimiento(v)) }
    fun onCvv(v: String) = editar { it.copy(cvv = v.filter(Char::isDigit).take(4)) }
    fun onTitular(v: String) = editar { it.copy(titular = v.uppercase().take(40)) }
    fun onEsDebito(v: Boolean) = editar { it.copy(esDebito = v) }

    fun guardarTarjeta() {
        val f = _uiState.value.formulario ?: return
        if (f.enviando) return
        _uiState.update { it.copy(formulario = f.copy(enviando = true, error = null)) }

        viewModelScope.launch {
            val datos = DatosTarjetaNueva(
                numero = f.numero,
                vencimiento = f.vencimiento,
                cvv = f.cvv,
                titular = f.titular,
                tipo = if (f.esDebito) "DEBITO" else "CREDITO",
            )
            orderRepository.registrarTarjeta(datos)
                .onSuccess { r ->
                    when (r) {
                        is ResultadoRegistroTarjeta.Guardada -> _uiState.update {
                            it.copy(formulario = null, mensaje = "Tarjeta ${r.etiqueta} guardada.")
                        }
                        is ResultadoRegistroTarjeta.Rechazada -> _uiState.update {
                            it.copy(formulario = f.copy(enviando = false, cvv = "", error = r.motivo))
                        }
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            formulario = f.copy(
                                enviando = false,
                                error = e.message ?: "No se pudo conectar con la pasarela.",
                            ),
                        )
                    }
                }
        }
    }

    fun eliminar(tarjeta: TarjetaGuardada) {
        viewModelScope.launch {
            orderRepository.eliminarTarjeta(tarjeta.id)
                .onSuccess { _uiState.update { it.copy(mensaje = "Tarjeta ${tarjeta.etiquetaResumen} eliminada.") } }
                .onFailure { _uiState.update { it.copy(mensaje = "No se pudo eliminar la tarjeta.") } }
        }
    }

    fun hacerPredeterminada(tarjeta: TarjetaGuardada) {
        viewModelScope.launch {
            orderRepository.establecerTarjetaPredeterminada(tarjeta.id)
                .onFailure { _uiState.update { it.copy(mensaje = "No se pudo cambiar la tarjeta predeterminada.") } }
        }
    }

    fun mensajeMostrado() {
        _uiState.update { it.copy(mensaje = null) }
    }
}

/** Convierte lo que se escribe en MM/AA, agregando la barra automáticamente. */
internal fun formatearVencimiento(v: String): String {
    val d = v.filter(Char::isDigit).take(4)
    return if (d.length <= 2) d else "${d.take(2)}/${d.drop(2)}"
}
