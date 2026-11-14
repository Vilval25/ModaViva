package pe.modaviva.app.data.repository

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.modaviva.app.data.local.CartDao
import pe.modaviva.app.data.local.CartEntity
import pe.modaviva.app.data.local.aDominio
import pe.modaviva.app.data.remote.await
import pe.modaviva.app.domain.model.CartItem
import pe.modaviva.app.domain.repository.CartRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

internal data class CartRemoteItem(
    val varianteId: String,
    val prendaId: String,
    val talla: String,
    val cantidad: Int,
    val agregadoEnMillis: Long,
) {
    fun aCartEntity(localExistente: CartEntity?): CartEntity = CartEntity(
        varianteId = varianteId,
        prendaId = prendaId,
        talla = talla,
        colorNombre = localExistente?.colorNombre.orEmpty(),
        colorHex = localExistente?.colorHex.orEmpty(),
        cantidad = cantidad,
        agregadoEnMillis = agregadoEnMillis,
        precioAlAgregar = localExistente?.precioAlAgregar ?: 0.0,
    )
}

internal interface CartOwnerStore {
    fun getOwnerUid(): String?
    fun setOwnerUid(uid: String?)
}

internal interface CartRemoteDataSource {
    suspend fun obtenerCarrito(uid: String): List<CartRemoteItem>
    suspend fun guardarItem(uid: String, item: CartEntity)
    suspend fun eliminarItem(uid: String, varianteId: String)
    suspend fun vaciarCarrito(uid: String)
    fun observarCambios(uid: String, onItems: (List<CartRemoteItem>) -> Unit): AutoCloseable?
}

