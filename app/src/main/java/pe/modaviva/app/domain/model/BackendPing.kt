package pe.modaviva.app.domain.model

/** Respuesta de la Cloud Function `ping`. */
data class BackendPing(
    val message: String,
    val serverTime: String,
)
