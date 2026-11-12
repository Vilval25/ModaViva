package pe.modaviva.app.domain.model

import java.text.Normalizer
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class DistritoCoordenada(
    val id: String,
    val distrito: String,
    val lat: Double,
    val lng: Double,
    val aliases: List<String> = emptyList(),
)

/**
 * Catálogo de referencia de los distritos oficiales de Lima Metropolitana con sus
 * coordenadas geográficas y alias comunes.
 *
 * Permite resolver el distrito tanto por texto (Geocoder de Android, nombres o urbanizaciones)
 * como por proximidad geográfica (coordenadas GPS directas), garantizando que nunca falle
 * la detección del distrito ni el cálculo de distancia a la tienda más cercana.
 */
object DistritoResolver {

    val DISTRITOS_LIMA = listOf(
        DistritoCoordenada(
            id = "miraflores",
            distrito = "Miraflores",
            lat = -12.1219,
            lng = -77.0305,
            aliases = listOf("miraflores"),
        ),
        DistritoCoordenada(
            id = "san-isidro",
            distrito = "San Isidro",
            lat = -12.0975,
            lng = -77.0365,
            aliases = listOf("san isidro", "san-isidro"),
        ),
        DistritoCoordenada(
            id = "surco",
            distrito = "Santiago de Surco",
            lat = -12.1350,
            lng = -76.9920,
            aliases = listOf("santiago de surco", "surco", "chama", "monterrico", "chacarilla", "higuereta", "surco viejo", "surco pueblo"),
        ),
        DistritoCoordenada(
            id = "san-borja",
            distrito = "San Borja",
            lat = -12.0950,
            lng = -77.0010,
            aliases = listOf("san borja", "san-borja", "chacarilla del estanque", "torres de san borja"),
        ),
        DistritoCoordenada(
            id = "barranco",
            distrito = "Barranco",
            lat = -12.1480,
            lng = -77.0210,
            aliases = listOf("barranco"),
        ),
        DistritoCoordenada(
            id = "la-molina",
            distrito = "La Molina",
            lat = -12.0850,
            lng = -76.9380,
            aliases = listOf("la molina", "la-molina", "camacho", "rinconada", "sol de la molina"),
        ),
        DistritoCoordenada(
            id = "jesus-maria",
            distrito = "Jesús María",
            lat = -12.0730,
            lng = -77.0480,
            aliases = listOf("jesus maria", "jesus-maria"),
        ),
        DistritoCoordenada(
            id = "magdalena",
            distrito = "Magdalena del Mar",
            lat = -12.0910,
            lng = -77.0690,
            aliases = listOf("magdalena del mar", "magdalena"),
        ),
        DistritoCoordenada(
            id = "san-miguel",
            distrito = "San Miguel",
            lat = -12.0780,
            lng = -77.0870,
            aliases = listOf("san miguel", "san-miguel", "maranga"),
        ),
        DistritoCoordenada(
            id = "lince",
            distrito = "Lince",
            lat = -12.0830,
            lng = -77.0340,
            aliases = listOf("lince"),
        ),
        DistritoCoordenada(
            id = "pueblo-libre",
            distrito = "Pueblo Libre",
            lat = -12.0760,
            lng = -77.0630,
            aliases = listOf("pueblo libre", "pueblo-libre", "magdalena vieja"),
        ),
        DistritoCoordenada(
            id = "lima-cercado",
            distrito = "Lima Cercado",
            lat = -12.0460,
            lng = -77.0310,
            aliases = listOf("lima cercado", "cercado de lima", "cercado", "centro de lima", "lima centro"),
        ),
    )

