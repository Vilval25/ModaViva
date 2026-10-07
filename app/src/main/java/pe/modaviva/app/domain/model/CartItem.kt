package pe.modaviva.app.domain.model

import java.time.Instant

/**
 * Ítem del carrito de compras (HU-09).
 * La clave de la variante es [varianteId] = "${prendaId}_${talla}".
 */
data class CartItem(
    val varianteId: String,
    val prendaId: String,
    val talla: String,
    val colorNombre: String,
    val colorHex: String,
    val cantidad: Int,
    val precioAlAgregar: Double = 0.0,
    val agregadoEn: Instant = Instant.now(),
) {
    companion object {
        fun generarVarianteId(prendaId: String, talla: String): String = "${prendaId}_${talla}"
    }
}
