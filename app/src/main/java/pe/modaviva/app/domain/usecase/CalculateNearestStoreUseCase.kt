package pe.modaviva.app.domain.usecase

import pe.modaviva.app.domain.model.DestinoEntrega
import pe.modaviva.app.domain.model.Stock
import pe.modaviva.app.domain.model.Tienda
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class TiendaConDistancia(
    val tienda: Tienda,
    val stockCantidad: Int,
    val distanciaKm: Double?,
)

data class ResultadoTiendaCercana(
    val tiendasConStock: List<TiendaConDistancia> = emptyList(),
    val tiendaRecomendada: TiendaConDistancia? = null,
    val requiereCoordenadasParaDistancia: Boolean = false,
)

class CalculateNearestStoreUseCase @Inject constructor() {

    operator fun invoke(
        destino: DestinoEntrega?,
        stock: Stock?,
        tiendas: List<Tienda>,
    ): ResultadoTiendaCercana {
        if (stock == null || stock.porTienda.isEmpty()) {
            return ResultadoTiendaCercana()
        }

        val tiendasConStockIds = stock.porTienda.filterValues { it > 0 }
        if (tiendasConStockIds.isEmpty()) {
            return ResultadoTiendaCercana()
        }

        val tiendasMap = tiendas.associateBy { it.id }
        val listaConStock = mutableListOf<TiendaConDistancia>()

        val destinoTieneCoordenadas = destino?.lat != null && destino.lng != null

        for ((tiendaId, cantidad) in tiendasConStockIds) {
            val tienda = tiendasMap[tiendaId] ?: continue
            var distancia: Double? = null

            if (destinoTieneCoordenadas && tienda.lat != null && tienda.lng != null) {
                distancia = calcularDistanciaKm(destino.lat!!, destino.lng!!, tienda.lat, tienda.lng)
            }

            listaConStock.add(
                TiendaConDistancia(
                    tienda = tienda,
                    stockCantidad = cantidad,
                    distanciaKm = distancia,
                )
            )
        }

        if (listaConStock.isEmpty()) {
            return ResultadoTiendaCercana()
        }

        if (!destinoTieneCoordenadas) {
            return ResultadoTiendaCercana(
                tiendasConStock = listaConStock,
                tiendaRecomendada = null,
                requiereCoordenadasParaDistancia = true,
            )
        }

        val conDistancia = listaConStock.filter { it.distanciaKm != null }
        val tiendaRecomendada = conDistancia.minByOrNull { it.distanciaKm!! }

        return ResultadoTiendaCercana(
            tiendasConStock = listaConStock,
            tiendaRecomendada = tiendaRecomendada,
            requiereCoordenadasParaDistancia = false,
        )
    }

    private fun calcularDistanciaKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val radioTierraKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return radioTierraKm * c
    }
}
