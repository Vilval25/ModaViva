package pe.modaviva.app.data.remote

import com.google.firebase.Timestamp
import pe.modaviva.app.domain.model.ColorPrenda
import pe.modaviva.app.domain.model.Prenda
import java.time.Instant
import java.util.Date
import pe.modaviva.app.domain.model.MedidasPrenda

/**
 * Convierte un documento de `prendas` al modelo del dominio.
 *
 * Devuelve null si la ficha está incompleta: sin código, precio, tallas,
 * color, fotos o medidas de todas sus tallas. Esas prendas no se muestran
 * aunque estén publicadas (HU-04 CA-04).
 */
object PrendaMapper {

    fun desdeFirestore(id: String, data: Map<String, Any?>): Prenda? {
        val codigo = (data["codigo"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        val precio = (data["precio"] as? Number)?.toDouble()?.takeIf { it > 0 } ?: return null
        val tallas = (data["tallas"] as? List<*>)?.filterIsInstance<String>()
            ?.takeIf { it.isNotEmpty() } ?: return null
        val color = (data["color"] as? Map<*, *>)?.let(::color) ?: return null
        val fotos = (data["fotos"] as? List<*>)?.filterIsInstance<String>()
            ?.takeIf { it.isNotEmpty() } ?: return null
        val medidas = data["medidas"] as? Map<*, *> ?: return null
        val medidasCompletas = tallas.all { talla -> (medidas[talla] as? Map<*, *>)?.isNotEmpty() == true }
        if (!medidasCompletas) return null

        return Prenda(
            codigo = codigo,
            nombre = data["nombre"] as? String ?: return null,
            descripcion = data["descripcion"] as? String ?: "",
            marca = data["marca"] as? String ?: "",
            categoria = data["categoria"] as? String ?: "",
            subcategoria = data["subcategoria"] as? String ?: "",
            genero = data["genero"] as? String,
            precio = precio,
            precioPromo = (data["precioPromo"] as? Number)?.toDouble(),
            promoHasta = instante(data["promoHasta"]),
            color = color,
            tallas = tallas,
            medidas = convertirMedidas(medidas, tallas),
            fotos = fotos,
            stockTotal = (data["stockTotal"] as? Number)?.toInt() ?: 0,
            creadaEn = instante(data["creadaEn"]) ?: Instant.EPOCH,
            actualizadaEn = instante(data["actualizadaEn"]),
        ).takeIf { id == codigo }
    }

    private fun color(mapa: Map<*, *>): ColorPrenda? {
        val slug = (mapa["slug"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        val nombre = (mapa["nombre"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        return ColorPrenda(slug = slug, nombre = nombre, hex = mapa["hex"] as? String ?: "")
    }

    private fun convertirMedidas(
        mapa: Map<*, *>,
        tallasOrdenadas: List<String>,
    ): Map<String, MedidasPrenda> {
        val porTalla = mapa.mapNotNull { (tallaKey, valores) ->
            val talla = tallaKey as? String ?: return@mapNotNull null
            val datos = valores as? Map<*, *> ?: return@mapNotNull null

            val pecho = datos.medidaCm("pecho")
            val cintura = datos.medidaCm("cintura")
            val cadera = datos.medidaCm("cadera")
            val hombro = datos.medidaCm("hombro")
            val largo = datos.medidaCm("largo")
            val manga = datos.medidaCm("manga")

            val tieneAlgunValor = pecho > 0 || cintura > 0 || cadera > 0 || hombro > 0 || largo > 0 || manga > 0
            if (!tieneAlgunValor) return@mapNotNull null

            talla to MedidasPrenda(
                pecho = pecho,
                cintura = cintura,
                cadera = cadera,
                hombro = hombro,
                largo = largo,
                manga = manga,
            )
        }.toMap()

        if (porTalla.isEmpty()) return emptyMap()

        val ordenadas = linkedMapOf<String, MedidasPrenda>()
        for (talla in tallasOrdenadas) {
            porTalla[talla]?.let { ordenadas[talla] = it }
        }
        for ((talla, med) in porTalla) {
            ordenadas.putIfAbsent(talla, med)
        }
        return ordenadas
    }

    private fun Map<*, *>.medidaCm(clave: String): Double =
        (this[clave] as? Number)?.toDouble()?.takeIf { it > 0.0 } ?: 0.0


    private fun instante(valor: Any?): Instant? = when (valor) {
        is Timestamp -> valor.toDate().toInstant()
        is Date -> valor.toInstant()
        else -> null
    }
}
