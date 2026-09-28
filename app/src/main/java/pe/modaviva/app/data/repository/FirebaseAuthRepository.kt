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
 * Firebase Auth cubre la identidad (correo y contraseña) y Firestore guarda
 * el perfil del cliente. El esquema de colecciones es el descrito en
 * docs/diseno-firestore.md:
 *
 *  - `clientes/{authUid}`      perfil del cliente dentro de la app
 *  - `documentos/{documento}`  índice espejo que garantiza la unicidad del DNI
 *
 * La unicidad del correo la impone el propio Firebase Auth, que rechaza el
 * registro con ERROR_EMAIL_ALREADY_IN_USE; por eso no hay un método para
 * consultarla.
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

        reflectEmailVerification(firebaseUser)

        return loadProfile(firebaseUser.uid, firebaseUser.email.orEmpty())
    }

    /**
     * Refleja la verificación del correo en `clientes/{uid}`. Después de
     * pulsar el enlace, el estado vive en Firebase Auth; Firestore solo lo
     * espeja. Es best effort: si la escritura falla, el perfil se devuelve
     * igual y el detector de la pantalla de verificación reintentará.
     */
    private suspend fun reflectEmailVerification(firebaseUser: FirebaseUser) {
        runCatching { firebaseUser.reload().await() }
        if (firebaseUser.isEmailVerified) {
            runCatching {
                firestore.collection(CLIENTES).document(firebaseUser.uid)
                    .update("emailVerificado", true)
                    .await()
            }
        }
    }

    override suspend fun syncEmailVerification(): Boolean {
        val firebaseUser = firebaseAuth.currentUser ?: return false
        reflectEmailVerification(firebaseUser)
        return firebaseUser.isEmailVerified
    }

    override suspend fun register(request: RegisterRequest): Result<UserProfile> {
        // Comprobación preventiva para avisar antes de crear la cuenta. No es
        // la garantía: hay una condición de carrera entre esta lectura y la
        // escritura, y las reglas pueden negarla a usuarios anónimos (en ese
        // caso devuelve false y se sigue igual); la transacción de abajo es
        // la que impone la unicidad de verdad.
        if (isDocumentoTaken(request.documento)) {
            return Result.failure(AuthException(AuthError.DOCUMENT_TAKEN))
        }

        val firebaseUser = try {
            firebaseAuth
                .createUserWithEmailAndPassword(request.email, request.password)
                .await()
                .user
        } catch (e: Exception) {
            return Result.failure(e.toAuthException())
        }

        if (firebaseUser == null) {
            return Result.failure(AuthException(AuthError.UNKNOWN))
        }

        val uid = firebaseUser.uid
        val profile = request.toProfile()
        var documentoLibre = false

        try {
            firestore.runTransaction { transaction ->
                val documentoRef = firestore.collection(DOCUMENTOS).document(profile.documento)
                val yaReservado = transaction.get(documentoRef).exists()
                if (!yaReservado) {
                    transaction.set(documentoRef, mapOf("authUid" to uid))
                    transaction.set(
                        firestore.collection(CLIENTES).document(uid),
                        mapOf(
                            "nombres" to profile.nombres,
                            "apellidos" to profile.apellidos,
                            "documento" to profile.documento,
                            "telefono" to profile.telefono,
                            "email" to profile.email,
                            "emailVerificado" to false,
                            "creadoEn" to FieldValue.serverTimestamp(),
                        ),
                    )
                }
                // Firestore puede reintentar la transacción; solo la invocación
                // que confirma el commit deja este valor en firme.
                documentoLibre = !yaReservado
                null
            }.await()
        } catch (e: Exception) {
            discardFailedRegistration(firebaseUser)
            return Result.failure(e.toAuthException())
        }

        if (!documentoLibre) {
            discardFailedRegistration(firebaseUser)
            return Result.failure(AuthException(AuthError.DOCUMENT_TAKEN))
        }

        // El envío del correo es best effort: si falla, la cuenta ya existe y
        // el cliente puede volver a solicitar el envío desde la app.
        runCatching { firebaseUser.sendEmailVerification().await() }

        return Result.success(profile)
    }

    /**
     * Elimina la cuenta recién creada cuando la transacción no pudo escribir
     * el perfil. Sin esto quedaría un usuario de Auth huérfano: una cuenta
     * sin `clientes/{uid}` que no podría iniciar sesión.
     */
    private suspend fun discardFailedRegistration(firebaseUser: FirebaseUser) {
        // delete() exige sesión activa; por eso va antes de cerrar sesión. Si
        // el borrado falla, se cierra igual para no dejar la app a medias.
        runCatching { firebaseUser.delete().await() }
        firebaseAuth.signOut()
    }

    override suspend fun isDocumentoTaken(documento: String): Boolean = try {
        firestore
            .collection(DOCUMENTOS)
            .document(documento.trim())
            .get()
            .await()
            .exists()
    } catch (e: Exception) {
        // Es solo una comprobación preventiva para avisar antes de enviar. La
        // garantía real la impone la transacción del registro, así que un
        // fallo de red aquí no debe bloquear el formulario.
        false
    }

    private suspend fun loadProfile(uid: String, fallbackEmail: String): Result<UserProfile> {
        val snapshot = try {
            firestore.collection(CLIENTES).document(uid).get().await()
        } catch (e: Exception) {
            return Result.failure(e.toAuthException())
        }

        if (!snapshot.exists()) {
            // Firebase Auth acepta el acceso pero no hay perfil en Firestore. Se
            // cierra la sesión para no dejar la app en un estado a medias.
            firebaseAuth.signOut()
            return Result.failure(AuthException(AuthError.PROFILE_NOT_FOUND))
        }

        val data = snapshot.data.orEmpty()
        return Result.success(
            UserProfile(
                nombres = data["nombres"] as? String ?: "",
                apellidos = data["apellidos"] as? String ?: "",
                documento = data["documento"] as? String ?: "",
                telefono = data["telefono"] as? String ?: "",
                email = data["email"] as? String ?: fallbackEmail,
                emailVerificado = data["emailVerificado"] as? Boolean ?: false,
            )
        )
    }

    private companion object {
        const val CLIENTES = "clientes"
        const val DOCUMENTOS = "documentos"
    }
}

private fun RegisterRequest.toProfile() = UserProfile(
    nombres = nombres,
    apellidos = apellidos,
    documento = documento,
    telefono = telefono,
    email = email,
)

/**
 * Traduce las excepciones de Firebase al vocabulario del dominio. La interfaz
 * recibe un [AuthError] y nunca ve un tipo de Firebase.
 *
 * firebase-auth 24.x retiró las constantes `ERROR_*` de `FirebaseAuth` y las
 * sustituyó por excepciones tipadas. Se comprueban primero esas subclases y,
 * para los códigos que no tienen tipo propio, se compara el `errorCode` que
 * llega por la red.
 */
private fun Exception.toAuthException(): AuthException = when (this) {
    // El orden importa: FirebaseAuthWeakPasswordException hereda de
    // FirebaseAuthInvalidCredentialsException y debe comprobarse antes.
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
