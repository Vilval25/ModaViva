package pe.modaviva.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import pe.modaviva.app.domain.model.MedidasPrenda

/**
 * Base local de la app.
 *
 * Guarda copias de datos del servidor (catálogo) y también datos que SOLO
 * existen en el teléfono, como el carrito de un invitado (HU-09). Por eso,
 * desde la versión 5, cada cambio de esquema necesita una migración en
 * Migraciones.kt: si no, al actualizar la app se perderían esos datos.
 *
 * Al cambiar el esquema: subir `version`, compilar (Room guarda el esquema
 * nuevo en app/schemas/), escribir la migración y su prueba en
 * MigracionesTest.
 */
@Database(
    entities = [PrendaEntity::class, SincronizacionEntity::class, CartEntity::class],
    version = 5,
    exportSchema = true,
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
