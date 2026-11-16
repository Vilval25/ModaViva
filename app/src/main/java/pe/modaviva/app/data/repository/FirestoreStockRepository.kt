package pe.modaviva.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import pe.modaviva.app.data.local.StockDao
import pe.modaviva.app.data.local.aDominio
import pe.modaviva.app.data.local.aEntidad
import pe.modaviva.app.domain.model.Stock
import pe.modaviva.app.domain.repository.StockRepository
import java.util.Date
import javax.inject.Inject

/**
 * Stock de Firestore con copia local en Room (HU-05).
 *
 * - El catálogo, el carrito y el checkout leen la copia local, que StockSyncWorker refresca cada
 *   15 min trayendo solo los documentos que cambiaron (CA-03).
 * - La ficha de la prenda escucha en tiempo real el stock de sus tallas y lo guarda en la copia
 *   local, así el cambio se ve de inmediato (CA-03 y CA-05).
 */
class FirestoreStockRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val dao: StockDao,
) : StockRepository {

    private val escritura = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeStock(
        prendaId: String,
        talla: String,
    ): Flow<Stock?> = callbackFlow {
        val listener = firestore
            .collection(STOCK)
            .document("${prendaId}_$talla")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.takeIf { it.exists() }?.toObject(Stock::class.java))
            }
        awaitClose { listener.remove() }
    }

    override fun observeStocks(): Flow<Map<String, Stock>> =
        dao.observarTodos().map { lista -> lista.associate { it.varianteId to it.aDominio() } }

    override fun observeStocksDePrenda(prendaId: String): Flow<Map<String, Stock>> = callbackFlow {
        val listener = firestore
            .collection(STOCK)
            .whereEqualTo("prendaId", prendaId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val docs = snapshot.documents.mapNotNull { doc -> doc.aStock()?.let { doc to it } }
                trySend(docs.associate { (doc, stock) -> doc.id to stock })
                // Se guarda también en la copia local para que el catálogo quede al día.
                escritura.launch {
                    dao.guardarTodos(docs.map { (doc, stock) -> stock.aEntidad(doc.id, doc.actualizadoEnMillis()) })
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sincronizar(): Result<Int> = runCatching {
        val desde = dao.ultimaActualizacion()
        val consulta = if (desde == null) {
            firestore.collection(STOCK)
        } else {
            // Margen de 1 min por diferencias de reloj; repetir un documento no causa problemas.
            firestore.collection(STOCK).whereGreaterThan("actualizadoEn", Timestamp(Date(desde - MARGEN_MS)))
        }
        val snapshot = consulta.get().await()
        val entidades = snapshot.documents.mapNotNull { doc ->
            doc.aStock()?.aEntidad(doc.id, doc.actualizadoEnMillis())
        }
        if (desde == null) dao.reemplazarTodos(entidades) else dao.guardarTodos(entidades)
        entidades.size
    }

    private fun DocumentSnapshot.aStock(): Stock? = runCatching { toObject(Stock::class.java) }.getOrNull()

    private fun DocumentSnapshot.actualizadoEnMillis(): Long =
        getTimestamp("actualizadoEn")?.toDate()?.time ?: 0L

    private companion object {
        const val STOCK = "stock"
        const val MARGEN_MS = 60_000L
    }
}
