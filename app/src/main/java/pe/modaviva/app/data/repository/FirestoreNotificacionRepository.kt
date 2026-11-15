package pe.modaviva.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import pe.modaviva.app.data.remote.await
import pe.modaviva.app.domain.model.Notificacion
import pe.modaviva.app.domain.model.TipoNotificacion
import pe.modaviva.app.domain.model.cantidadNoLeidas
import pe.modaviva.app.domain.model.ordenadasRecientePrimero
import pe.modaviva.app.domain.repository.NotificacionRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Avisos en `clientes/{uid}/notificaciones` (HU-18). Una sola escucha en tiempo
 * real alimenta la bandeja y la insignia de Inicio; cambia sola al iniciar o
 * cerrar sesión.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FirestoreNotificacionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : NotificacionRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val uidActual: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override val notificaciones: Flow<List<Notificacion>> = uidActual
        .flatMapLatest { uid -> if (uid == null) flowOf(emptyList()) else escuchar(uid) }
        .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    override val noLeidas: Flow<Int> = notificaciones
        .map { it.cantidadNoLeidas() }
        .distinctUntilChanged()

    private fun coleccion(uid: String) =
        firestore.collection(CLIENTES).document(uid).collection(NOTIFICACIONES)

    private fun escuchar(uid: String): Flow<List<Notificacion>> = callbackFlow {
        val registro = coleccion(uid)
            .orderBy(CAMPO_CREADA_EN, Query.Direction.DESCENDING)
            .limit(LIMITE)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // No se cierra el flujo: la app sigue funcionando con la bandeja vacía.
                    Log.w(TAG, "No se pudo leer la bandeja de avisos", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val lista = snapshot?.documents.orEmpty()
                    .mapNotNull { it.aNotificacion() }
                    .ordenadasRecientePrimero()
                trySend(lista)
            }
        awaitClose { registro.remove() }
    }

    override suspend fun marcarLeida(id: String) {
        val uid = auth.currentUser?.uid ?: return
        runCatching { coleccion(uid).document(id).update(CAMPO_LEIDA, true).await() }
            .onFailure { Log.w(TAG, "No se pudo marcar el aviso como leído", it) }
    }

    override suspend fun marcarTodasLeidas() {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            val pendientes = coleccion(uid).whereEqualTo(CAMPO_LEIDA, false).get().await()
            if (pendientes.isEmpty) return@runCatching
            val lote = firestore.batch()
            pendientes.documents.forEach { lote.update(it.reference, CAMPO_LEIDA, true) }
            lote.commit().await()
        }.onFailure { Log.w(TAG, "No se pudieron marcar los avisos como leídos", it) }
    }

    override fun crear(
        tipo: TipoNotificacion,
        titulo: String,
        texto: String,
        refId: String?,
    ) {
        val uid = auth.currentUser?.uid ?: return
        val datos = buildMap<String, Any> {
            put("tipo", tipo.name)
            put("titulo", titulo.take(MAX_TITULO))
            put("texto", texto.take(MAX_TEXTO))
            put(CAMPO_CREADA_EN, FieldValue.serverTimestamp())
            put(CAMPO_LEIDA, false)
            if (!refId.isNullOrBlank()) put("refId", refId)
        }
        // Sin await: con conexión se sube al instante y sin conexión queda en cola.
        coleccion(uid).document().set(datos)
            .addOnFailureListener { Log.w(TAG, "No se pudo registrar el aviso", it) }
    }

    private fun DocumentSnapshot.aNotificacion(): Notificacion? {
        val titulo = getString("titulo") ?: return null
        val fecha = getTimestamp(CAMPO_CREADA_EN, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        return Notificacion(
            id = id,
            tipo = TipoNotificacion.desdeCodigo(getString("tipo")),
            titulo = titulo,
            texto = getString("texto").orEmpty(),
            creadaEn = fecha?.toDate()?.toInstant() ?: Instant.now(),
            leida = getBoolean(CAMPO_LEIDA) == true,
            refId = getString("refId"),
        )
    }

    private companion object {
        const val TAG = "Notificaciones"
        const val CLIENTES = "clientes"
        const val NOTIFICACIONES = "notificaciones"
        const val CAMPO_CREADA_EN = "creadaEn"
        const val CAMPO_LEIDA = "leida"
        const val LIMITE = 100L
        const val MAX_TITULO = 120
        const val MAX_TEXTO = 500
    }
}
