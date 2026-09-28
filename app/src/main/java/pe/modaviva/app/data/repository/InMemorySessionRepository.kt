package pe.modaviva.app.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.modaviva.app.domain.model.UserProfile
import pe.modaviva.app.domain.repository.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sesión temporal en memoria. Se reemplaza por una implementación sobre
 * DataStore cuando se integre Firebase Auth, para conservar la sesión entre
 *arranques de la app.
 */
@Singleton
class InMemorySessionRepository @Inject constructor() : SessionRepository {

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    override fun signIn(profile: UserProfile) {
        _currentUser.value = profile
    }

    override fun signOut() {
        _currentUser.value = null
    }
}
