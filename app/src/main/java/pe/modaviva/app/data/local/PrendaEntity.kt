package pe.modaviva.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import pe.modaviva.app.domain.model.ColorPrenda
import pe.modaviva.app.domain.model.Prenda
import java.time.Instant

/** Copia local de una prenda del catálogo (HU-04 CA-07). */
@Entity(tableName = "prendas")
data class PrendaEntity(
    @PrimaryKey val codigo: String,
    val nombre: String,
    val marca: String,
    val categoria: String,
    val subcategoria: String,
    val genero: String?,
    val precio: Double,
    val precioPromo: Double?,
    val promoHastaMillis: Long?,
    val colorSlug: String,
    val colorNombre: String,
    val colorHex: String,
    val tallas: List<String>,
    val fotos: List<String>,
    val stockTotal: Int,
    val creadaEnMillis: Long,
    val actualizadaEnMillis: Long?,
)

fun PrendaEntity.aDominio() = Prenda(
    codigo = codigo,
    nombre = nombre,
    marca = marca,
    categoria = categoria,
    subcategoria = subcategoria,
    genero = genero,
    precio = precio,
    precioPromo = precioPromo,
    promoHasta = promoHastaMillis?.let(Instant::ofEpochMilli),
    color = ColorPrenda(slug = colorSlug, nombre = colorNombre, hex = colorHex),
    tallas = tallas,
    fotos = fotos,
    stockTotal = stockTotal,
    creadaEn = Instant.ofEpochMilli(creadaEnMillis),
    actualizadaEn = actualizadaEnMillis?.let(Instant::ofEpochMilli),
)

fun Prenda.aEntidad() = PrendaEntity(
    codigo = codigo,
    nombre = nombre,
    marca = marca,
    categoria = categoria,
    subcategoria = subcategoria,
    genero = genero,
    precio = precio,
    precioPromo = precioPromo,
    promoHastaMillis = promoHasta?.toEpochMilli(),
    colorSlug = color.slug,
    colorNombre = color.nombre,
    colorHex = color.hex,
    tallas = tallas,
    fotos = fotos,
    stockTotal = stockTotal,
    creadaEnMillis = creadaEn.toEpochMilli(),
    actualizadaEnMillis = actualizadaEn?.toEpochMilli(),
)
