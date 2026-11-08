package pe.modaviva.app.domain.model

data class EstimacionEntrega(
    val tieneCobertura: Boolean = false,
    val tarifaRegistrada: Boolean = false,
    val costoEnvio: Double = 0.0,
    val plazoMinDias: Int = 0,
    val plazoMaxDias: Int = 0,
    val mensajeInformativo: String? = null,
    val esProvisional: Boolean = false,
) {
    val rangoDiasTexto: String
        get() = when {
            !tarifaRegistrada -> "Sin tarifa registrada para este distrito"
            !tieneCobertura -> "Sin cobertura de entrega a domicilio"
            plazoMinDias == plazoMaxDias -> "$plazoMinDias días hábiles"
            else -> "$plazoMinDias a $plazoMaxDias días hábiles"
        }
}
