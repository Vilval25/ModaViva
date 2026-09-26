package pe.modaviva.app.domain.model

data class UserProfile(
    val nombres: String,
    val apellidos: String,
    val documento: String,
    val telefono: String,
    val email: String,
)
