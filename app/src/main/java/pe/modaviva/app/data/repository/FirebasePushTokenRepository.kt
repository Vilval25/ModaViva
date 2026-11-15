package pe.modaviva.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.withTimeoutOrNull
import pe.modaviva.app.data.remote.await
import pe.modaviva.app.domain.repository.PushTokenRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda el token de FCM en `clientes/{uid}.fcmTokens` cada vez que alguien
 * inicia sesión en el dispositivo, y lo quita al cerrar sesión para que otra
 * cuenta no reciba los avisos de la anterior (HU-18 CA-05).
 */
@Singleton
class FirebasePushTokenRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val messaging: FirebaseMessaging,
) : PushTokenRepository {

    private var uidRegistrado: String? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                uidRegistrado = null
            } else if (uid != uidRegistrado) {
                uidRegistrado = uid
                messaging.token
                    .addOnSuccessListener { token -> registrar(token) }
                    .addOnFailureListener { Log.w(TAG, "No se pudo obtener el token de FCM", it) }
            }
        }
    }

    override fun registrar(token: String) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection(CLIENTES).document(uid)
            .update(CAMPO_TOKENS, FieldValue.arrayUnion(token))
            .addOnFailureListener { Log.w(TAG, "No se pudo guardar el token de FCM", it) }
    }

    override suspend fun desregistrar() {
        val uid = auth.currentUser?.uid ?: return
        // Con tope de tiempo: sin conexión, cerrar sesión no debe quedarse esperando.
        runCatching {
            withTimeoutOrNull(TIEMPO_MAXIMO_MS) {
                val token = messaging.token.await()
                firestore.collection(CLIENTES).document(uid)
                    .update(CAMPO_TOKENS, FieldValue.arrayRemove(token))
                    .await()
            }
        }.onFailure { Log.w(TAG, "No se pudo quitar el token de FCM", it) }
    }

    private companion object {
        const val TAG = "PushTokens"
        const val CLIENTES = "clientes"
        const val CAMPO_TOKENS = "fcmTokens"
        const val TIEMPO_MAXIMO_MS = 3_000L
    }
}
