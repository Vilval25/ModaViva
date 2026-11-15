package pe.modaviva.app.domain.repository

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import pe.modaviva.app.domain.model.NuevaSolicitud
import pe.modaviva.app.domain.model.SolicitudPostventa

/** Solicitudes de cambio o devolución del cliente (HU-24). */
interface SolicitudRepository {
    /** Todas las solicitudes del cliente, de la más reciente a la más antigua. */
    fun observarSolicitudes(): Flow<List<SolicitudPostventa>>

    /** Solicitudes de un pedido (puede tener varias, una por prenda). */
    fun observarSolicitudesDePedido(pedidoId: String): Flow<List<SolicitudPostventa>>

    /**
     * Sube las fotos (máximo 3) a Storage y registra la solicitud 'En evaluación' con su número
     * (CA-04, CA-05). Marca si hubo prueba virtual previa de la prenda (CA-06).
     */
    suspend fun registrar(
        nueva: NuevaSolicitud,
        fotos: List<Uri>,
        onProgreso: (Float) -> Unit = {},
    ): Result<SolicitudPostventa>
}
