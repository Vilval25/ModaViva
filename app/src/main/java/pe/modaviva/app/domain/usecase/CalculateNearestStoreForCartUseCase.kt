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
    val disponibleHoy: Boolean = true,
) {
    val plazoRecojoTexto: String
        get() = if (disponibleHoy) "Disponible hoy" else "Disponible en 24–48 h"
}

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

        val latDestino = destino?.lat
        val lngDestino = destino?.lng
        val destinoTieneCoordenadas = latDestino != null && lngDestino != null

        val hayStockGlobal = cartItems.all { item ->
            val stockVariante = stocksMap[item.varianteId]
            val stockTotal = maxOf(
                stockVariante?.total ?: 0,
                stockVariante?.porTienda?.values?.sum() ?: 0,
            )
            stockTotal >= item.cantidad
        }

        if (!hayStockGlobal) {
            return ResultadoTiendasRecojo(
                tiendasElegibles = emptyList(),
                tiendaRecomendada = null,
                requiereCoordenadasParaDistancia = !destinoTieneCoordenadas,
                hayStockSuficiente = false,
            )
        }

        // Si el stock global alcanza, todas las tiendas pueden entregar el pedido:
        // "Disponible hoy" si esa sede tiene todo; "Disponible en 24–48 h" si requiere reposición/traslado.
        val tiendasEvaluadas = tiendas.map { tienda ->
            val disponibleHoy = cartItems.all { item ->
                val stockEnTienda = stocksMap[item.varianteId]?.porTienda?.get(tienda.id) ?: 0
                stockEnTienda >= item.cantidad
            }
            val latTienda = tienda.lat
            val lngTienda = tienda.lng
            val distancia = if (
                latDestino != null && lngDestino != null &&
                latTienda != null && lngTienda != null
            ) {
                calcularDistanciaKm(latDestino, lngDestino, latTienda, lngTienda)
            } else {
                null
            }
            TiendaCartConDistancia(
                tienda = tienda,
                distanciaKm = distancia,
                disponibleHoy = disponibleHoy,
            )
        }

        val tiendasOrdenadas = tiendasEvaluadas.sortedWith(
            compareByDescending<TiendaCartConDistancia> { it.disponibleHoy }
                .thenBy { it.distanciaKm ?: Double.MAX_VALUE },
        )

        val tiendaRecomendada = tiendasOrdenadas.firstOrNull()?.copy(esRecomendada = true)
        val listaConRecomendada = tiendasOrdenadas.map { t ->
            if (tiendaRecomendada != null && t.tienda.id == tiendaRecomendada.tienda.id) {
                tiendaRecomendada
            } else {
                t
            }
        }

        return ResultadoTiendasRecojo(
            tiendasElegibles = listaConRecomendada,
            tiendaRecomendada = tiendaRecomendada,
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
