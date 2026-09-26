package pe.modaviva.app.domain.repository

import pe.modaviva.app.domain.model.RegisterRequest
import pe.modaviva.app.domain.model.UserProfile

interface AuthRepository {

    suspend fun isEmailTaken(email: String): Boolean

    suspend fun isDocumentoTaken(documento: String): Boolean

    suspend fun register(request: RegisterRequest): Result<UserProfile>
}
