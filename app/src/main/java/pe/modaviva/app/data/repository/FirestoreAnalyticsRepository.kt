package pe.modaviva.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import pe.modaviva.app.domain.model.PruebaVirtualRegistro
import pe.modaviva.app.domain.repository.AnalyticsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreAnalyticsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : AnalyticsRepository {

    @Suppress("UNCHECKED_CAST")
    override fun observarPruebasVirtuales(): Flow<List<PruebaVirtualRegistro>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(CLIENTES)
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val rawPruebas = snapshot.get(CAMPO_PRUEBAS) as? List<Map<String, Any?>>
                    ?: emptyList()
                val pruebas = rawPruebas.mapNotNull { item ->
                    val codigo = item["prendaCodigo"] as? String ?: return@mapNotNull null
                    val fecha = (item["fechaEpochMillis"] as? Number)?.toLong()
                        ?: return@mapNotNull null
                    PruebaVirtualRegistro(
                        prendaCodigo = codigo,
                        prendaNombre = item["prendaNombre"] as? String ?: codigo,
                        categoria = item["categoria"] as? String ?: "",
                        fechaEpochMillis = fecha,
                        comprada = item["comprada"] as? Boolean ?: false,
                    )
                }.sortedBy { it.fechaEpochMillis }

                trySend(pruebas)
            }

        awaitClose { listener.remove() }
    }

    private companion object {
        const val CLIENTES = "clientes"
        const val CAMPO_PRUEBAS = "pruebasVirtuales"
    }
}

