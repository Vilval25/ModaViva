package pe.modaviva.app.domain.usecase

import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.domain.model.RecomendacionTalla
import pe.modaviva.app.domain.model.UserProfile
import javax.inject.Inject
import kotlin.math.abs

/**
 * Caso de uso para calcular la recomendación de talla según altura y fotografía (HU-15).
 *
 * Criterios implementados:
 * - CA-01: Talla recomendada para la prenda seleccionada.
 * - CA-02: Reutilizable en el resultado del probador virtual y en la ficha del producto.
 * - CA-03: Si las medidas caen entre dos tallas, retorna ambas (ej. 'Entre M y L').
 * - CA-04: Aviso referencial visible en el resultado.
 * - CA-05: Si no hay datos suficientes o foto no permite pose, retorna fallback a tabla genérica.
 */
class RecommendSizeUseCase @Inject constructor() {

    operator fun invoke(
        prenda: Prenda,
        userProfile: UserProfile?,
    ): RecomendacionTalla {
        if (userProfile == null) return RecomendacionTalla.SinDatosSuficientes

        val altura = userProfile.alturaCm
        val fotoUrl = userProfile.fotoProbadorUrl

        if (altura == null || fotoUrl.isNullOrBlank()) {
            return RecomendacionTalla.SinDatosSuficientes
        }

        // Estimación antropométrica a partir de la pose y altura del cliente:
        val ratioHombro = userProfile.ratioHombro ?: 0.245
        val ratioCadera = userProfile.ratioCadera ?: 0.220

        val hombroEstimado = ratioHombro * altura
        val pechoEstimado = hombroEstimado * 2.18
        val cinturaEstimada = pechoEstimado * 0.80
        val caderaEstimada = ratioCadera * altura * 2.25

        if (prenda.tallas.isEmpty()) {
            return RecomendacionTalla.SinDatosSuficientes
        }

        // Si la prenda tiene tabla de medidas en cm por talla en Firestore
        if (prenda.medidas.isNotEmpty()) {
            val esParteInferior = prenda.categoria.contains("pantalon", ignoreCase = true) ||
                    prenda.categoria.contains("falda", ignoreCase = true) ||
                    prenda.categoria.contains("short", ignoreCase = true) ||
                    prenda.categoria.contains("jean", ignoreCase = true)

            val tallasConMedidas = prenda.tallas.filter { prenda.medidas.containsKey(it) }
            if (tallasConMedidas.isNotEmpty()) {
                val distancias = tallasConMedidas.map { talla ->
                    val med = prenda.medidas[talla]!!
                    val diff = if (esParteInferior && med.cintura > 0) {
                        abs(med.cintura - cinturaEstimada)
                    } else if (med.pecho > 0) {
                        abs(med.pecho - pechoEstimado)
                    } else if (med.cadera > 0) {
                        abs(med.cadera - caderaEstimada)
                    } else {
                        abs(med.hombro - hombroEstimado)
                    }
                    talla to diff
                }.sortedBy { it.second }

                val mejor = distancias[0]
                if (distancias.size > 1) {
                    val segunda = distancias[1]
                    // CA-03: Si la diferencia entre las dos tallas más cercanas es menor a 2 cm,
                    // el cliente queda entre dos tallas (ej. 'Entre M y L').
                    if (abs(segunda.second - mejor.second) <= 2.0) {
                        val index1 = prenda.tallas.indexOf(mejor.first)
                        val index2 = prenda.tallas.indexOf(segunda.first)
                        val (menor, mayor) = if (index1 < index2) {
                            mejor.first to segunda.first
                        } else {
                            segunda.first to mejor.first
                        }
                        return RecomendacionTalla.EntreDosTallas(
                            tallaMenor = menor,
                            tallaMayor = mayor,
                            pechoEstimado = pechoEstimado,
                            cinturaEstimada = cinturaEstimada,
                            caderaEstimada = caderaEstimada,
                        )
                    }
                }

                return RecomendacionTalla.TallaExacta(
                    talla = mejor.first,
                    pechoEstimado = pechoEstimado,
                    cinturaEstimada = cinturaEstimada,
                    caderaEstimada = caderaEstimada,
                )
            }
        }

        // Fallback antropométrico estándar por altura (S, M, L, XL, etc.)
        val tallaGenerica = when {
            altura < 158 -> "XS"
            altura in 158..167 -> "S"
            altura in 168..177 -> "M"
            altura in 178..186 -> "L"
            else -> "XL"
        }

        val tallaFinal = prenda.tallas.firstOrNull { it.equals(tallaGenerica, ignoreCase = true) }
            ?: prenda.tallas.firstOrNull { it.contains(tallaGenerica, ignoreCase = true) }
            ?: prenda.tallas[prenda.tallas.size / 2]

        return RecomendacionTalla.TallaExacta(
            talla = tallaFinal,
            pechoEstimado = pechoEstimado,
            cinturaEstimada = cinturaEstimada,
            caderaEstimada = caderaEstimada,
        )
    }
}

