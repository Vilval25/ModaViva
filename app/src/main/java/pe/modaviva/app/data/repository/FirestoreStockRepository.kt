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

                println("STOCK CALLBACK EJECUTADO")

                if (error != null) {
                    println("ERROR STOCK FIRESTORE: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                println("STOCK SNAPSHOT: $snapshot")

                if (snapshot == null || !snapshot.exists()) {
                    println("STOCK DOCUMENTO NO EXISTE")
                    trySend(null)
                    return@addSnapshotListener
                }

                val stock = snapshot.toObject(Stock::class.java)

                println("STOCK CONVERTIDO: $stock")

                trySend(stock)
            }

        awaitClose {
            listener.remove()
        }
    }
}