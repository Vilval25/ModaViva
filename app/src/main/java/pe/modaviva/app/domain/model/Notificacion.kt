package pe.modaviva.app.domain.model

import java.time.Instant

/**
 * Tipo de aviso del Centro de notificaciones (HU-18). Define el ícono y la
 * pantalla que se abre al tocarlo (CA-04).
 */
enum class TipoNotificacion {
    /** La imagen del probador está lista (HU-12). [Notificacion.refId] = código de la prenda. */
    PRUEBA_VIRTUAL,

    /** Constancia de eliminación de la foto (HU-17). [Notificacion.refId] = epoch millis de la eliminación. */
    ELIMINACION_FOTO,

    /** Pago, vencimiento o cambio de estado de un pedido (HU-21 a HU-23). [Notificacion.refId] = id del pedido. */
    PEDIDO,

    /** Cambio de estado de una solicitud de cambio o devolución (HU-24). [Notificacion.refId] = id del pedido. */
    SOLICITUD,

    /** Aviso informativo sin pantalla asociada. */
    GENERAL,
    ;

    companion object {
        fun desdeCodigo(codigo: String?): TipoNotificacion =
            entries.firstOrNull { it.name.equals(codigo?.trim(), ignoreCase = true) } ?: GENERAL
    }
}

/** Aviso guardado en `clientes/{uid}/notificaciones/{id}` (HU-18). */
data class Notificacion(
    val id: String,
    val tipo: TipoNotificacion,
    val titulo: String,
    val texto: String,
    val creadaEn: Instant,
    val leida: Boolean,
    val refId: String? = null,
)

/** CA-01: del más reciente al más antiguo. */
fun List<Notificacion>.ordenadasRecientePrimero(): List<Notificacion> =
    sortedByDescending { it.creadaEn }

/** CA-02: cantidad que muestra la insignia del ícono de Inicio. */
fun List<Notificacion>.cantidadNoLeidas(): Int = count { !it.leida }
