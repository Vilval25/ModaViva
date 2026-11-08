package pe.modaviva.app.checkout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.modaviva.app.data.repository.CheckoutSessionRepository
import pe.modaviva.app.domain.model.DestinoEntrega
import pe.modaviva.app.domain.model.TipoDestino

class CheckoutSessionRepositoryTest {

    private val repository = CheckoutSessionRepository()

    @Test
    fun `repositorio comienza sin destino`() {
        assertNull(repository.destinoActual.value)
    }

    @Test
    fun `actualizar destino permite recuperarlo y editarlo`() {
        val destino = DestinoEntrega(
            tipo = TipoDestino.OTRA_DIRECCION,
            distrito = "Miraflores",
            distritoSlug = "miraflores",
            lat = -12.12,
            lng = -77.03,
        )
        repository.actualizarDestino(destino)
        assertEquals(destino, repository.destinoActual.value)

        val nuevoDestino = destino.copy(distrito = "San Isidro", distritoSlug = "san-isidro")
        repository.actualizarDestino(nuevoDestino)
        assertEquals("San Isidro", repository.destinoActual.value?.distrito)
        assertEquals(-12.12, repository.destinoActual.value?.lat)
    }

    @Test
    fun `limpiar destino restablece el valor a nulo`() {
        val destino = DestinoEntrega(distrito = "Surco")
        repository.actualizarDestino(destino)
        repository.limpiarDestino()
        assertNull(repository.destinoActual.value)
    }
}
