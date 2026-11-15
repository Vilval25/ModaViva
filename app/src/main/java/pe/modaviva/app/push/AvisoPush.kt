package pe.modaviva.app.push

import android.content.Intent
import pe.modaviva.app.domain.model.TipoNotificacion

/**
 * Datos de un aviso que llegan en el push y viajan en el Intent que abre la app
 * al tocar la notificación (HU-18 CA-04 y CA-05).
 */
data class AvisoPush(
    val avisoId: String,
    val tipo: TipoNotificacion,
    val refId: String?,
) {
    fun escribirEn(intent: Intent): Intent = intent
        .putExtra(EXTRA_AVISO_ID, avisoId)
        .putExtra(EXTRA_TIPO, tipo.name)
        .putExtra(EXTRA_REF_ID, refId)

    companion object {
        const val CLAVE_AVISO_ID = "avisoId"
        const val CLAVE_TIPO = "tipo"
        const val CLAVE_TITULO = "titulo"
        const val CLAVE_TEXTO = "texto"
        const val CLAVE_REF_ID = "refId"

        private const val EXTRA_AVISO_ID = "pe.modaviva.app.extra.AVISO_ID"
        private const val EXTRA_TIPO = "pe.modaviva.app.extra.TIPO"
        private const val EXTRA_REF_ID = "pe.modaviva.app.extra.REF_ID"

        /** Datos del mensaje de FCM. */
        fun desdeDatos(datos: Map<String, String>): AvisoPush? {
            val id = datos[CLAVE_AVISO_ID]?.takeIf { it.isNotBlank() } ?: return null
            return AvisoPush(
                avisoId = id,
                tipo = TipoNotificacion.desdeCodigo(datos[CLAVE_TIPO]),
                refId = datos[CLAVE_REF_ID]?.takeIf { it.isNotBlank() },
            )
        }

        /** Intent con el que se abrió la app desde la notificación, si lo hay. */
        fun desdeIntent(intent: Intent?): AvisoPush? {
            val id = intent?.getStringExtra(EXTRA_AVISO_ID)?.takeIf { it.isNotBlank() } ?: return null
            return AvisoPush(
                avisoId = id,
                tipo = TipoNotificacion.desdeCodigo(intent.getStringExtra(EXTRA_TIPO)),
                refId = intent.getStringExtra(EXTRA_REF_ID)?.takeIf { it.isNotBlank() },
            )
        }

        /** Quita los datos del Intent para no volver a navegar al rotar la pantalla. */
        fun limpiar(intent: Intent?) {
            intent?.removeExtra(EXTRA_AVISO_ID)
            intent?.removeExtra(EXTRA_TIPO)
            intent?.removeExtra(EXTRA_REF_ID)
        }
    }
}
