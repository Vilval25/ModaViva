package pe.modaviva.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import pe.modaviva.app.data.local.CartDao
import pe.modaviva.app.data.local.CartEntity
import pe.modaviva.app.data.local.aDominio
import pe.modaviva.app.data.remote.await
import pe.modaviva.app.domain.model.CartItem
import pe.modaviva.app.domain.repository.CartRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomCartRepository @Inject constructor(
    private val cartDao: CartDao,
    private val firestore: FirebaseFirestore? = null,
    private val firebaseAuth: FirebaseAuth? = null,
) : CartRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null
    private var remoteListener: ListenerRegistration? = null
    private var lastSyncedUid: String? = null

    init {
        firebaseAuth?.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                if (lastSyncedUid != user.uid) {
                    lastSyncedUid = user.uid
                    syncJob?.cancel()
                    syncJob = scope.launch {
                        sincronizarConCuenta(user.uid)
                    }
                }
            } else {
                if (lastSyncedUid != null) {
                    // Al cerrar sesión: el carrito de la cuenta queda a salvo en Firestore,
                    // y limpiamos el carrito local para proteger la privacidad del usuario.
                    lastSyncedUid = null
                    remoteListener?.remove()
                    remoteListener = null
                    syncJob?.cancel()
                    syncJob = scope.launch {
                        cartDao.vaciarCarrito()
                    }
                }
            }
        }
    }

    override val items: Flow<List<CartItem>> =
        cartDao.observarCarrito().map { list -> list.map { it.aDominio() } }

    override val totalItems: Flow<Int> =
        cartDao.observarTotalItems()

    override suspend fun agregarAlCarrito(
        prendaId: String,
        talla: String,
        colorNombre: String,
        colorHex: String,
        cantidad: Int,
        precioAlAgregar: Double,
    ) {
        val varianteId = CartItem.generarVarianteId(prendaId, talla)
        val entity = CartEntity(
            varianteId = varianteId,
            prendaId = prendaId,
            talla = talla,
            colorNombre = colorNombre,
            colorHex = colorHex,
            cantidad = cantidad,
            agregadoEnMillis = Instant.now().toEpochMilli(),
            precioAlAgregar = precioAlAgregar,
        )
        cartDao.agregarOIncrementar(entity)

        val uid = firebaseAuth?.currentUser?.uid
        if (uid != null) {
            val itemActualizado = cartDao.obtenerPorVarianteId(varianteId) ?: entity
            guardarEnFirestore(uid, itemActualizado)
        }
    }

    override suspend fun actualizarCantidad(varianteId: String, cantidad: Int) {
        if (cantidad <= 0) {
            eliminarItem(varianteId)
        } else {
            cartDao.actualizarCantidad(varianteId, cantidad)
            val uid = firebaseAuth?.currentUser?.uid
            if (uid != null) {
                val item = cartDao.obtenerPorVarianteId(varianteId)
                if (item != null) {
                    guardarEnFirestore(uid, item)
                }
            }
        }
    }

    override suspend fun eliminarItem(varianteId: String) {
        cartDao.eliminarItem(varianteId)
        val uid = firebaseAuth?.currentUser?.uid
        val fs = firestore
        if (uid != null && fs != null) {
            runCatching {
                fs.collection("clientes")
                    .document(uid)
                    .collection("carrito")
                    .document(varianteId)
                    .delete()
                    .await()
            }
        }
    }

    override suspend fun vaciarCarrito() {
        cartDao.vaciarCarrito()
        val uid = firebaseAuth?.currentUser?.uid
        val fs = firestore
        if (uid != null && fs != null) {
            runCatching {
                val snapshot = fs.collection("clientes")
                    .document(uid)
                    .collection("carrito")
                    .get()
                    .await()
                for (doc in snapshot.documents) {
                    doc.reference.delete().await()
                }
            }
        }
    }

    /**
     * CA-05 y CA-07: Al iniciar sesión, fusiona los ítems locales del invitado con los de la cuenta
     * en Firestore sin duplicar líneas (sumando cantidades).
     */
    override suspend fun sincronizarConCuenta(uid: String) {
        val fs = firestore ?: return
        try {
            val localItems = cartDao.obtenerTodos().associateBy { it.varianteId }.toMutableMap()
            val remoteSnapshot = fs.collection("clientes")
                .document(uid)
                .collection("carrito")
                .get()
                .await()

            for (doc in remoteSnapshot.documents) {
                val varianteId = doc.id
                val prendaId = doc.getString("prendaId").orEmpty()
                val talla = doc.getString("talla").orEmpty()
                val cantidadRemota = doc.getLong("cantidad")?.toInt() ?: 1
                val agregadoEnMillis = doc.getTimestamp("agregadoEn")?.toDate()?.time
                    ?: System.currentTimeMillis()

                val local = localItems[varianteId]
                if (local != null) {
                    // CA-07: Sumar la cantidad en lugar de crear otra línea
                    val cantidadFusionada = (local.cantidad + cantidadRemota).coerceAtMost(99)
                    val fusionado = local.copy(cantidad = cantidadFusionada)
                    localItems[varianteId] = fusionado
                    guardarEnFirestore(uid, fusionado)
                } else {
                    // En Firestore pero no en Room: descargarlo a Room
                    val nuevoLocal = CartEntity(
                        varianteId = varianteId,
                        prendaId = prendaId,
                        talla = talla,
                        colorNombre = "",
                        colorHex = "",
                        cantidad = cantidadRemota,
                        agregadoEnMillis = agregadoEnMillis,
                        precioAlAgregar = 0.0,
                    )
                    localItems[varianteId] = nuevoLocal
                }
            }

            // Los ítems que solo estaban en el dispositivo local se suben a Firestore
            for ((_, localItem) in localItems) {
                guardarEnFirestore(uid, localItem)
            }

            // Actualizar Room con el carrito unificado
            if (localItems.isNotEmpty()) {
                cartDao.insertarItems(localItems.values.toList())
            }

            // Escuchar cambios remotos en vivo para persistencia multi-dispositivo (CA-05)
            escucharCambiosRemotos(uid)
        } catch (e: Exception) {
            android.util.Log.e("RoomCartRepository", "Error al sincronizar con cuenta: ${e.message}", e)
        }
    }

    private fun escucharCambiosRemotos(uid: String) {
        val fs = firestore ?: return
        remoteListener?.remove()
        remoteListener = fs.collection("clientes")
            .document(uid)
            .collection("carrito")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    if (error != null) {
                        android.util.Log.e("RoomCartRepository", "Error en listener remoto: ${error.message}", error)
                    }
                    return@addSnapshotListener
                }
                scope.launch {
                    val remotos = snapshot.documents.mapNotNull { doc ->
                        val prendaId = doc.getString("prendaId") ?: return@mapNotNull null
                        val talla = doc.getString("talla") ?: return@mapNotNull null
                        val localExistente = cartDao.obtenerPorVarianteId(doc.id)
                        CartEntity(
                            varianteId = doc.id,
                            prendaId = prendaId,
                            talla = talla,
                            colorNombre = localExistente?.colorNombre.orEmpty(),
                            colorHex = localExistente?.colorHex.orEmpty(),
                            cantidad = doc.getLong("cantidad")?.toInt() ?: 1,
                            agregadoEnMillis = doc.getTimestamp("agregadoEn")?.toDate()?.time
                                ?: System.currentTimeMillis(),
                            precioAlAgregar = localExistente?.precioAlAgregar ?: 0.0,
                        )
                    }
                    if (remotos.isNotEmpty()) {
                        cartDao.insertarItems(remotos)
                    }
                }
            }
    }

    private suspend fun guardarEnFirestore(uid: String, item: CartEntity) {
        val fs = firestore ?: return
        try {
            // firestore.rules solo permite: ['prendaId', 'talla', 'cantidad', 'agregadoEn']
            val datos = mapOf(
                "prendaId" to item.prendaId,
                "talla" to item.talla,
                "cantidad" to item.cantidad.coerceIn(1, 99),
                "agregadoEn" to Timestamp(Instant.ofEpochMilli(item.agregadoEnMillis)),
            )
            fs.collection("clientes")
                .document(uid)
                .collection("carrito")
                .document(item.varianteId)
                .set(datos)
                .await()
            android.util.Log.d("RoomCartRepository", "Firestore guardó exitosamente ${item.varianteId} para $uid")
        } catch (e: Exception) {
            android.util.Log.e("RoomCartRepository", "Error al guardar ${item.varianteId} en Firestore: ${e.message}", e)
        }
    }
}
