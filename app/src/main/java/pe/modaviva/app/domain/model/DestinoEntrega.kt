package pe.modaviva.app.domain.model

enum class TipoDestino {
    DIRECCION_REGISTRADA,
    UBICACION_ACTUAL_GPS,
    OTRA_DIRECCION,
}

data class DestinoEntrega(
    val tipo: TipoDestino = TipoDestino.OTRA_DIRECCION,
    val direccion: String = "",
    val referencia: String? = null,
    val distrito: String = "",
    val distritoSlug: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
) {
    val estaCompleto: Boolean
        get() = distrito.isNotBlank() || (lat != null && lng != null)
}
