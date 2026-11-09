package pe.modaviva.app.fitting

import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.modaviva.app.data.repository.EliminacionFotoFirestore
import pe.modaviva.app.domain.model.ConstanciaEliminacionFoto
import pe.modaviva.app.domain.model.UserProfile
import pe.modaviva.app.domain.model.sinFotoProbador
import pe.modaviva.app.domain.repository.FittingRepository
import pe.modaviva.app.ui.fitting.EliminacionDeFoto
import pe.modaviva.app.ui.fitting.EstadoEliminacion
import pe.modaviva.app.ui.fitting.MotivoFallo
import pe.modaviva.app.ui.fitting.fechaConstancia
import java.time.Instant
import java.time.ZoneId

/** HU-17: Eliminación de la fotografía. */
@OptIn(ExperimentalCoroutinesApi::class)
class EliminacionDeFotoTest {

    private val momento = Instant.parse("2026-11-09T20:42:00Z")

    private class RepositorioFalso(
        private val resultado: Result<ConstanciaEliminacionFoto>,
    ) : FittingRepository {
        var eliminaciones = 0

        override suspend fun deleteFittingPhoto(): Result<ConstanciaEliminacionFoto> {
            eliminaciones++
            return resultado
        }

        override suspend fun validateImageMetadata(uri: Uri) = error("no se usa")
        override suspend fun validatePose(bitmap: Bitmap) = error("no se usa")
        override suspend fun uploadFittingPhoto(bitmap: Bitmap, onProgress: (Float) -> Unit) = error("no se usa")
        override suspend fun updateHeight(heightCm: Int) = error("no se usa")
    }

    private fun TestScope.flujo(
        repositorio: FittingRepository,
        conectado: Boolean = true,
    ) = EliminacionDeFoto(repositorio, hayConexion = { conectado }, scope = this)

    private fun exito() = RepositorioFalso(Result.success(ConstanciaEliminacionFoto(momento)))

    @Test
    fun `CA-01 tocar eliminar abre la confirmacion`() = runTest {
        val eliminacion = flujo(exito())
        eliminacion.solicitar()
        assertEquals(EstadoEliminacion.Confirmando, eliminacion.estado.value)
    }

    @Test
    fun `CA-02 si cancelo no se elimina nada`() = runTest {
        val repositorio = exito()
        val eliminacion = flujo(repositorio)

        eliminacion.solicitar()
        eliminacion.cancelar()
        advanceUntilIdle()

        assertEquals(EstadoEliminacion.Inactiva, eliminacion.estado.value)
        assertEquals(0, repositorio.eliminaciones)
    }

    @Test
    fun `CA-03 al confirmar se muestra la constancia con la fecha y hora`() = runTest {
        val repositorio = exito()
        val eliminacion = flujo(repositorio)

        eliminacion.solicitar()
        eliminacion.confirmar()
        assertEquals(EstadoEliminacion.Eliminando, eliminacion.estado.value)
        advanceUntilIdle()

        assertEquals(EstadoEliminacion.Hecha(ConstanciaEliminacionFoto(momento)), eliminacion.estado.value)
        assertEquals(1, repositorio.eliminaciones)
    }

    @Test
    fun `CA-03 la constancia muestra fecha y hora en espanol y en la hora del telefono`() {
        assertEquals(
            "9 de noviembre de 2026, 15:42",
            fechaConstancia(momento, ZoneId.of("America/Lima")),
        )
    }

    @Test
    fun `confirmar dos veces elimina una sola vez`() = runTest {
        val repositorio = exito()
        val eliminacion = flujo(repositorio)

        eliminacion.solicitar()
        eliminacion.confirmar()
        eliminacion.confirmar()
        advanceUntilIdle()

        assertEquals(1, repositorio.eliminaciones)
    }

    @Test
    fun `confirmar sin haber pasado por el dialogo no elimina`() = runTest {
        val repositorio = exito()
        val eliminacion = flujo(repositorio)

        eliminacion.confirmar()
        advanceUntilIdle()

        assertEquals(EstadoEliminacion.Inactiva, eliminacion.estado.value)
        assertEquals(0, repositorio.eliminaciones)
    }

    @Test
    fun `sin conexion no se intenta eliminar y se avisa`() = runTest {
        val repositorio = exito()
        val eliminacion = flujo(repositorio, conectado = false)

        eliminacion.solicitar()
        eliminacion.confirmar()
        advanceUntilIdle()

        assertEquals(EstadoEliminacion.Fallida(MotivoFallo.SIN_CONEXION), eliminacion.estado.value)
        assertEquals(0, repositorio.eliminaciones)
    }

    @Test
    fun `si falla se avisa y se puede reintentar`() = runTest {
        val repositorio = RepositorioFalso(Result.failure(RuntimeException("storage")))
        val eliminacion = flujo(repositorio)

        eliminacion.solicitar()
        eliminacion.confirmar()
        advanceUntilIdle()
        assertEquals(EstadoEliminacion.Fallida(MotivoFallo.ERROR), eliminacion.estado.value)

        eliminacion.solicitar()
        assertEquals(EstadoEliminacion.Confirmando, eliminacion.estado.value)
    }

    @Test
    fun `cerrar la constancia vuelve al probador`() = runTest {
        val eliminacion = flujo(exito())
        eliminacion.solicitar()
        eliminacion.confirmar()
        advanceUntilIdle()

        eliminacion.cerrar()

        assertEquals(EstadoEliminacion.Inactiva, eliminacion.estado.value)
    }

    @Test
    fun `CA-05 sin foto el probador vuelve a pedir una nueva`() {
        val perfil = perfilConFoto().sinFotoProbador()

        assertNull(perfil.fotoProbadorUrl)
        assertNull(perfil.ratioHombro)
        assertNull(perfil.ratioCadera)
    }

    @Test
    fun `CA-06 la cuenta se conserva`() {
        val antes = perfilConFoto()
        val despues = antes.sinFotoProbador()

        assertEquals(antes.uid, despues.uid)
        assertEquals(antes.nombres, despues.nombres)
        assertEquals(antes.apellidos, despues.apellidos)
        assertEquals(antes.email, despues.email)
        assertEquals(antes.telefono, despues.telefono)
        assertEquals(antes.documento, despues.documento)
        assertEquals(antes.alturaCm, despues.alturaCm)
        assertTrue(despues.tieneConsentimientoProbador)
    }

    @Test
    fun `CA-06 en Firestore solo se borran campos de la foto del probador`() {
        val camposDeLaFoto = setOf("fotoProbadorUrl", "ratioHombro", "ratioCadera")
        EliminacionFotoFirestore.CAMPOS_BORRADOS.forEach { campo ->
            assertTrue(campo, campo.startsWith("probador.") || campo in camposDeLaFoto)
        }
        assertFalse("probador.alturaCm" in EliminacionFotoFirestore.CAMPOS_BORRADOS)
        assertFalse("alturaCm" in EliminacionFotoFirestore.CAMPOS_BORRADOS)
    }

    private fun perfilConFoto() = UserProfile(
        uid = "cliente_1",
        nombres = "Ana",
        apellidos = "García",
        email = "ana@modaviva.pe",
        telefono = "987654321",
        documento = "12345678",
        consentimientoProbadorAceptado = true,
        consentimientoProbadorVersion = UserProfile.PROBADOR_AVISO_VERSION_ACTUAL,
        fotoProbadorUrl = "https://example.com/foto.webp",
        alturaCm = 165,
        ratioHombro = 0.25,
        ratioCadera = 0.22,
    )
}
