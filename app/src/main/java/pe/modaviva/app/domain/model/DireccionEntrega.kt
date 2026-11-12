package pe.modaviva.app.domain.model

data class DireccionEntrega(
    val id: String = "",
    val alias: String = "", // e.g. "Casa", "Trabajo"
    val destinatario: String = "",
    val telefono: String = "",
    val direccion: String = "",
    val referencia: String? = null,
    val distrito: String = "",
    val distritoSlug: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val esPrincipal: Boolean = false,
) {
    val direccionCompleta: String
        get() = if (referencia.isNullOrBlank()) "$direccion, $distrito" else "$direccion (Ref: $referencia), $distrito"
}

fun DireccionEntrega.aDestinoEntrega(): DestinoEntrega {
    val coords = if (lat != null && lng != null) {
        lat to lng
    } else {
        DistritoResolver.coordenadasParaDistrito(distrito.ifBlank { distritoSlug })
    }
    return DestinoEntrega(
        tipo = TipoDestino.DIRECCION_REGISTRADA,
        direccion = direccion,
        referencia = referencia,
        distrito = distrito,
        distritoSlug = distritoSlug,
        lat = coords?.first,
        lng = coords?.second,
    )
}
