package pe.modaviva.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import pe.modaviva.app.domain.model.Stock
import pe.modaviva.app.domain.repository.StockRepository
import javax.inject.Inject

class FirestoreStockRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : StockRepository {

    override fun observeStock(
        prendaId: String,
        talla: String,
    ): Flow<Stock?> = callbackFlow {

        val documentId = "${prendaId}_${talla}"

        val listener = firestore
            .collection("stock")
            .document(documentId)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }

                val stock = snapshot.toObject(Stock::class.java)

                trySend(stock)
            }

        awaitClose {
            listener.remove()
        }
    }

    override fun observeStocks(): Flow<Map<String, Stock>> = callbackFlow {
        val listener = firestore
            .collection("stock")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(emptyMap())
                    return@addSnapshotListener
                }

                val mapaStock = snapshot.documents.mapNotNull { doc ->
                    val stock = doc.toObject(Stock::class.java)
                    if (stock != null) doc.id to stock else null
                }.toMap()

                trySend(mapaStock)
            }

        awaitClose {
            listener.remove()
        }
    }
}