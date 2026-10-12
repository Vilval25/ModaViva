package pe.modaviva.app.ui.favorites

import pe.modaviva.app.domain.model.Favorito
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.domain.model.PrendaRetirada
import pe.modaviva.app.ui.format.formatearSoles
import java.time.Instant

/** Una tarjeta de Favoritos, con lo que la prenda es hoy (HU-08 CA-03). */
data class FavoritoUi(
    val favorito: Favorito,
    /** Vacío si es una prenda retirada de la que aún no se sabe el nombre. */
    val nombre: String,
    val marca: String?,
    val foto: String?,
    /** Precio vigente formateado; null si la prenda ya no está a la venta. */
    val precio: String?,
    val precioOriginal: String?,
    val agotada: Boolean,
) {
    val codigo: String get() = favorito.prendaId
    val disponible: Boolean get() = precio != null
}

/**
 * Cruza los favoritos con el catálogo publicado, en el orden de los favoritos
 * (del más reciente al más antiguo, CA-02). La que sigue publicada muestra su
 * precio y stock actuales; la que no, el aviso "Ya no disponible" con lo que
 * se sepa de ella en [retiradas].
 */
fun armarFavoritos(
    favoritos: List<Favorito>,
    catalogo: List<Prenda>,
    retiradas: Map<String, PrendaRetirada>,
    ahora: Instant,
): List<FavoritoUi> {
    val porCodigo = catalogo.associateBy { it.codigo }
    return favoritos.map { favorito ->
        val prenda = porCodigo[favorito.prendaId]
        if (prenda != null) {
            FavoritoUi(
                favorito = favorito,
                nombre = prenda.nombre,
                marca = prenda.marca,
                foto = prenda.fotos.firstOrNull(),
                precio = formatearSoles(prenda.precioVigente(ahora)),
                precioOriginal = if (prenda.enPromocion(ahora)) formatearSoles(prenda.precio) else null,
                agotada = prenda.agotada,
            )
        } else {
            val retirada = retiradas[favorito.prendaId]
            FavoritoUi(
                favorito = favorito,
                nombre = retirada?.nombre.orEmpty(),
                marca = retirada?.marca?.takeIf { it.isNotBlank() },
                foto = retirada?.foto,
                precio = null,
                precioOriginal = null,
                agotada = false,
            )
        }
    }
}
