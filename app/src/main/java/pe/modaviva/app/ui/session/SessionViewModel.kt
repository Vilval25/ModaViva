package pe.modaviva.app.ui.session

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import pe.modaviva.app.domain.model.UserProfile
import pe.modaviva.app.domain.repository.SessionRepository
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    val currentUser: StateFlow<UserProfile?> = sessionRepository.currentUser

    fun signOut() = sessionRepository.signOut()
}