@Singleton
class RoomCartRepository internal constructor(
    private val cartDao: CartDao,
    private val remoteDataSource: CartRemoteDataSource?,
    private val ownerStore: CartOwnerStore,
    private val currentUidProvider: () -> String?,
    private val initialUid: String?,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    firebaseAuth: FirebaseAuth? = null,
) : CartRepository {

    @Inject
    constructor(
        cartDao: CartDao,
        firestore: FirebaseFirestore? = null,
        firebaseAuth: FirebaseAuth? = null,
        @ApplicationContext context: Context? = null,
    ) : this(
        cartDao = cartDao,
        remoteDataSource = firestore?.let(::FirestoreCartRemoteDataSource),
        ownerStore = if (context != null) {
            SharedPrefsCartOwnerStore(context)
        } else {
            InMemoryCartOwnerStore()
        },
        currentUidProvider = { firebaseAuth?.currentUser?.uid },
        initialUid = firebaseAuth?.currentUser?.uid,
        firebaseAuth = firebaseAuth,
    )

    private val syncMutex = Mutex()
    private var remoteSubscription: AutoCloseable? = null
    private var lastSyncedUid: String? = null

    init {
        firebaseAuth?.addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            scope.launch {
                onAuthStateChanged(uid)
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

        val uid = currentUidProvider()
        val remote = remoteDataSource
        if (uid != null && remote != null) {
            val itemActualizado = cartDao.obtenerPorVarianteId(varianteId) ?: entity
            remote.guardarItem(uid, itemActualizado)
        }
    }

    override suspend fun actualizarCantidad(varianteId: String, cantidad: Int) {
        if (cantidad <= 0) {
            eliminarItem(varianteId)
            return
        }
        cartDao.actualizarCantidad(varianteId, cantidad)
        val uid = currentUidProvider()
        val remote = remoteDataSource
        if (uid != null && remote != null) {
            val item = cartDao.obtenerPorVarianteId(varianteId)
            if (item != null) {
                remote.guardarItem(uid, item)
            }
        }
    }

    override suspend fun eliminarItem(varianteId: String) {
        cartDao.eliminarItem(varianteId)
        val uid = currentUidProvider()
        val remote = remoteDataSource
        if (uid != null && remote != null) {
            runCatching { remote.eliminarItem(uid, varianteId) }
        }
    }

    override suspend fun vaciarCarrito() {
        cartDao.vaciarCarrito()
        val uid = currentUidProvider()
        val remote = remoteDataSource
        if (uid != null && remote != null) {
            runCatching { remote.vaciarCarrito(uid) }
        }
    }

    /**
     * CA-05 y CA-07: Sincroniza el carrito local con el de Firestore.
     * Solo fusiona sumando cantidades cuando el carrito local pertenece a un invitado que
     * acaba de iniciar sesión; en reaperturas con la misma cuenta reconcilia sin duplicar.
     */
    override suspend fun sincronizarConCuenta(uid: String) {
        syncMutex.withLock {
            lastSyncedUid = uid
            sincronizarConCuentaLocked(uid)
        }
    }

    internal suspend fun onAuthStateChanged(uid: String?) {
        syncMutex.withLock {
            if (uid != null) {
                if (lastSyncedUid != uid) {
                    lastSyncedUid = uid
                    sincronizarConCuentaLocked(uid)
                }
            } else {
                // Al cerrar sesión: el carrito de la cuenta queda a salvo en Firestore,
                // y limpiamos el carrito local para proteger la privacidad del usuario.
                val hadSession = lastSyncedUid != null || ownerStore.getOwnerUid() != null
                lastSyncedUid = null
                remoteSubscription?.close()
                remoteSubscription = null
                if (hadSession) {
                    ownerStore.setOwnerUid(null)
                    cartDao.vaciarCarrito()
                }
            }
        }
    }

    private suspend fun sincronizarConCuentaLocked(uid: String) {
        val remote = remoteDataSource ?: return
        try {
            val persistedOwner = ownerStore.getOwnerUid()
            // Si la sesión ya estaba activa al abrir la app y no existía owner persistido
            // (migración desde una versión previa), no se trata como invitado nuevo para evitar duplicar.
            val legacySameUserAtStartup = (persistedOwner == null && initialUid == uid)
            if (persistedOwner != null && persistedOwner != uid) {
                ownerStore.setOwnerUid(null)
                cartDao.vaciarCarrito()
            }

            val shouldMergeGuestItems = (persistedOwner == null && !legacySameUserAtStartup)
            val remoteItems = remote.obtenerCarrito(uid)
            val localByVariant = cartDao.obtenerTodos().associateBy { it.varianteId }

            val unifiedItems = if (shouldMergeGuestItems) {
                fusionarInvitadoConRemoto(uid, localByVariant, remoteItems, remote)
            } else {
                remoteItems.map { it.aCartEntity(localByVariant[it.varianteId]) }
            }

            cartDao.reemplazarCarrito(unifiedItems)
            ownerStore.setOwnerUid(uid)

            // Escuchar cambios remotos en vivo para persistencia multi-dispositivo (CA-05)
            escucharCambiosRemotosLocked(uid)
        } catch (e: Exception) {
            logError("Error al sincronizar con cuenta: ${e.message}", e)
        }
    }

    private suspend fun fusionarInvitadoConRemoto(
        uid: String,
        localByVariant: Map<String, CartEntity>,
        remoteItems: List<CartRemoteItem>,
        remote: CartRemoteDataSource,
    ): List<CartEntity> {
        val merged = localByVariant.toMutableMap()
        val remoteVariants = remoteItems.mapTo(mutableSetOf()) { it.varianteId }

        for (remoteItem in remoteItems) {
            val local = merged[remoteItem.varianteId]
            if (local != null) {
                // CA-07: Sumar la cantidad en lugar de crear otra línea
                val cantidadFusionada = (local.cantidad + remoteItem.cantidad).coerceAtMost(99)
                val fusionado = local.copy(cantidad = cantidadFusionada)
                merged[remoteItem.varianteId] = fusionado
                remote.guardarItem(uid, fusionado)
            } else {
                merged[remoteItem.varianteId] = remoteItem.aCartEntity(localExistente = null)
            }
        }

        // Los ítems que solo estaban en el dispositivo local del invitado se suben a Firestore
        for ((varianteId, localItem) in merged) {
            if (varianteId !in remoteVariants) {
                remote.guardarItem(uid, localItem)
            }
        }

        return merged.values.toList()
    }

    private fun escucharCambiosRemotosLocked(uid: String) {
        val remote = remoteDataSource ?: return
        remoteSubscription?.close()
        remoteSubscription = remote.observarCambios(uid) { remotos ->
            scope.launch {
                syncMutex.withLock {
                    if (lastSyncedUid != uid) return@withLock
                    val localByVariant = cartDao.obtenerTodos().associateBy { it.varianteId }
                    val entidades = remotos.map { it.aCartEntity(localByVariant[it.varianteId]) }
                    cartDao.reemplazarCarrito(entidades)
                }
            }
        }
    }

    private companion object {
        const val TAG = "RoomCartRepository"

        fun logError(message: String, error: Throwable? = null) {
            runCatching { android.util.Log.e(TAG, message, error) }
        }
    }
}

private class InMemoryCartOwnerStore : CartOwnerStore {
    private var ownerUid: String? = null
    override fun getOwnerUid(): String? = ownerUid
    override fun setOwnerUid(uid: String?) {
        ownerUid = uid
    }
}

private class SharedPrefsCartOwnerStore(context: Context) : CartOwnerStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getOwnerUid(): String? = prefs.getString(KEY_SYNCED_OWNER_UID, null)

    override fun setOwnerUid(uid: String?) {
        prefs.edit().apply {
            if (uid == null) remove(KEY_SYNCED_OWNER_UID) else putString(KEY_SYNCED_OWNER_UID, uid)
        }.apply()
    }

    private companion object {
        const val PREFS_NAME = "modaviva_cart_sync"
        const val KEY_SYNCED_OWNER_UID = "synced_owner_uid"
    }
}

