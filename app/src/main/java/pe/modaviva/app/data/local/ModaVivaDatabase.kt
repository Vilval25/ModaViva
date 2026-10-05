package pe.modaviva.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

/**
 * Base local de la app. Por ahora solo guarda copias de datos del servidor,
 * así que un cambio de esquema puede borrarla y volver a descargarla (ver
 * DatabaseModule). Si algún día guarda datos que solo existen en el
 * dispositivo, como el carrito de un invitado (HU-09), habrá que escribir
 * migraciones.
 */
@Database(
    entities = [PrendaEntity::class, SincronizacionEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Convertidores::class)
abstract class ModaVivaDatabase : RoomDatabase() {
    abstract fun catalogoDao(): CatalogoDao
}

class Convertidores {
    // Separador de unidad (U+001F): no aparece en tallas ni en URLs.
    @TypeConverter
    fun desdeLista(valores: List<String>): String = valores.joinToString(SEPARADOR)

    @TypeConverter
    fun aLista(texto: String): List<String> = if (texto.isEmpty()) emptyList() else texto.split(SEPARADOR)

    private companion object {
        const val SEPARADOR = "\u001F"
    }
}
