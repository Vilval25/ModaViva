package pe.modaviva.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pe.modaviva.app.domain.model.DireccionEntrega
import pe.modaviva.app.domain.repository.DireccionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreDireccionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) : DireccionRepository {

    override fun observarDirecciones(): Flow<List<DireccionEntrega>> = callbackFlow {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(CLIENTES)
            .document(uid)
            .collection(DIRECCIONES)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val lista = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    DireccionEntrega(
                        id = doc.id,
                        alias = data["alias"] as? String ?: "",
                        destinatario = data["destinatario"] as? String ?: "",
                        telefono = data["telefono"] as? String ?: "",
                        direccion = data["direccion"] as? String ?: "",
                        referencia = data["referencia"] as? String,
                        distrito = data["distrito"] as? String ?: "",
                        distritoSlug = data["distritoSlug"] as? String ?: "",
                        lat = (data["lat"] as? Number)?.toDouble(),
                        lng = (data["lng"] as? Number)?.toDouble(),
                        esPrincipal = data["esPrincipal"] as? Boolean ?: false,
                    )
                }.sortedByDescending { it.esPrincipal }

                trySend(lista)
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun guardarDireccion(direccion: DireccionEntrega): Result<Unit> = runCatching {
        val uid = firebaseAuth.currentUser?.uid
            ?: throw IllegalStateException("Usuario no autenticado")

        val collectionRef = firestore.collection(CLIENTES)
            .document(uid)
            .collection(DIRECCIONES)

        val snapshot = collectionRef.get().await()
        val existingDocs = snapshot.documents
        val isFirst = existingDocs.isEmpty() || (existingDocs.size == 1 && existingDocs.first().id == direccion.id)

        val debeSerPrincipal = direccion.esPrincipal || isFirst

        // Si esta dirección va a ser principal, quitar principal a las demás
        if (debeSerPrincipal) {
            val batch = firestore.batch()
            for (doc in existingDocs) {
                if (doc.id != direccion.id && (doc.getBoolean("esPrincipal") == true)) {
                    batch.update(doc.reference, "esPrincipal", false)
                }
            }
            batch.commit().await()
        }

        val docRef = if (direccion.id.isBlank()) {
            collectionRef.document()
        } else {
            collectionRef.document(direccion.id)
        }

        val data = mapOf(
            "alias" to direccion.alias.trim(),
            "destinatario" to direccion.destinatario.trim(),
            "telefono" to direccion.telefono.trim(),
            "direccion" to direccion.direccion.trim(),
            "referencia" to direccion.referencia?.trim()?.ifBlank { null },
            "distrito" to direccion.distrito.trim(),
            "distritoSlug" to direccion.distritoSlug.trim(),
            "lat" to direccion.lat,
            "lng" to direccion.lng,
            "esPrincipal" to debeSerPrincipal,
            "actualizadoEn" to FieldValue.serverTimestamp(),
        )

        if (direccion.id.isBlank()) {
            docRef.set(data + mapOf("creadoEn" to FieldValue.serverTimestamp())).await()
        } else {
            docRef.set(data, com.google.firebase.firestore.SetOptions.merge()).await()
        }
    }

    override suspend fun eliminarDireccion(direccionId: String): Result<Unit> = runCatching {
        val uid = firebaseAuth.currentUser?.uid
            ?: throw IllegalStateException("Usuario no autenticado")

        val docRef = firestore.collection(CLIENTES)
            .document(uid)
            .collection(DIRECCIONES)
            .document(direccionId)

        val snapshot = docRef.get().await()
        val eraPrincipal = snapshot.getBoolean("esPrincipal") == true

        docRef.delete().await()

        if (eraPrincipal) {
            val remaining = firestore.collection(CLIENTES)
                .document(uid)
                .collection(DIRECCIONES)
                .get()
                .await()
            if (!remaining.isEmpty) {
                val firstDoc = remaining.documents.first()
                firstDoc.reference.update("esPrincipal", true).await()
            }
        }
    }

    override suspend fun establecerPrincipal(direccionId: String): Result<Unit> = runCatching {
        val uid = firebaseAuth.currentUser?.uid
            ?: throw IllegalStateException("Usuario no autenticado")

        val collectionRef = firestore.collection(CLIENTES)
            .document(uid)
            .collection(DIRECCIONES)

        val snapshot = collectionRef.get().await()
        val batch = firestore.batch()
        for (doc in snapshot.documents) {
            val esEste = doc.id == direccionId
            batch.update(doc.reference, "esPrincipal", esEste)
        }
        batch.commit().await()
    }

    private companion object {
        const val CLIENTES = "clientes"
        const val DIRECCIONES = "direcciones"
    }
}
