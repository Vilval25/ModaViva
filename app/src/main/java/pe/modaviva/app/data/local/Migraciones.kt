package pe.modaviva.app.data.local

import androidx.room.migration.Migration

/**
 * Migraciones de la base local, una por cada cambio de versión desde la 5.
 *
 * Cada una transforma la base del teléfono sin perder lo que solo existe ahí
 * (el carrito del invitado). El SQL se copia del esquema que Room exporta en
 * app/schemas/ para la versión nueva, y cada migración tiene su prueba en
 * androidTest (MigracionesTest).
 */
object Migraciones {

    val TODAS: Array<Migration> = arrayOf()
}
