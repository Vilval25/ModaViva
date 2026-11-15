package pe.modaviva.app.notifications

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.modaviva.app.domain.model.Notificacion
import pe.modaviva.app.domain.model.TipoNotificacion
import pe.modaviva.app.domain.model.cantidadNoLeidas
import pe.modaviva.app.domain.model.ordenadasRecientePrimero
import java.time.Instant

class NotificacionTest {

    private fun aviso(id: String, minuto: Long, leida: Boolean) = Notificacion(
        id = id,
        tipo = TipoNotificacion.PEDIDO,
        titulo = "Aviso $id",
        texto = "",
        creadaEn = Instant.parse("2026-11-15T10:00:00Z").plusSeconds(minuto * 60),
        leida = leida,
    )

    @Test
    fun `HU-18 CA-01 la bandeja va del mas reciente al mas antiguo`() {
        val lista = listOf(aviso("a", 1, true), aviso("c", 30, false), aviso("b", 10, false))
        assertEquals(listOf("c", "b", "a"), lista.ordenadasRecientePrimero().map { it.id })
    }

    @Test
    fun `HU-18 CA-02 la insignia cuenta solo los no leidos`() {
        val lista = listOf(aviso("a", 1, true), aviso("b", 2, false), aviso("c", 3, false))
        assertEquals(2, lista.cantidadNoLeidas())
    }

    @Test
    fun `HU-18 CA-03 con todo leido la insignia queda en cero`() {
        val lista = listOf(aviso("a", 1, true), aviso("b", 2, true))
        assertEquals(0, lista.cantidadNoLeidas())
        assertEquals(0, emptyList<Notificacion>().cantidadNoLeidas())
    }

    @Test
    fun `tipo desconocido o vacio se trata como aviso general`() {
        assertEquals(TipoNotificacion.PEDIDO, TipoNotificacion.desdeCodigo("pedido"))
        assertEquals(TipoNotificacion.GENERAL, TipoNotificacion.desdeCodigo("OTRO"))
        assertEquals(TipoNotificacion.GENERAL, TipoNotificacion.desdeCodigo(null))
    }
}
