package pe.modaviva.app.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import pe.modaviva.app.domain.model.Prenda
import javax.inject.Inject
import javax.inject.Singleton

/** Lectura del catálogo publicado en Firestore. */
@Singleton
class CatalogoRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    /**
     * Solo lo publicado y con fotos aprobadas (HU-04 CA-03). Usa el índice
     * compuesto de firestore.indexes.json.
     */
    private fun consulta(): Query = firestore.collection(PRENDAS)
        .whereEqualTo("publicada", true)
        .whereEqualTo("fotosAprobadas", true)
        .orderBy("creadaEn", Query.Direction.DESCENDING)

    /**
     * Emite el catálogo cada vez que el servidor lo confirma. Ignora lo que
     * Firestore sirve desde su propia caché (sin conexión): la copia local la
     * maneja Room, y así la fecha de "última actualización" es real.
     *
     * MetadataChanges.INCLUDE es necesario: si al reconectar el catálogo no
     * cambió, Firestore solo avisa que los datos pasaron de "caché" a
     * "servidor", sin cambios en los documentos, y sin INCLUDE ese aviso no
     * llega.
     */
    fun escuchar(): Flow<List<Prenda>> = callbackFlow {
        val registro = consulta().addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            when {
                error != null -> close(error)
                snapshot != null && !snapshot.metadata.isFromCache -> trySend(prendasValidas(snapshot))
            }
        }
        awaitClose { registro.remove() }
    }

    /** Pide el catálogo directamente al servidor; falla si no hay conexión. */
    suspend fun obtener(): List<Prenda> = prendasValidas(consulta().get(Source.SERVER).await())

    private fun prendasValidas(snapshot: QuerySnapshot): List<Prenda> {
        val prendas = snapshot.documents.mapNotNull { doc ->
            PrendaMapper.desdeFirestore(doc.id, doc.data.orEmpty())
        }
        val descartadas = snapshot.size() - prendas.size
        if (descartadas > 0) {
            Log.w(TAG, "$descartadas prenda(s) publicadas con ficha incompleta; no se muestran")
        }
        return prendas
    }

    private companion object {
        const val PRENDAS = "prendas"
        const val TAG = "Catalogo"
    }
}
