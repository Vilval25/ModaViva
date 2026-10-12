package pe.modaviva.app.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import pe.modaviva.app.data.local.FavoritoDao
import pe.modaviva.app.data.local.FavoritoEntity
import pe.modaviva.app.data.local.aDominio
import pe.modaviva.app.data.local.aEntidad
import pe.modaviva.app.data.remote.await
import pe.modaviva.app.domain.model.Favorito
import pe.modaviva.app.domain.model.PrendaRetirada
import pe.modaviva.app.domain.repository.FavoritosRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Favoritos en Firestore con copia en Room (HU-08).
 *
 * Mientras hay sesión, una escucha de clientes/{uid}/favoritos deja Room igual
 * a la cuenta: así se ven los mismos favoritos al reiniciar la app o en otro
 * dispositivo (CA-06). Las marcas se guardan primero en Room, para que el
 * corazón cambie al instante, y Firestore las sube cuando hay conexión.
 * Al cerrar sesión se borra la copia local.
 */
@Singleton
class FirestoreFavoritosRepository @Inject constructor(
    private val dao: FavoritoDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val reloj: Clock,
) : FavoritosRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var escucha: ListenerRegistration? = null
    private var uidActual: String? = null
    private var pendiente: String? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid == uidActual) return@addAuthStateListener
            val habiaSesion = uidActual != null
            uidActual = uid
            escucha?.remove()
            escucha = null
            if (uid == null) {
                pendiente = null
                if (habiaSesion) scope.launch { dao.borrarTodos() }
            } else {
                escuchar(uid)
                pendiente?.let { prendaId ->
                    pendiente = null
                    scope.launch { marcar(prendaId) }
                }
            }
        }
    }

    override val favoritos: Flow<List<Favorito>> =
        dao.observar().map { lista -> lista.map { it.aDominio() } }

    override suspend fun marcar(prendaId: String) {
        guardar(Favorito(prendaId, Instant.now(reloj)))
    }

    override suspend fun restaurar(favorito: Favorito) {
        guardar(favorito)
    }

    override suspend fun quitar(prendaId: String) {
        val uid = uidActual ?: return
        dao.borrar(prendaId)
        // Sin await: sin conexión, Firestore guarda el cambio y lo sube después.
        coleccion(uid).document(prendaId).delete()
            .addOnFailureListener { Log.w(TAG, "No se pudo quitar $prendaId", it) }
    }

    override fun marcarAlIniciarSesion(prendaId: String?) {
        pendiente = prendaId
    }

    override suspend fun buscarRetirada(prendaId: String): PrendaRetirada? = runCatching {
        val doc = firestore.collection("prendas").document(prendaId).get().await()
        if (!doc.exists()) return null
        PrendaRetirada(
            nombre = doc.getString("nombre").orEmpty(),
            marca = doc.getString("marca").orEmpty(),
            foto = (doc.get("fotos") as? List<*>)?.firstOrNull() as? String,
        )
    }.getOrNull()

    private suspend fun guardar(favorito: Favorito) {
        val uid = uidActual ?: return
        dao.guardar(favorito.aEntidad())
        // firestore.rules solo permite el campo agregadoEn.
        coleccion(uid).document(favorito.prendaId)
            .set(mapOf("agregadoEn" to Timestamp(favorito.agregadoEn)))
            .addOnFailureListener { Log.w(TAG, "No se pudo guardar ${favorito.prendaId}", it) }
    }

    private fun escuchar(uid: String) {
        escucha = coleccion(uid).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                Log.w(TAG, "Error al escuchar los favoritos", error)
                return@addSnapshotListener
            }
            val lista = snapshot.documents.map { doc ->
                FavoritoEntity(
                    prendaId = doc.id,
                    // Un favorito recién marcado aún no tiene la hora del servidor.
                    agregadoEnMillis = doc.getTimestamp("agregadoEn")?.toDate()?.time
                        ?: Instant.now(reloj).toEpochMilli(),
                )
            }
            scope.launch {
                // Si la sesión cambió mientras tanto, esta lista ya no corresponde.
                if (uidActual == uid) dao.reemplazarTodos(lista)
            }
        }
    }

    private fun coleccion(uid: String) =
        firestore.collection("clientes").document(uid).collection("favoritos")

    private companion object {
        const val TAG = "Favoritos"
    }
}
