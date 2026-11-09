package pe.modaviva.app.ui.fitting

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.modaviva.app.domain.model.ConstanciaEliminacionFoto
import pe.modaviva.app.domain.repository.FittingRepository

/** En qué punto está la eliminación de la foto del probador (HU-17). */
sealed interface EstadoEliminacion {
    data object Inactiva : EstadoEliminacion

    /** El diálogo que explica las consecuencias está abierto (CA-02). */
    data object Confirmando : EstadoEliminacion

    data object Eliminando : EstadoEliminacion

    /** No se eliminó nada; la foto sigue registrada y se puede reintentar. */
    data class Fallida(val motivo: MotivoFallo) : EstadoEliminacion

    /** Terminó: se muestra la constancia (CA-03). */
    data class Hecha(val constancia: ConstanciaEliminacionFoto) : EstadoEliminacion
}

enum class MotivoFallo { SIN_CONEXION, ERROR }

/**
 * Flujo de eliminación de la foto (HU-17), separado del ViewModel para poder
 * probarlo sin Android: pedir → confirmar o cancelar → constancia o error.
 */
class EliminacionDeFoto(
    private val repositorio: FittingRepository,
    private val hayConexion: suspend () -> Boolean,
    private val scope: CoroutineScope,
) {
    private val _estado = MutableStateFlow<EstadoEliminacion>(EstadoEliminacion.Inactiva)
    val estado: StateFlow<EstadoEliminacion> = _estado.asStateFlow()

    /** CA-01: toca 'Eliminar mi foto'. También sirve para reintentar tras un fallo. */
    fun solicitar() {
        val actual = _estado.value
        if (actual is EstadoEliminacion.Inactiva || actual is EstadoEliminacion.Fallida) {
            _estado.value = EstadoEliminacion.Confirmando
        }
    }

    /** CA-02: si cancela, no se elimina nada. */
    fun cancelar() {
        if (_estado.value is EstadoEliminacion.Confirmando) {
            _estado.value = EstadoEliminacion.Inactiva
        }
    }

    fun confirmar() {
        if (_estado.value !is EstadoEliminacion.Confirmando) return
        _estado.value = EstadoEliminacion.Eliminando
        scope.launch {
            if (!hayConexion()) {
                _estado.value = EstadoEliminacion.Fallida(MotivoFallo.SIN_CONEXION)
                return@launch
            }
            _estado.value = repositorio.deleteFittingPhoto().fold(
                onSuccess = { EstadoEliminacion.Hecha(it) },
                onFailure = { EstadoEliminacion.Fallida(MotivoFallo.ERROR) },
            )
        }
    }

    /** Cierra la constancia o el aviso de error. */
    fun cerrar() {
        val actual = _estado.value
        if (actual is EstadoEliminacion.Hecha || actual is EstadoEliminacion.Fallida) {
            _estado.value = EstadoEliminacion.Inactiva
        }
    }
}
