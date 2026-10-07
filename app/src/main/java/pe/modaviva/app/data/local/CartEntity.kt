package pe.modaviva.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import pe.modaviva.app.domain.model.CartItem
import java.time.Instant

/**
 * Entidad de Room para el carrito de compras (HU-09).
 * Guarda la variante con clave primaria [varianteId] = "${prendaId}_${talla}".
 */
@Entity(tableName = "carrito")
data class CartEntity(
    @PrimaryKey val varianteId: String,
    val prendaId: String,
    val talla: String,
    val colorNombre: String,
    val colorHex: String,
    val cantidad: Int,
    val agregadoEnMillis: Long,
    val precioAlAgregar: Double = 0.0,
)

fun CartEntity.aDominio() = CartItem(
    varianteId = varianteId,
    prendaId = prendaId,
    talla = talla,
    colorNombre = colorNombre,
    colorHex = colorHex,
    cantidad = cantidad,
    precioAlAgregar = precioAlAgregar,
    agregadoEn = Instant.ofEpochMilli(agregadoEnMillis),
)

fun CartItem.aEntidad() = CartEntity(
    varianteId = varianteId,
    prendaId = prendaId,
    talla = talla,
    colorNombre = colorNombre,
    colorHex = colorHex,
    cantidad = cantidad,
    precioAlAgregar = precioAlAgregar,
    agregadoEnMillis = agregadoEn.toEpochMilli(),
)
