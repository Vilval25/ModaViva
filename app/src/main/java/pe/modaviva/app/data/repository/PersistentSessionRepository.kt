package pe.modaviva.app.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import pe.modaviva.app.data.remote.await
import pe.modaviva.app.domain.model.UserProfile
import pe.modaviva.app.domain.repository.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio de sesión persistente.
 *
 * Mantiene la sesión del usuario activa entre aperturas de la app combinando:
 * 1. SharedPreferences: Carga y restauración instantánea del perfil sin parpadeos visuales ni espera de red.
 * 2. Firebase Auth: Persistencia del token de autenticación del usuario.
 * 3. Cloud Firestore: Sincronización en segundo plano de datos actualizados del cliente.
 */
@Singleton
class PersistentSessionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : SessionRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    init {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null) {
            val cached = loadCachedProfile()
            if (cached != null && cached.uid == firebaseUser.uid) {
                _currentUser.value = cached
            }
            // Sincronizar en segundo plano con Firestore
            scope.launch {
                syncProfileWithRemote(firebaseUser.uid)
            }
        } else {
            clearCachedProfile()
        }

        firebaseAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user == null) {
                clearCachedProfile()
                _currentUser.value = null
            } else if (_currentUser.value?.uid != user.uid) {
                val cached = loadCachedProfile()
                if (cached != null && cached.uid == user.uid) {
                    _currentUser.value = cached
                }
                scope.launch {
                    syncProfileWithRemote(user.uid)
                }
            }
        }
    }

    override fun signIn(profile: UserProfile) {
        saveCachedProfile(profile)
        _currentUser.value = profile
    }

    override fun signOut() {
        clearCachedProfile()
        _currentUser.value = null
        if (firebaseAuth.currentUser != null) {
            firebaseAuth.signOut()
        }
    }

    private suspend fun syncProfileWithRemote(uid: String) {
        try {
            val firebaseUser = firebaseAuth.currentUser ?: return
            runCatching { firebaseUser.reload().await() }

            val snapshot = firestore.collection(CLIENTES).document(uid).get().await()
            if (!snapshot.exists()) {
                signOut()
                return
            }

            val data = snapshot.data.orEmpty()
            val isVerified = firebaseUser.isEmailVerified
            val isGoogleUser = firebaseUser.providerData.any { it.providerId == "google.com" }
            val consentimiento = data["consentimiento"] as? Map<*, *>
            val tieneConsentimiento =
                consentimiento != null && !consentimiento["version"]?.toString().isNullOrBlank()

            val remoteProfile = UserProfile(
                uid = uid,
                nombres = data["nombres"] as? String ?: "",
                apellidos = data["apellidos"] as? String ?: "",
                documento = data["documento"] as? String ?: "",
                telefono = data["telefono"] as? String,
                email = data["email"] as? String ?: firebaseUser.email.orEmpty(),
                emailVerificado = isVerified,
                consentimientoAceptado = tieneConsentimiento,
                isGoogleUser = isGoogleUser,
            )

            saveCachedProfile(remoteProfile)
            _currentUser.value = remoteProfile
        } catch (_: Exception) {
            // Sin conexión o error transitorio de red: se conserva el perfil en caché
        }
    }

    private fun loadCachedProfile(): UserProfile? {
        val raw = prefs.getString(KEY_PROFILE, null) ?: return null
        return raw.toUserProfile()
    }

    private fun saveCachedProfile(profile: UserProfile) {
        prefs.edit().putString(KEY_PROFILE, profile.toJsonString()).apply()
    }

    private fun clearCachedProfile() {
        prefs.edit().remove(KEY_PROFILE).apply()
    }

    private companion object {
        const val PREFS_NAME = "pe.modaviva.app.session"
        const val KEY_PROFILE = "current_user_profile"
        const val CLIENTES = "clientes"

        fun UserProfile.toJsonString(): String {
            val json = JSONObject()
            json.put("uid", uid)
            json.put("nombres", nombres)
            json.put("apellidos", apellidos)
            json.put("email", email)
            if (telefono != null) {
                json.put("telefono", telefono)
            } else {
                json.put("telefono", JSONObject.NULL)
            }
            json.put("documento", documento)
            json.put("emailVerificado", emailVerificado)
            json.put("consentimientoAceptado", consentimientoAceptado)
            json.put("isGoogleUser", isGoogleUser)
            return json.toString()
        }

        fun String.toUserProfile(): UserProfile? {
            return try {
                val json = JSONObject(this)
                UserProfile(
                    uid = json.optString("uid", ""),
                    nombres = json.optString("nombres", ""),
                    apellidos = json.optString("apellidos", ""),
                    email = json.optString("email", ""),
                    telefono = if (json.isNull("telefono")) null else json.optString("telefono").ifBlank { null },
                    documento = json.optString("documento", ""),
                    emailVerificado = json.optBoolean("emailVerificado", false),
                    consentimientoAceptado = json.optBoolean("consentimientoAceptado", false),
                    isGoogleUser = json.optBoolean("isGoogleUser", false),
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
