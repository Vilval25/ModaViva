package pe.modaviva.app.domain.model

import java.time.Instant

/**
 * Prenda guardada como favorita (HU-08). Solo es la referencia: precio, stock
 * y si sigue publicada se leen de la prenda actual (CA-03).
 */
data class Favorito(
    val prendaId: String,
    val agregadoEn: Instant,
)

/** Lo que se sabe de una favorita que ya no está en el catálogo publicado. */
data class PrendaRetirada(
    val nombre: String,
    val marca: String,
    val foto: String?,
)