    fun normalizar(texto: String): String {
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .lowercase(Locale("es", "PE"))
            .replace("[^a-z0-9\\s]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    /**
     * Obtiene las coordenadas centrales de un distrito o slug.
     */
    fun coordenadasParaDistrito(distritoOSlug: String): Pair<Double, Double>? {
        if (distritoOSlug.isBlank()) return null
        val norm = normalizar(distritoOSlug)
        val match = DISTRITOS_LIMA.firstOrNull { d ->
            normalizar(d.id) == norm ||
                    normalizar(d.distrito) == norm ||
                    d.aliases.any { normalizar(it) == norm } ||
                    norm.contains(normalizar(d.distrito)) ||
                    normalizar(d.distrito).contains(norm)
        }
        return match?.let { it.lat to it.lng }
    }

    /**
     * Resuelve un distrito por coincidencia textual contra la lista de alias o nombres oficiales.
     */
    fun resolverPorTexto(candidatos: List<String>): DistritoCoordenada? {
        for (candidato in candidatos) {
            val norm = normalizar(candidato)
            if (norm.isBlank()) continue

            // 1. Coincidencia exacta con ID, nombre o alias
            val exacto = DISTRITOS_LIMA.firstOrNull { d ->
                normalizar(d.id) == norm ||
                        normalizar(d.distrito) == norm ||
                        d.aliases.any { normalizar(it) == norm }
            }
            if (exacto != null) return exacto

            // 2. Coincidencia por contención (e.g. "Av. Benavides, Surco 15038")
            val porContencion = DISTRITOS_LIMA.firstOrNull { d ->
                val normDistrito = normalizar(d.distrito)
                norm.contains(normDistrito) || d.aliases.any { alias ->
                    val normAlias = normalizar(alias)
                    normAlias.length >= 4 && norm.contains(normAlias)
                }
            }
            if (porContencion != null) return porContencion
        }
        return null
    }

    /**
     * Resuelve el distrito más cercano geográficamente comparando la distancia Haversine a los centros distritales.
     */
    fun resolverPorCoordenadas(lat: Double, lng: Double): DistritoCoordenada {
        return DISTRITOS_LIMA.minBy { d ->
            calcularDistanciaKm(lat, lng, d.lat, d.lng)
        }
    }

    /**
     * Estrategia de resolución integral:
     * 1. Revisa componentes de dirección y dirección completa.
     * 2. Si no halla coincidencia textual y hay coordenadas (lat, lng), usa proximidad geográfica.
     * 3. Sincroniza con la lista de tarifas activas en Firestore para conservar costos y plazos reales.
     */
    fun resolverTarifa(
        componentes: List<String>,
        direccionCompleta: String? = null,
        lat: Double? = null,
        lng: Double? = null,
        tarifasDisponibles: List<TarifaEnvio> = emptyList(),
    ): TarifaEnvio? {
        val listaCandidatos = mutableListOf<String>()
        listaCandidatos.addAll(componentes)
        if (!direccionCompleta.isNullOrBlank()) {
            listaCandidatos.add(direccionCompleta)
            listaCandidatos.addAll(direccionCompleta.split(",").map { it.trim() })
        }

        // 1. Resolver por texto contra distritos oficiales
        val distritoDetectado = resolverPorTexto(listaCandidatos)
        if (distritoDetectado != null) {
            val tarifaMatch = tarifasDisponibles.firstOrNull {
                it.id.equals(distritoDetectado.id, ignoreCase = true) ||
                        it.distrito.equals(distritoDetectado.distrito, ignoreCase = true)
            }
            if (tarifaMatch != null) {
                return tarifaMatch.copy(
                    lat = lat ?: tarifaMatch.lat ?: distritoDetectado.lat,
                    lng = lng ?: tarifaMatch.lng ?: distritoDetectado.lng,
                )
            }
            return TarifaEnvio(
                id = distritoDetectado.id,
                distrito = distritoDetectado.distrito,
                cobertura = true,
                costo = 10.0,
                plazoDias = 3,
                lat = lat ?: distritoDetectado.lat,
                lng = lng ?: distritoDetectado.lng,
            )
        }

        // 2. Coincidencia directa contra tarifasDisponibles
        for (cand in listaCandidatos) {
            val norm = normalizar(cand)
            if (norm.isBlank()) continue
            val tMatch = tarifasDisponibles.firstOrNull { t ->
                val normT = normalizar(t.distrito)
                norm == normT || (normT.length >= 4 && norm.contains(normT))
            }
            if (tMatch != null) {
                val coords = coordenadasParaDistrito(tMatch.distrito)
                return tMatch.copy(
                    lat = lat ?: tMatch.lat ?: coords?.first,
                    lng = lng ?: tMatch.lng ?: coords?.second,
                )
            }
        }

        // 3. Fallback geográfico por coordenadas GPS
        if (lat != null && lng != null) {
            val masCercano = resolverPorCoordenadas(lat, lng)
            val tarifaMatch = tarifasDisponibles.firstOrNull {
                it.id.equals(masCercano.id, ignoreCase = true) ||
                        it.distrito.equals(masCercano.distrito, ignoreCase = true)
            }
            if (tarifaMatch != null) {
                return tarifaMatch.copy(
                    lat = lat,
                    lng = lng,
                )
            }
            return TarifaEnvio(
                id = masCercano.id,
                distrito = masCercano.distrito,
                cobertura = true,
                costo = 10.0,
                plazoDias = 3,
                lat = lat,
                lng = lng,
            )
        }

        return null
    }

    fun calcularDistanciaKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
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
