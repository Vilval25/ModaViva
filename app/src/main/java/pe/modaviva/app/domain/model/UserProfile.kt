package pe.modaviva.app.domain.model

data class UserProfile(
    val uid: String = "",
    val nombres: String,
    val apellidos: String,
    val email: String,
    val telefono: String? = null,
    val documento: String = "",
    val emailVerificado: Boolean = false,
    val consentimientoAceptado: Boolean = false,
)