private class FirestoreCartRemoteDataSource(
    private val firestore: FirebaseFirestore,
) : CartRemoteDataSource {

    private fun carritoCollection(uid: String) =
        firestore.collection("clientes").document(uid).collection("carrito")

    override suspend fun obtenerCarrito(uid: String): List<CartRemoteItem> {
        val snapshot = carritoCollection(uid).get().await()
        return snapshot.documents.mapNotNull { it.aCartRemoteItem() }
    }

    override suspend fun guardarItem(uid: String, item: CartEntity) {
        try {
            // firestore.rules solo permite: ['prendaId', 'talla', 'cantidad', 'agregadoEn']
            val datos = mapOf(
                "prendaId" to item.prendaId,
                "talla" to item.talla,
                "cantidad" to item.cantidad.coerceIn(1, 99),
                "agregadoEn" to Timestamp(Instant.ofEpochMilli(item.agregadoEnMillis)),
            )
            carritoCollection(uid).document(item.varianteId).set(datos).await()
            runCatching {
                android.util.Log.d(TAG, "Firestore guardó exitosamente ${item.varianteId} para $uid")
            }
        } catch (e: Exception) {
            runCatching {
                android.util.Log.e(TAG, "Error al guardar ${item.varianteId} en Firestore: ${e.message}", e)
            }
        }
    }

    override suspend fun eliminarItem(uid: String, varianteId: String) {
        carritoCollection(uid).document(varianteId).delete().await()
    }

    override suspend fun vaciarCarrito(uid: String) {
        val snapshot = carritoCollection(uid).get().await()
        for (doc in snapshot.documents) {
            doc.reference.delete().await()
        }
    }

    override fun observarCambios(uid: String, onItems: (List<CartRemoteItem>) -> Unit): AutoCloseable {
        val registration = carritoCollection(uid).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                if (error != null) {
                    runCatching {
                        android.util.Log.e(TAG, "Error en listener remoto: ${error.message}", error)
                    }
                }
                return@addSnapshotListener
            }
            onItems(snapshot.documents.mapNotNull { it.aCartRemoteItem() })
        }
        return AutoCloseable { registration.remove() }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.aCartRemoteItem(): CartRemoteItem? {
        val prendaId = getString("prendaId") ?: return null
        val talla = getString("talla") ?: return null
        return CartRemoteItem(
            varianteId = id,
            prendaId = prendaId,
            talla = talla,
            cantidad = getLong("cantidad")?.toInt() ?: 1,
            agregadoEnMillis = getTimestamp("agregadoEn")?.toDate()?.time
                ?: System.currentTimeMillis(),
        )
    }

    private companion object {
        const val TAG = "RoomCartRepository"
    }
}
