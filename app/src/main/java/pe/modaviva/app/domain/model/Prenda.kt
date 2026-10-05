package pe.modaviva.app.domain.model

import java.time.Instant

/**
 * Prenda del catálogo (colección `prendas`, ver docs/modelo-datos.md).
 * Cada prenda es única y tiene un solo color.
 */
data class Prenda(
    val codigo: String,
    val nombre: String,
    val descripcion: String,
    val marca: String,
    val categoria: String,
    val subcategoria: String,
    val genero: String?,
    val precio: Double,
    val precioPromo: Double?,
    val promoHasta: Instant?,
    val color: ColorPrenda,
    val tallas: List<String>,
    val fotos: List<String>,
    val stockTotal: Int,
    val creadaEn: Instant,
    val actualizadaEn: Instant?,
) {
    /** Hay promoción si tiene precio promocional y no venció (HU-06 CA-07). */
    fun enPromocion(ahora: Instant): Boolean =
        precioPromo != null && (promoHasta == null || !ahora.isAfter(promoHasta))

    /** Precio que se cobra hoy (HU-04 CA-06). */
    fun precioVigente(ahora: Instant): Double =
        if (enPromocion(ahora)) precioPromo!! else precio

    val agotada: Boolean
        get() = stockTotal <= 0
}

data class ColorPrenda(
    val slug: String,
    val nombre: String,
    val hex: String,
)
