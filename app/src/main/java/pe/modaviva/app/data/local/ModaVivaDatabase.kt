package pe.modaviva.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import pe.modaviva.app.domain.model.MedidasPrenda

/**
 * Base local de la app. Por ahora solo guarda copias de datos del servidor,
 * así que un cambio de esquema puede borrarla y volver a descargarla (ver
 * DatabaseModule). Si algún día guarda datos que solo existen en el
 * dispositivo, como el carrito de un invitado (HU-09), habrá que escribir
 * migraciones.
 */
@Database(
    entities = [PrendaEntity::class, SincronizacionEntity::class, CartEntity::class],
    version = 5,
    exportSchema = false,
)
@TypeConverters(Convertidores::class)
abstract class ModaVivaDatabase : RoomDatabase() {
    abstract fun catalogoDao(): CatalogoDao
    abstract fun cartDao(): CartDao
}

class Convertidores {
    // Separador de unidad (U+001F): no aparece en tallas ni en URLs.
    @TypeConverter
    fun desdeLista(valores: List<String>): String =
        valores.joinToString(SEPARADOR)

    @TypeConverter
    fun aLista(texto: String): List<String> =
        if (texto.isEmpty()) emptyList() else texto.split(SEPARADOR)

    @TypeConverter
    fun desdeMedidas(
        valores: Map<String, MedidasPrenda>,
    ): String {
        return valores.entries.joinToString(";") { (talla, medida) ->
            listOf(
                talla,
                medida.pecho,
                medida.cintura,
                medida.cadera,
                medida.hombro,
                medida.largo,
                medida.manga,
            ).joinToString("|")
        }
    }

    @TypeConverter
    fun aMedidas(
        texto: String,
    ): Map<String, MedidasPrenda> {
        if (texto.isEmpty()) return emptyMap()

        return texto.split(";").mapNotNull { registro ->
            val partes = registro.split("|")

            if (partes.size != 7) {
                return@mapNotNull null
            }

            val talla = partes[0]

            val medidas = MedidasPrenda(
                pecho = partes[1].toDoubleOrNull() ?: return@mapNotNull null,
                cintura = partes[2].toDoubleOrNull() ?: return@mapNotNull null,
                cadera = partes[3].toDoubleOrNull() ?: return@mapNotNull null,
                hombro = partes[4].toDoubleOrNull() ?: return@mapNotNull null,
                largo = partes[5].toDoubleOrNull() ?: return@mapNotNull null,
                manga = partes[6].toDoubleOrNull() ?: return@mapNotNull null,
            )

            talla to medidas
        }.toMap()
    }

    private companion object {
        const val SEPARADOR = "\u001F"
    }
}
