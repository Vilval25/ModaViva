package pe.modaviva.app.data.repository

import kotlinx.coroutines.delay
import pe.modaviva.app.domain.model.RegisterRequest
import pe.modaviva.app.domain.model.UserProfile
import pe.modaviva.app.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación temporal en memoria. Se reemplaza por FirebaseAuthRepository
 * en la fase de integración con Firebase (HU-01).
 */
@Singleton
class FakeAuthRepository @Inject constructor() : AuthRepository {

    private val webCustomers = mutableListOf(
        UserProfile(
            nombres = "Lucia",
            apellidos = "Quispe Mamani",
            documento = "40125478",
            telefono = "987654321",
            email = "lucia.quispe@modaviva.pe",
        ),
        UserProfile(
            nombres = "Carlos",
            apellidos = "Flores Rojas",
            documento = "45230196",
            telefono = "912345678",
            email = "carlos.flores@modaviva.pe",
        ),
        UserProfile(
            nombres = "Ana",
            apellidos = "Huaman Torres",
            documento = "70981532",
            telefono = "955667788",
            email = "ana.huaman@modaviva.pe",
        ),
    )

    override suspend fun isEmailTaken(email: String): Boolean =
        webCustomers.any { it.email.equals(email.trim(), ignoreCase = true) }

    override suspend fun isDocumentoTaken(documento: String): Boolean =
        webCustomers.any { it.documento == documento.trim() }

    override suspend fun register(request: RegisterRequest): Result<UserProfile> {
        delay(NETWORK_DELAY_MS)
        val profile = request.toProfile()
        webCustomers += profile
        return Result.success(profile)
    }

    private companion object {
        const val NETWORK_DELAY_MS = 600L
    }
}
