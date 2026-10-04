package pe.modaviva.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pe.modaviva.app.data.repository.FirebaseBackendRepository

/**
 * HT-02 CA-05: la app llama a la Cloud Function de prueba y responde
 * correctamente. Usa el backend real, así que necesita internet.
 */
@RunWith(AndroidJUnit4::class)
class PingFunctionTest {

    @Test
    fun pingRespondsPong() = runBlocking {
        val repository = FirebaseBackendRepository(FirebaseFunctions.getInstance("us-central1"))

        val result = repository.ping()

        assertTrue("ping falló: ${result.exceptionOrNull()}", result.isSuccess)
        assertEquals("pong", result.getOrThrow().message)
    }
}
