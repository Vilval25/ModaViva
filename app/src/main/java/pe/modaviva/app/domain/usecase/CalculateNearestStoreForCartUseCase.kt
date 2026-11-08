package pe.modaviva.app.domain.usecase

import pe.modaviva.app.domain.model.CartItem
import pe.modaviva.app.domain.model.DestinoEntrega
import pe.modaviva.app.domain.model.Stock
import pe.modaviva.app.domain.model.Tienda
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class TiendaCartConDistancia(
    val tienda: Tienda,
    val distanciaKm: Double?,
    val esRecomendada: Boolean = false,
)

data class ResultadoTiendasRecojo(
    val tiendasElegibles: List<TiendaCartConDistancia> = emptyList(),
    val tiendaRecomendada: TiendaCartConDistancia? = null,
    val requiereCoordenadasParaDistancia: Boolean = false,
    val hayStockSuficiente: Boolean = true,
)

class CalculateNearestStoreForCartUseCase @Inject constructor() {

    operator fun invoke(
        cartItems: List<CartItem>,
        stocksMap: Map<String, Stock>,
        tiendas: List<Tienda>,
        destino: DestinoEntrega?,
    ): ResultadoTiendasRecojo {
        if (cartItems.isEmpty() || tiendas.isEmpty()) {
            return ResultadoTiendasRecojo(hayStockSuficiente = true)
        }

        val tiendasElegibles = mutableListOf<TiendaCartConDistancia>()
        val latDestino = destino?.lat
        val lngDestino = destino?.lng
        val destinoTieneCoordenadas = latDestino != null && lngDestino != null

        for (tienda in tiendas) {
            var puedeAtenderTodo = true
            for (item in cartItems) {
                val stockVariante = stocksMap[item.varianteId]
                val stockEnTienda = stockVariante?.porTienda?.get(tienda.id) ?: 0
                if (stockEnTienda < item.cantidad) {
                    puedeAtenderTodo = false
                    break
                }
            }

            if (puedeAtenderTodo) {
                var distancia: Double? = null
                val latTienda = tienda.lat
                val lngTienda = tienda.lng
                if (destinoTieneCoordenadas && latDestino != null && lngDestino != null && latTienda != null && lngTienda != null) {
                    distancia = calcularDistanciaKm(latDestino, lngDestino, latTienda, lngTienda)
                }
                tiendasElegibles.add(
                    TiendaCartConDistancia(
                        tienda = tienda,
                        distanciaKm = distancia,
                    )
                )
            }
        }

        if (tiendasElegibles.isEmpty()) {
            return ResultadoTiendasRecojo(
                tiendasElegibles = emptyList(),
                tiendaRecomendada = null,
                requiereCoordenadasParaDistancia = !destinoTieneCoordenadas,
                hayStockSuficiente = false,
            )
        }

        var tiendaRecomendada: TiendaCartConDistancia? = null
        if (destinoTieneCoordenadas) {
            val conDistancia = tiendasElegibles.filter { it.distanciaKm != null }
            val masCercana = conDistancia.minByOrNull { it.distanciaKm!! }
            if (masCercana != null) {
                tiendaRecomendada = masCercana.copy(esRecomendada = true)
            }
        } else {
            tiendaRecomendada = tiendasElegibles.first().copy(esRecomendada = true)
        }

        val rec = tiendaRecomendada
        val listaConRecomendada = tiendasElegibles.map { t ->
            if (rec != null && t.tienda.id == rec.tienda.id) {
                rec
            } else {
                t
            }
        }

        return ResultadoTiendasRecojo(
            tiendasElegibles = listaConRecomendada,
            tiendaRecomendada = rec,
            requiereCoordenadasParaDistancia = !destinoTieneCoordenadas,
            hayStockSuficiente = true,
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
