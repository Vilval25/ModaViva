package pe.modaviva.app.domain.model

data class Tienda(
    val id: String = "",
    val nombre: String = "",
    val direccion: String = "",
    val distrito: String = "",
    val horario: String = "",
    val telefono: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
)
