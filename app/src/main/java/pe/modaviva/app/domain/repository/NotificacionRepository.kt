package pe.modaviva.app.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.modaviva.app.domain.model.Notificacion
import pe.modaviva.app.domain.model.TipoNotificacion

/** Bandeja de avisos del cliente con sesión (HU-18). Sin sesión, la bandeja está vacía. */
interface NotificacionRepository {

    /** Avisos del cliente, del más reciente al más antiguo, actualizados en tiempo real. */
    val notificaciones: Flow<List<Notificacion>>

    /** Cantidad de avisos sin leer, para la insignia de Inicio. */
    val noLeidas: Flow<Int>

    suspend fun marcarLeida(id: String)

    suspend fun marcarTodasLeidas()

    /**
     * Registra un aviso para el cliente con sesión. No bloquea ni lanza errores:
     * si no se puede guardar, el flujo que lo origina (pago, solicitud…) sigue igual.
     */
    fun crear(
        tipo: TipoNotificacion,
        titulo: String,
        texto: String,
        refId: String? = null,
    )
}
