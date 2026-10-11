package pe.modaviva.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import pe.modaviva.app.domain.model.Tienda
import pe.modaviva.app.domain.repository.TiendaRepository
import javax.inject.Inject

class FirestoreTiendaRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : TiendaRepository {

    override fun observarTiendas(): Flow<List<Tienda>> = callbackFlow {
        val listener = firestore.collection("tiendas")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val tiendas = snapshot.documents.mapNotNull { doc ->
                    Tienda(
                        id = doc.id,
                        nombre = doc.getString("nombre").orEmpty(),
                        direccion = doc.getString("direccion").orEmpty(),
                        distrito = doc.getString("distrito").orEmpty(),
                        horario = doc.getString("horario").orEmpty(),
                        telefono = doc.getString("telefono").orEmpty(),
                    )
                }

                trySend(tiendas)
            }

        awaitClose {
            listener.remove()
        }
    }
}
