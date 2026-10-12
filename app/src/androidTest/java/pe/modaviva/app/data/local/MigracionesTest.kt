package pe.modaviva.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprueba cada migración contra los esquemas exportados en app/schemas/:
 * la base resultante debe ser idéntica a la que crea Room, y el carrito del
 * invitado (que solo existe en el teléfono) debe sobrevivir.
 */
@RunWith(AndroidJUnit4::class)
class MigracionesTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ModaVivaDatabase::class.java,
    )

    @Test
    fun de5a6ConservaElCarrito() {
        helper.createDatabase(BASE, 5).use { db ->
            db.execSQL(
                "INSERT INTO carrito (varianteId, prendaId, talla, colorNombre, colorHex, " +
                    "cantidad, agregadoEnMillis, precioAlAgregar) " +
                    "VALUES ('AB-1002-M', 'AB-1002', 'M', 'Negro', '#000000', 2, 1700000000000, 349.9)",
            )
        }

        // Valida además que el esquema final coincide con 6.json.
        helper.runMigrationsAndValidate(BASE, 6, true, Migraciones.DE_5_A_6).use { db ->
            db.query("SELECT varianteId, cantidad FROM carrito").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals("AB-1002-M", cursor.getString(0))
                assertEquals(2, cursor.getInt(1))
            }
        }
    }

    @Test
    fun de6a7ConservaLaPortadaYElCarrito() {
        helper.createDatabase(BASE, 6).use { db ->
            db.execSQL(
                "INSERT INTO portada (id, etiqueta, titulo, imagenUrl, accion, categoria) " +
                    "VALUES ('portada', '', 'Abrigos', 'https://x/1.jpg', 'Descubrir', 'casacas-y-abrigos')",
            )
            db.execSQL(
                "INSERT INTO carrito (varianteId, prendaId, talla, colorNombre, colorHex, " +
                    "cantidad, agregadoEnMillis, precioAlAgregar) " +
                    "VALUES ('AB-1002-M', 'AB-1002', 'M', 'Negro', '#000000', 1, 1700000000000, 349.9)",
            )
        }

        helper.runMigrationsAndValidate(BASE, 7, true, Migraciones.DE_6_A_7).use { db ->
            db.query("SELECT titulo, marca FROM portada").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Abrigos", cursor.getString(0))
                assertTrue(cursor.isNull(1))
            }
            db.query("SELECT COUNT(*) FROM carrito").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
        }
    }

    @Test
    fun todasLasMigracionesLleganALaVersionActual() {
        helper.createDatabase(BASE, 5).close()
        helper.runMigrationsAndValidate(BASE, 7, true, *Migraciones.TODAS).close()
    }

    private companion object {
        const val BASE = "migraciones-test.db"
    }
}
