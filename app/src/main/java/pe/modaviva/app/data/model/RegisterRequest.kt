package pe.modaviva.app.data.model

data class RegisterRequest(
    val nombres: String,
    val apellidos: String,
    val documento: String,
    val telefono: String,
    val email: String,
    val password: String,
) {
    fun toProfile() = UserProfile(
        nombres = nombres,
        apellidos = apellidos,
        documento = documento,
        telefono = telefono,
        email = email,
    )
}
