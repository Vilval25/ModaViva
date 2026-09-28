package pe.modaviva.app.domain.repository

import kotlinx.coroutines.flow.StateFlow
import pe.modaviva.app.domain.model.UserProfile

interface SessionRepository {

    val currentUser: StateFlow<UserProfile?>

    fun signIn(profile: UserProfile)

    fun signOut()
}
