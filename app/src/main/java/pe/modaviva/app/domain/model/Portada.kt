package pe.modaviva.app.domain.model

/**
 * Portada de campaña de la vitrina de Inicio (documento `inicio/portada`).
 * La tienda la cambia desde el back-office, sin publicar otra versión de la app.
 */
data class Portada(
    val etiqueta: String,
    val titulo: String,
    val imagenUrl: String,
    val accion: String,
    /** Slug de la categoría que abre la portada. */
    val categoria: String,
)
