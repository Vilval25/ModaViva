package pe.modaviva.app.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthEmailException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import pe.modaviva.app.data.remote.await
import pe.modaviva.app.domain.exception.AuthError
import pe.modaviva.app.domain.exception.AuthException
import pe.modaviva.app.domain.model.RegisterRequest
import pe.modaviva.app.domain.model.UserProfile
import pe.modaviva.app.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Autenticación y perfil contra Firebase Auth + Cloud Firestore.
 *
 * Implementa el modelo de datos v2 (HT-02):
 *  - `clientes/{authUid}`: perfil del cliente indexado por el UID de Firebase Auth.
 *  - Soporta autenticación con correo/contraseña y Google Sign-in.
 *  - La verificación de correo vive en Firebase Auth (currentUser.isEmailVerified).
 *  - Registra el consentimiento legal de Términos y Condiciones (HU-01 CA-10).
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<UserProfile> {
        val firebaseUser = try {
            firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await().user
        } catch (e: Exception) {
            return Result.failure(e.toAuthException())
        }

        if (firebaseUser == null) {
            firebaseAuth.signOut()
            return Result.failure(AuthException(AuthError.INVALID_CREDENTIALS))
        }

        return loadProfile(firebaseUser.uid, firebaseUser.email.orEmpty())
    }

    override suspend fun signInWithGoogle(idToken: String): Result<UserProfile> {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val firebaseUser = try {
            firebaseAuth.signInWithCredential(credential).await().user
        } catch (e: Exception) {
            return Result.failure(e.toAuthException())
        }

        if (firebaseUser == null) {
            return Result.failure(AuthException(AuthError.UNKNOWN))
        }

        val uid = firebaseUser.uid
        val email = firebaseUser.email.orEmpty()

        val clientDocRef = firestore.collection(CLIENTES).document(uid)
        val clientDoc = try {
            clientDocRef.get().await()
        } catch (e: Exception) {
            null
        }

        if (clientDoc == null || !clientDoc.exists()) {
            val displayName = firebaseUser.displayName.orEmpty().trim()
            val nameParts = displayName.split(" ", limit = 2)
            val nombres = nameParts.getOrNull(0) ?: displayName
            val apellidos = nameParts.getOrNull(1) ?: ""

            // El consentimiento queda en null hasta que acepte el diálogo de T&C
            val newProfile = mapOf(
                "nombres" to nombres,
                "apellidos" to apellidos,
                "email" to email,
                "telefono" to null,
                "origen" to "app",
                "consentimiento" to null,
                "creadoEn" to FieldValue.serverTimestamp(),
                "actualizadoEn" to FieldValue.serverTimestamp(),
            )
            runCatching { clientDocRef.set(newProfile).await() }
        }

        return loadProfile(uid, email)
    }

    override suspend fun acceptTermsAndConditions(version: String): Result<Unit> {
        val uid = firebaseAuth.currentUser?.uid
            ?: return Result.failure(AuthException(AuthError.PROFILE_NOT_FOUND))

        return try {
            firestore.collection(CLIENTES).document(uid).update(
                mapOf(
                    "consentimiento" to mapOf(
                        "version" to version,
                        "aceptadoEn" to FieldValue.serverTimestamp(),
                    ),
                    "actualizadoEn" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e.toAuthException())
        }
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun syncEmailVerification(): Boolean {
        val firebaseUser = firebaseAuth.currentUser ?: return false
        runCatching { firebaseUser.reload().await() }
        return firebaseUser.isEmailVerified
    }

    override suspend fun register(request: RegisterRequest): Result<UserProfile> {
        val firebaseUser = try {
            firebaseAuth
                .createUserWithEmailAndPassword(request.email.trim(), request.password)
                .await()
                .user
        } catch (e: Exception) {
            return Result.failure(e.toAuthException())
        }

        if (firebaseUser == null) {
            return Result.failure(AuthException(AuthError.UNKNOWN))
        }

        val uid = firebaseUser.uid
        val profileData = mapOf(
            "nombres" to request.nombres.trim(),
            "apellidos" to request.apellidos.trim(),
            "email" to request.email.trim(),
            "telefono" to null,
            "origen" to "app",
            "consentimiento" to mapOf(
                "version" to "1.0",
                "aceptadoEn" to FieldValue.serverTimestamp(),
            ),
            "creadoEn" to FieldValue.serverTimestamp(),
            "actualizadoEn" to FieldValue.serverTimestamp(),
        )

        try {
            firestore.collection(CLIENTES).document(uid).set(profileData).await()
        } catch (e: Exception) {
            discardFailedRegistration(firebaseUser)
            return Result.failure(e.toAuthException())
        }

        runCatching { firebaseUser.sendEmailVerification().await() }

        val profile = request.toProfile(uid).copy(consentimientoAceptado = true)
        return Result.success(profile)
    }

    private suspend fun discardFailedRegistration(firebaseUser: FirebaseUser) {
        runCatching { firebaseUser.delete().await() }
        firebaseAuth.signOut()
    }

    private suspend fun loadProfile(uid: String, fallbackEmail: String): Result<UserProfile> {
        val snapshot = try {
            firestore.collection(CLIENTES).document(uid).get().await()
        } catch (e: Exception) {
            return Result.failure(e.toAuthException())
        }

        if (!snapshot.exists()) {
            firebaseAuth.signOut()
            return Result.failure(AuthException(AuthError.PROFILE_NOT_FOUND))
        }

        val data = snapshot.data.orEmpty()
        val firebaseUser = firebaseAuth.currentUser
        val isVerified = firebaseUser?.isEmailVerified ?: false

        val consentimiento = data["consentimiento"] as? Map<*, *>
        val tieneConsentimiento = consentimiento != null && !consentimiento["version"]?.toString().isNullOrBlank()

        return Result.success(
            UserProfile(
                uid = uid,
                nombres = data["nombres"] as? String ?: "",
                apellidos = data["apellidos"] as? String ?: "",
                documento = data["documento"] as? String ?: "",
                telefono = data["telefono"] as? String,
                email = data["email"] as? String ?: fallbackEmail,
                emailVerificado = isVerified,
                consentimientoAceptado = tieneConsentimiento,
            )
        )
    }

    private companion object {
        const val CLIENTES = "clientes"
    }
}

/**
 * Traduce las excepciones de Firebase al vocabulario del dominio. La interfaz
 * recibe un [AuthError] y nunca ve un tipo de Firebase.
 */
private fun Exception.toAuthException(): AuthException = when (this) {
    is FirebaseAuthUserCollisionException ->
        AuthException(AuthError.EMAIL_ALREADY_IN_USE)
    is FirebaseAuthWeakPasswordException ->
        AuthException(AuthError.WEAK_PASSWORD)
    is FirebaseAuthInvalidCredentialsException ->
        AuthException(AuthError.INVALID_CREDENTIALS)
    is FirebaseAuthInvalidUserException ->
        AuthException(AuthError.INVALID_CREDENTIALS)
    is FirebaseAuthEmailException ->
        AuthException(AuthError.INVALID_EMAIL)
    is FirebaseNetworkException ->
        AuthException(AuthError.NETWORK)

    is FirebaseAuthException -> when (errorCode) {
        ERROR_TOO_MANY_REQUESTS -> AuthException(AuthError.TOO_MANY_REQUESTS)
        ERROR_NETWORK_REQUEST_FAILED -> AuthException(AuthError.NETWORK)
        else -> AuthException(AuthError.UNKNOWN, message)
    }

    is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
            AuthException(AuthError.NETWORK)
        else -> AuthException(AuthError.UNKNOWN, message)
    }

    else -> AuthException(AuthError.UNKNOWN, message)
}

private const val ERROR_TOO_MANY_REQUESTS = "ERROR_TOO_MANY_REQUESTS"
private const val ERROR_NETWORK_REQUEST_FAILED = "ERROR_NETWORK_REQUEST_FAILED"
