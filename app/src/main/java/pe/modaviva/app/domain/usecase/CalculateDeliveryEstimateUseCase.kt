package pe.modaviva.app.domain.usecase

import pe.modaviva.app.domain.model.DestinoEntrega
import pe.modaviva.app.domain.model.EstimacionEntrega
import pe.modaviva.app.domain.model.TarifaEnvio
import java.text.Normalizer
import javax.inject.Inject

/**
 * Caso de uso del dominio para calcular la estimación logística de entrega a domicilio (HU-07).
 *
 * Responsabilidad: Recibe un [DestinoEntrega] y la lista de [TarifaEnvio] registradas en la plataforma,
 * identifica la tarifa correspondiente al distrito y calcula el plazo estimado agregando el margen
 * de seguridad de +2 a +3 días hábiles.
 */
class CalculateDeliveryEstimateUseCase @Inject constructor() {

    operator fun invoke(
        destino: DestinoEntrega,
        tarifas: List<TarifaEnvio>,
    ): EstimacionEntrega {
        val distritoRaw = destino.distrito.ifBlank { destino.distritoSlug }
        if (distritoRaw.isBlank()) {
            return EstimacionEntrega(
                tieneCobertura = false,
                tarifaRegistrada = false,
                costoEnvio = 0.0,
                plazoMinDias = 0,
                plazoMaxDias = 0,
                mensajeInformativo = "Selecciona o confirma un distrito de entrega",
            )
        }

        val distritoNormalizado = normalizar(distritoRaw)

        // Buscar coincidencia en la lista de tarifas oficiales por ID, slug o nombre de distrito
        val tarifaEncontrada = tarifas.firstOrNull { tarifa ->
            normalizar(tarifa.id) == distritoNormalizado ||
                    normalizar(tarifa.distrito) == distritoNormalizado
        }

        if (tarifaEncontrada == null) {
            return EstimacionEntrega(
                tieneCobertura = false,
                tarifaRegistrada = false,
                costoEnvio = 0.0,
                plazoMinDias = 0,
                plazoMaxDias = 0,
                mensajeInformativo = "Sin tarifa registrada para el distrito ingresado ($distritoRaw)",
            )
        }

        if (!tarifaEncontrada.cobertura) {
            return EstimacionEntrega(
                tieneCobertura = false,
                tarifaRegistrada = true,
                costoEnvio = 0.0,
                plazoMinDias = 0,
                plazoMaxDias = 0,
                mensajeInformativo = "Sin cobertura de entrega a domicilio para ${tarifaEncontrada.distrito}",
            )
        }

        // Regla logística: Plazo base de Firestore + Margen de Seguridad (+2 a +3 días hábiles)
        val plazoBase = tarifaEncontrada.plazoDias
        val plazoMinimo = plazoBase + MARGEN_SEGURIDAD_MINIMO_DIAS
        val plazoMaximo = plazoBase + MARGEN_SEGURIDAD_MAXIMO_DIAS

        return EstimacionEntrega(
            tieneCobertura = true,
            tarifaRegistrada = true,
            costoEnvio = tarifaEncontrada.costo,
            plazoMinDias = plazoMinimo,
            plazoMaxDias = plazoMaximo,
            mensajeInformativo = null,
            esProvisional = false,
        )
    }

    private fun normalizar(texto: String): String {
        val sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return sinAcentos
            .lowercase()
            .trim()
            .replace(" ", "-")
            .replace("_", "-")
    }

    companion object {
        const val MARGEN_SEGURIDAD_MINIMO_DIAS = 2
        const val MARGEN_SEGURIDAD_MAXIMO_DIAS = 3
    }
}
