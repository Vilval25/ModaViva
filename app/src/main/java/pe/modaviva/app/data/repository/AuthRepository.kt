package pe.modaviva.app.data.repository

import pe.modaviva.app.data.model.RegisterRequest
import pe.modaviva.app.data.model.UserProfile

interface AuthRepository {

    suspend fun isEmailTaken(email: String): Boolean

    suspend fun isDocumentoTaken(documento: String): Boolean

    suspend fun register(request: RegisterRequest): Result<UserProfile>
}
