package pe.modaviva.app.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.modaviva.app.domain.model.DestinoEntrega
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckoutSessionRepository @Inject constructor() {

    private val _destinoActual = MutableStateFlow<DestinoEntrega?>(null)
    val destinoActual: StateFlow<DestinoEntrega?> = _destinoActual.asStateFlow()

    private val _tiendaRecojoId = MutableStateFlow<String?>(null)
    val tiendaRecojoId: StateFlow<String?> = _tiendaRecojoId.asStateFlow()

    fun actualizarDestino(destino: DestinoEntrega) {
        _destinoActual.value = destino
    }

    fun actualizarTiendaRecojo(tiendaId: String?) {
        _tiendaRecojoId.value = tiendaId
    }

    fun limpiarDestino() {
        _destinoActual.value = null
        _tiendaRecojoId.value = null
    }
}
