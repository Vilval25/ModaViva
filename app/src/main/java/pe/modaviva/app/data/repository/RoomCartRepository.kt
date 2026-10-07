package pe.modaviva.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.modaviva.app.data.local.CartDao
import pe.modaviva.app.data.local.CartEntity
import pe.modaviva.app.data.local.aDominio
import pe.modaviva.app.domain.model.CartItem
import pe.modaviva.app.domain.repository.CartRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomCartRepository @Inject constructor(
    private val cartDao: CartDao,
) : CartRepository {

    override val items: Flow<List<CartItem>> =
        cartDao.observarCarrito().map { list -> list.map { it.aDominio() } }

    override val totalItems: Flow<Int> =
        cartDao.observarTotalItems()

    override suspend fun agregarAlCarrito(
        prendaId: String,
        talla: String,
        colorNombre: String,
        colorHex: String,
        cantidad: Int,
        precioAlAgregar: Double,
    ) {
        val varianteId = CartItem.generarVarianteId(prendaId, talla)
        val entity = CartEntity(
            varianteId = varianteId,
            prendaId = prendaId,
            talla = talla,
            colorNombre = colorNombre,
            colorHex = colorHex,
            cantidad = cantidad,
            agregadoEnMillis = Instant.now().toEpochMilli(),
            precioAlAgregar = precioAlAgregar,
        )
        cartDao.agregarOIncrementar(entity)
    }

    override suspend fun actualizarCantidad(varianteId: String, cantidad: Int) {
        if (cantidad <= 0) {
            eliminarItem(varianteId)
        } else {
            cartDao.actualizarCantidad(varianteId, cantidad)
        }
    }

    override suspend fun eliminarItem(varianteId: String) {
        cartDao.eliminarItem(varianteId)
    }

    override suspend fun vaciarCarrito() {
        cartDao.vaciarCarrito()
    }

    override suspend fun sincronizarConCuenta(uid: String) {
        // Implementado en Fase 5
    }
}
