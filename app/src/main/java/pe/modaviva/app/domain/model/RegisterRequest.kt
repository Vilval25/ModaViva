package pe.modaviva.app.domain.model

data class RegisterRequest(
    val nombres: String,
    val apellidos: String,
    val email: String,
    val password: String,
    val aceptaTerminos: Boolean = false,
) {
    fun toProfile(uid: String = "") = UserProfile(
        uid = uid,
        nombres = nombres,
        apellidos = apellidos,
        email = email,
        telefono = null,
        emailVerificado = false,
    )
}
