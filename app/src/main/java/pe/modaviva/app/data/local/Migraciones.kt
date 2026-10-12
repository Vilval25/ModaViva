package pe.modaviva.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migraciones de la base local, una por cada cambio de versión desde la 5.
 *
 * Cada una transforma la base del teléfono sin perder lo que solo existe ahí
 * (el carrito del invitado). El SQL se copia del esquema que Room exporta en
 * app/schemas/ para la versión nueva, y cada migración tiene su prueba en
 * androidTest (MigracionesTest).
 */
object Migraciones {

    /** 5 → 6: tabla de la portada de Inicio (HU-04). No toca el carrito. */
    val DE_5_A_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `portada` (`id` TEXT NOT NULL, " +
                    "`etiqueta` TEXT NOT NULL, `titulo` TEXT NOT NULL, " +
                    "`imagenUrl` TEXT NOT NULL, `accion` TEXT NOT NULL, " +
                    "`categoria` TEXT NOT NULL, PRIMARY KEY(`id`))",
            )
        }
    }

    /** 6 → 7: la portada puede llevar a una marca (HU-04). */
    val DE_6_A_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `portada` ADD COLUMN `marca` TEXT")
        }
    }

    /** 7 → 8: copia local de los favoritos de la cuenta (HU-08). */
    val DE_7_A_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `favoritos` (`prendaId` TEXT NOT NULL, " +
                    "`agregadoEnMillis` INTEGER NOT NULL, PRIMARY KEY(`prendaId`))",
            )
        }
    }

    val TODAS: Array<Migration> = arrayOf(DE_5_A_6, DE_6_A_7, DE_7_A_8)
}
