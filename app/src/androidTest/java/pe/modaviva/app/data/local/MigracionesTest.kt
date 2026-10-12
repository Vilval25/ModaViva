package pe.modaviva.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
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
    fun todasLasMigracionesLleganALaVersionActual() {
        helper.createDatabase(BASE, 5).close()
        helper.runMigrationsAndValidate(BASE, 6, true, *Migraciones.TODAS).close()
    }

    private companion object {
        const val BASE = "migraciones-test.db"
    }
}
