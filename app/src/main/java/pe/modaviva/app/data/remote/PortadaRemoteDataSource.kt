package pe.modaviva.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import pe.modaviva.app.domain.model.Portada
import javax.inject.Inject
import javax.inject.Singleton

/** Lectura de la portada de Inicio (`inicio/portada`) en Firestore. */
@Singleton
class PortadaRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    /**
     * Emite la portada cada vez que el servidor la confirma (como el catálogo,
     * ignora lo que Firestore sirve desde su caché); null si no existe, está
     * inactiva o le faltan datos.
     */
    fun escuchar(): Flow<Portada?> = callbackFlow {
        val registro = firestore.document(DOCUMENTO)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                when {
                    error != null -> close(error)
                    snapshot != null && !snapshot.metadata.isFromCache ->
                        trySend(snapshot.data?.let(::aPortada))
                }
            }
        awaitClose { registro.remove() }
    }

    private fun aPortada(data: Map<String, Any>): Portada? {
        if (data["activa"] == false) return null
        return Portada(
            etiqueta = data["etiqueta"] as? String ?: "",
            titulo = (data["titulo"] as? String)?.takeIf { it.isNotBlank() } ?: return null,
            imagenUrl = (data["imagenUrl"] as? String)?.takeIf { it.isNotBlank() } ?: return null,
            accion = data["accion"] as? String ?: "",
            categoria = data["categoria"] as? String ?: "",
        )
    }

    private companion object {
        const val DOCUMENTO = "inicio/portada"
    }
}
