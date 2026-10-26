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
    val isGoogleUser: Boolean = false,
    val consentimientoProbadorAceptado: Boolean = false,
    val consentimientoProbadorVersion: String? = null,
) {
    val tieneConsentimientoProbador: Boolean
        get() = consentimientoProbadorAceptado && consentimientoProbadorVersion == PROBADOR_AVISO_VERSION_ACTUAL

    companion object {
        const val PROBADOR_AVISO_VERSION_ACTUAL = "1.0"
    }
}
