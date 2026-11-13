package pe.modaviva.app.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import pe.modaviva.app.data.seed.PedidosSeedLoader
import pe.modaviva.app.domain.model.EstadoPedido
import pe.modaviva.app.domain.model.HitoPedido
import pe.modaviva.app.domain.model.ItemPedido
import pe.modaviva.app.domain.model.Pedido
import pe.modaviva.app.domain.model.SolicitudPostventa
import pe.modaviva.app.domain.model.TarjetaGuardada
import pe.modaviva.app.domain.model.TipoMedioPago
import pe.modaviva.app.domain.repository.CartRepository
import pe.modaviva.app.domain.repository.CatalogoRepository
import pe.modaviva.app.domain.repository.OrderRepository
import pe.modaviva.app.domain.repository.SessionRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreOrderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val cartRepository: CartRepository,
    private val catalogoRepository: CatalogoRepository,
    private val sessionRepository: SessionRepository,
) : OrderRepository {

    override fun observarPedidos(): Flow<List<Pedido>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(CLIENTES)
            .document(uid)
            .collection(PEDIDOS)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val pedidos = snapshot.documents.mapNotNull { doc ->
                    mapearPedido(doc.id, doc.data ?: return@mapNotNull null)
                }.sortedByDescending { it.creadoEnMillis }

                trySend(pedidos)
            }

        awaitClose { listener.remove() }
    }

    override fun observarPedidoPorId(pedidoId: String): Flow<Pedido?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(CLIENTES)
            .document(uid)
            .collection(PEDIDOS)
            .document(pedidoId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }

                val pedido = snapshot.data?.let { mapearPedido(snapshot.id, it) }
                trySend(pedido)
            }

        awaitClose { listener.remove() }
    }

    override fun observarTarjetasGuardadas(): Flow<List<TarjetaGuardada>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(CLIENTES)
            .document(uid)
            .collection(METODOS_PAGO)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val tarjetas = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    TarjetaGuardada(
                        id = doc.id,
                        marca = data["marca"] as? String ?: "Visa",
                        ultimos4 = data["ultimos4"] as? String ?: "",
                        vencimiento = data["vencimiento"] as? String ?: "",
                        titular = data["titular"] as? String ?: "",
                        tipo = data["tipo"] as? String ?: "CREDITO",
                        tokenPasarela = data["tokenPasarela"] as? String ?: "",
                        esPredeterminada = data["esPredeterminada"] as? Boolean ?: false,
                    )
                }.sortedByDescending { it.esPredeterminada }

                trySend(tarjetas)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun crearPedido(
        pedido: Pedido,
        idempotencyKey: String?,
    ): Result<Pedido> = runCatching {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado")
        val collection = firestore.collection(CLIENTES).document(uid).collection(PEDIDOS)

        val correlativo = if (pedido.numeroPedido.isNotBlank()) {
            pedido.numeroPedido
        } else {
            generarNumeroPedido()
        }

        val pedidoFinalId = if (pedido.id.isNotBlank()) pedido.id else "ped_${correlativo.lowercase()}"
        val ahora = System.currentTimeMillis()

        val hitosIniciales = if (pedido.hitos.isNotEmpty()) {
            pedido.hitos
        } else {
            listOf(
                HitoPedido(
                    estado = pedido.estado,
                    titulo = if (pedido.estado == EstadoPedido.APROBADO) "Pago aprobado" else "Reserva creada (30 min)",
                    descripcion = if (pedido.estado == EstadoPedido.APROBADO) {
                        "Pago confirmado mediante ${pedido.resumenMedioPago}. El stock ha sido reservado."
                    } else {
                        "Stock reservado temporalmente por 30 minutos mientras completas el pago."
                    },
                    fechaEpochMillis = ahora,
                )
            )
        }

        val venceEn = if (pedido.estado == EstadoPedido.PENDIENTE) {
            pedido.venceEnMillis ?: (ahora + Pedido.DURACION_RESERVA_MS)
        } else null

        val pedidoListo = pedido.copy(
            id = pedidoFinalId,
            numeroPedido = correlativo,
            uidCliente = uid,
            creadoEnMillis = if (pedido.creadoEnMillis > 0) pedido.creadoEnMillis else ahora,
            actualizadoEnMillis = ahora,
            venceEnMillis = venceEn,
            hitos = hitosIniciales,
        )

        collection.document(pedidoFinalId).set(pedidoAMap(pedidoListo)).await()

        // Si el pedido fue Aprobado o Pendiente, se vacía el carrito tras confirmar la compra (HU-21 CA-07)
        if (pedidoListo.estado == EstadoPedido.APROBADO || pedidoListo.estado == EstadoPedido.PENDIENTE) {
            cartRepository.vaciarCarrito()
        }

        pedidoListo
    }

    override suspend fun confirmarPagoPedidoPendiente(
        pedidoId: String,
        medioPago: TipoMedioPago,
        resumenMedioPago: String,
    ): Result<Pedido> = runCatching {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado")
        val docRef = firestore.collection(CLIENTES).document(uid).collection(PEDIDOS).document(pedidoId)
        val snapshot = docRef.get().await()
        if (!snapshot.exists()) throw IllegalArgumentException("Pedido no encontrado")

        val pedidoActual = mapearPedido(snapshot.id, snapshot.data!!)
            ?: throw IllegalStateException("Error al leer datos del pedido")

        val ahora = System.currentTimeMillis()
        val nuevoHito = HitoPedido(
            estado = EstadoPedido.APROBADO,
            titulo = "Pago aprobado",
            descripcion = "Pago regularizado y confirmado mediante $resumenMedioPago.",
            fechaEpochMillis = ahora,
        )

        val pedidoActualizado = pedidoActual.copy(
            estado = EstadoPedido.APROBADO,
            medioPago = medioPago,
            resumenMedioPago = resumenMedioPago,
            actualizadoEnMillis = ahora,
            venceEnMillis = null,
            hitos = pedidoActual.hitos + nuevoHito,
        )

        docRef.set(pedidoAMap(pedidoActualizado), SetOptions.merge()).await()
        pedidoActualizado
    }

    override suspend fun anularPedidoVencido(pedidoId: String): Result<Pedido> = runCatching {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado")
        val docRef = firestore.collection(CLIENTES).document(uid).collection(PEDIDOS).document(pedidoId)
        val snapshot = docRef.get().await()
        if (!snapshot.exists()) throw IllegalArgumentException("Pedido no encontrado")

        val pedidoActual = mapearPedido(snapshot.id, snapshot.data!!)
            ?: throw IllegalStateException("Error al leer datos del pedido")

        val ahora = System.currentTimeMillis()
        val nuevoHito = HitoPedido(
            estado = EstadoPedido.ANULADO,
            titulo = "Pedido anulado por vencimiento",
            descripcion = "Transcurrieron los 30 minutos sin confirmación de pago. El stock ha sido liberado.",
            fechaEpochMillis = ahora,
        )

        val pedidoActualizado = pedidoActual.copy(
            estado = EstadoPedido.ANULADO,
            actualizadoEnMillis = ahora,
            hitos = pedidoActual.hitos + nuevoHito,
        )

        docRef.set(pedidoAMap(pedidoActualizado), SetOptions.merge()).await()

        // HU-22 CA-05: Al anularse, devolver prendas con stock al carrito
        for (item in pedidoActual.items) {
            cartRepository.agregarAlCarrito(
                prendaId = item.prendaId,
                talla = item.talla,
                colorNombre = "Predeterminado",
                colorHex = "#000000",
                cantidad = item.cantidad,
                precioAlAgregar = item.precioUnitario,
            )
        }

        pedidoActualizado
    }

    override suspend fun avanzarEstadoDemo(
        pedidoId: String,
        nuevoEstado: EstadoPedido,
    ): Result<Pedido> = runCatching {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado")
        val docRef = firestore.collection(CLIENTES).document(uid).collection(PEDIDOS).document(pedidoId)
        val snapshot = docRef.get().await()
        if (!snapshot.exists()) throw IllegalArgumentException("Pedido no encontrado")

        val pedidoActual = mapearPedido(snapshot.id, snapshot.data!!)
            ?: throw IllegalStateException("Error al leer datos del pedido")

        val ahora = System.currentTimeMillis()
        val (titulo, desc) = when (nuevoEstado) {
            EstadoPedido.PENDIENTE -> "Reserva pendiente" to "Reserva temporal activa por 30 minutos."
            EstadoPedido.APROBADO -> "Pago aprobado" to "Pago confirmado y stock asignado."
            EstadoPedido.EN_PREPARACION -> "En preparación" to if (pedidoActual.esRecojoEnTienda) {
                "¡Listo para recoger en tienda! Te esperamos en ${pedidoActual.tiendaRecojoNombre ?: "nuestra sede"}."
            } else {
                "Almacén central está empaquetando tus prendas."
            }
            EstadoPedido.DESPACHADO -> "Pedido despachado" to "En camino con Olva Courier (Guía: OLVA-${(ahora % 900000) + 100000}-PE)."
            EstadoPedido.ENTREGADO -> "Pedido entregado" to if (pedidoActual.esRecojoEnTienda) {
                "Recogido con éxito en ${pedidoActual.tiendaRecojoNombre ?: "tienda"}."
            } else {
                "Entregado en ${pedidoActual.direccionEntrega}."
            }
            EstadoPedido.ANULADO -> "Pedido anulado" to "Reserva liberada por falta de pago."
        }

        val nuevoHito = HitoPedido(
            estado = nuevoEstado,
            titulo = titulo,
            descripcion = desc,
            fechaEpochMillis = ahora,
        )

        val pedidoActualizado = pedidoActual.copy(
            estado = nuevoEstado,
            actualizadoEnMillis = ahora,
            entregadoEnMillis = if (nuevoEstado == EstadoPedido.ENTREGADO) ahora else pedidoActual.entregadoEnMillis,
            empresaTransporte = if (nuevoEstado == EstadoPedido.DESPACHADO && !pedidoActual.esRecojoEnTienda) "Olva Courier" else pedidoActual.empresaTransporte,
            numeroSeguimiento = if (nuevoEstado == EstadoPedido.DESPACHADO && !pedidoActual.esRecojoEnTienda) "OLVA-${(ahora % 900000) + 100000}-PE" else pedidoActual.numeroSeguimiento,
            listoParaRecoger = pedidoActual.esRecojoEnTienda && (nuevoEstado == EstadoPedido.EN_PREPARACION || nuevoEstado == EstadoPedido.ENTREGADO),
            hitos = pedidoActual.hitos + nuevoHito,
        )

        docRef.set(pedidoAMap(pedidoActualizado), SetOptions.merge()).await()
        pedidoActualizado
    }

    override suspend fun registrarSolicitudPostventa(
        pedidoId: String,
        solicitud: SolicitudPostventa,
    ): Result<SolicitudPostventa> = runCatching {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado")
        val docRef = firestore.collection(CLIENTES).document(uid).collection(PEDIDOS).document(pedidoId)
        val snapshot = docRef.get().await()
        if (!snapshot.exists()) throw IllegalArgumentException("Pedido no encontrado")

        val numSolicitud = "SOL-${(System.currentTimeMillis() % 900000) + 100000}"
        val solicitudFinal = solicitud.copy(
            id = "sol_${System.currentTimeMillis()}",
            numeroSolicitud = numSolicitud,
            pedidoId = pedidoId,
            estado = "EN_EVALUACION",
            creadaEnMillis = System.currentTimeMillis(),
        )

        docRef.update("solicitudPostventa", solicitudAMap(solicitudFinal)).await()
        solicitudFinal
    }

    override suspend fun guardarTarjeta(tarjeta: TarjetaGuardada): Result<TarjetaGuardada> = runCatching {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado")
        val collection = firestore.collection(CLIENTES).document(uid).collection(METODOS_PAGO)

        val existing = collection.get().await()
        val esPrimera = existing.isEmpty
        val tarjetaId = if (tarjeta.id.isNotBlank()) tarjeta.id else "card_${tarjeta.marca.lowercase()}_${tarjeta.ultimos4}"
        val debeSerPredeterminada = tarjeta.esPredeterminada || esPrimera

        if (debeSerPredeterminada && !existing.isEmpty) {
            val batch = firestore.batch()
            for (doc in existing.documents) {
                if (doc.id != tarjetaId) {
                    batch.update(doc.reference, "esPredeterminada", false)
                }
            }
            batch.commit().await()
        }

        val tarjetaFinal = tarjeta.copy(id = tarjetaId, esPredeterminada = debeSerPredeterminada)
        val data = mapOf(
            "marca" to tarjetaFinal.marca,
            "ultimos4" to tarjetaFinal.ultimos4,
            "vencimiento" to tarjetaFinal.vencimiento,
            "titular" to tarjetaFinal.titular,
            "tipo" to tarjetaFinal.tipo,
            "tokenPasarela" to tarjetaFinal.tokenPasarela,
            "esPredeterminada" to tarjetaFinal.esPredeterminada,
            "actualizadoEn" to FieldValue.serverTimestamp(),
        )
        collection.document(tarjetaId).set(data, SetOptions.merge()).await()
        tarjetaFinal
    }

    override suspend fun eliminarTarjeta(tarjetaId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado")
        val collection = firestore.collection(CLIENTES).document(uid).collection(METODOS_PAGO)
        val docRef = collection.document(tarjetaId)
        val snapshot = docRef.get().await()
        val eraPredeterminada = snapshot.getBoolean("esPredeterminada") == true

        docRef.delete().await()

        if (eraPredeterminada) {
            val remaining = collection.get().await()
            if (!remaining.isEmpty) {
                remaining.documents.first().reference.update("esPredeterminada", true).await()
            }
        }
    }

    override suspend fun cargarDatosSemilla(): Result<Int> = runCatching {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado")
        val userProfile = sessionRepository.currentUser.value
        val nombreCliente = userProfile?.let { "${it.nombres} ${it.apellidos}".trim() }.orEmpty()
        val catalogo = catalogoRepository.prendas.first().associateBy { it.codigo }

        val semilla = PedidosSeedLoader.cargarDesdeAssets(
            context = context,
            uidCliente = uid,
            nombreCliente = nombreCliente,
            catalogoPorCodigo = catalogo,
        )

        val batch = firestore.batch()
        val pedidosCol = firestore.collection(CLIENTES).document(uid).collection(PEDIDOS)
        val tarjetasCol = firestore.collection(CLIENTES).document(uid).collection(METODOS_PAGO)

        for (tarjeta in semilla.tarjetas) {
            tarjetasCol.document(tarjeta.id).set(
                mapOf(
                    "marca" to tarjeta.marca,
                    "ultimos4" to tarjeta.ultimos4,
                    "vencimiento" to tarjeta.vencimiento,
                    "titular" to tarjeta.titular,
                    "tipo" to tarjeta.tipo,
                    "tokenPasarela" to tarjeta.tokenPasarela,
                    "esPredeterminada" to tarjeta.esPredeterminada,
                    "creadoEn" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge()
            )
        }

        for (pedido in semilla.pedidos) {
            pedidosCol.document(pedido.id).set(pedidoAMap(pedido), SetOptions.merge())
        }

        semilla.pedidos.size
    }

    private fun generarNumeroPedido(): String {
        val fecha = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("America/Lima")).format(Instant.now())
        val randomSuffix = (1000..9999).random()
        return "MV-$fecha-$randomSuffix"
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapearPedido(id: String, data: Map<String, Any?>): Pedido? {
        val rawItems = data["items"] as? List<Map<String, Any?>> ?: emptyList()
        val items = rawItems.map { m ->
            ItemPedido(
                prendaId = m["prendaId"] as? String ?: "",
                codigo = m["codigo"] as? String ?: "",
                nombre = m["nombre"] as? String ?: "",
                fotoUrl = m["fotoUrl"] as? String ?: "",
                talla = m["talla"] as? String ?: "",
                cantidad = (m["cantidad"] as? Number)?.toInt() ?: 1,
                precioUnitario = (m["precioUnitario"] as? Number)?.toDouble() ?: 0.0,
                subtotal = (m["subtotal"] as? Number)?.toDouble() ?: 0.0,
                usoProbadorVirtual = m["usoProbadorVirtual"] as? Boolean ?: false,
            )
        }

        val rawHitos = data["hitos"] as? List<Map<String, Any?>> ?: emptyList()
        val hitos = rawHitos.map { m ->
            HitoPedido(
                estado = EstadoPedido.desdeCodigo(m["estado"] as? String),
                titulo = m["titulo"] as? String ?: "",
                descripcion = m["descripcion"] as? String ?: "",
                fechaEpochMillis = (m["fechaEpochMillis"] as? Number)?.toLong() ?: 0L,
            )
        }

        val rawPostventa = data["solicitudPostventa"] as? Map<String, Any?>
        val postventa = rawPostventa?.let { m ->
            SolicitudPostventa(
                id = m["id"] as? String ?: "",
                numeroSolicitud = m["numeroSolicitud"] as? String ?: "",
                pedidoId = m["pedidoId"] as? String ?: id,
                prendaCodigo = m["prendaCodigo"] as? String ?: "",
                prendaNombre = m["prendaNombre"] as? String ?: "",
                talla = m["talla"] as? String ?: "",
                tipo = m["tipo"] as? String ?: "CAMBIO",
                motivo = m["motivo"] as? String ?: "",
                detalleOtro = m["detalleOtro"] as? String,
                fotosUrls = (m["fotosUrls"] as? List<String>) ?: emptyList(),
                tuvoPruebaVirtual = m["tuvoPruebaVirtual"] as? Boolean ?: false,
                estado = m["estado"] as? String ?: "EN_EVALUACION",
                resolucion = m["resolucion"] as? String,
                creadaEnMillis = (m["creadaEnMillis"] as? Number)?.toLong() ?: 0L,
            )
        }

        return Pedido(
            id = id,
            numeroPedido = data["numeroPedido"] as? String ?: id,
            uidCliente = data["uidCliente"] as? String ?: "",
            estado = EstadoPedido.desdeCodigo(data["estado"] as? String),
            tipoEntrega = data["tipoEntrega"] as? String ?: "delivery",
            direccionEntrega = data["direccionEntrega"] as? String ?: "",
            referenciaEntrega = data["referenciaEntrega"] as? String,
            distrito = data["distrito"] as? String ?: "",
            distritoSlug = data["distritoSlug"] as? String ?: "",
            tiendaRecojoId = data["tiendaRecojoId"] as? String,
            tiendaRecojoNombre = data["tiendaRecojoNombre"] as? String,
            tiendaRecojoDireccion = data["tiendaRecojoDireccion"] as? String,
            tiendaRecojoHorario = data["tiendaRecojoHorario"] as? String,
            destinatario = data["destinatario"] as? String ?: "",
            telefonoContacto = data["telefonoContacto"] as? String ?: "",
            medioPago = TipoMedioPago.desdeCodigo(data["medioPago"] as? String),
            resumenMedioPago = data["resumenMedioPago"] as? String ?: "",
            items = items,
            subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
            descuentoTotal = (data["descuentoTotal"] as? Number)?.toDouble() ?: 0.0,
            costoEnvio = (data["costoEnvio"] as? Number)?.toDouble() ?: 0.0,
            total = (data["total"] as? Number)?.toDouble() ?: 0.0,
            cuponAplicado = data["cuponAplicado"] as? String,
            creadoEnMillis = (data["creadoEnMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            actualizadoEnMillis = (data["actualizadoEnMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            venceEnMillis = (data["venceEnMillis"] as? Number)?.toLong(),
            entregadoEnMillis = (data["entregadoEnMillis"] as? Number)?.toLong(),
            empresaTransporte = data["empresaTransporte"] as? String,
            numeroSeguimiento = data["numeroSeguimiento"] as? String,
            listoParaRecoger = data["listoParaRecoger"] as? Boolean ?: false,
            hitos = hitos,
            solicitudPostventa = postventa,
        )
    }

    private fun pedidoAMap(pedido: Pedido): Map<String, Any?> {
        return mapOf(
            "id" to pedido.id,
            "numeroPedido" to pedido.numeroPedido,
            "uidCliente" to pedido.uidCliente,
            "estado" to pedido.estado.codigo,
            "tipoEntrega" to pedido.tipoEntrega,
            "direccionEntrega" to pedido.direccionEntrega,
            "referenciaEntrega" to pedido.referenciaEntrega,
            "distrito" to pedido.distrito,
            "distritoSlug" to pedido.distritoSlug,
            "tiendaRecojoId" to pedido.tiendaRecojoId,
            "tiendaRecojoNombre" to pedido.tiendaRecojoNombre,
            "tiendaRecojoDireccion" to pedido.tiendaRecojoDireccion,
            "tiendaRecojoHorario" to pedido.tiendaRecojoHorario,
            "destinatario" to pedido.destinatario,
            "telefonoContacto" to pedido.telefonoContacto,
            "medioPago" to pedido.medioPago.codigo,
            "resumenMedioPago" to pedido.resumenMedioPago,
            "subtotal" to pedido.subtotal,
            "descuentoTotal" to pedido.descuentoTotal,
            "costoEnvio" to pedido.costoEnvio,
            "total" to pedido.total,
            "cuponAplicado" to pedido.cuponAplicado,
            "creadoEnMillis" to pedido.creadoEnMillis,
            "actualizadoEnMillis" to pedido.actualizadoEnMillis,
            "venceEnMillis" to pedido.venceEnMillis,
            "entregadoEnMillis" to pedido.entregadoEnMillis,
            "empresaTransporte" to pedido.empresaTransporte,
            "numeroSeguimiento" to pedido.numeroSeguimiento,
            "listoParaRecoger" to pedido.listoParaRecoger,
            "items" to pedido.items.map {
                mapOf(
                    "prendaId" to it.prendaId,
                    "codigo" to it.codigo,
                    "nombre" to it.nombre,
                    "fotoUrl" to it.fotoUrl,
                    "talla" to it.talla,
                    "cantidad" to it.cantidad,
                    "precioUnitario" to it.precioUnitario,
                    "subtotal" to it.subtotal,
                    "usoProbadorVirtual" to it.usoProbadorVirtual,
                )
            },
            "hitos" to pedido.hitos.map {
                mapOf(
                    "estado" to it.estado.codigo,
                    "titulo" to it.titulo,
                    "descripcion" to it.descripcion,
                    "fechaEpochMillis" to it.fechaEpochMillis,
                )
            },
            "solicitudPostventa" to pedido.solicitudPostventa?.let { solicitudAMap(it) },
        )
    }

    private fun solicitudAMap(s: SolicitudPostventa): Map<String, Any?> {
        return mapOf(
            "id" to s.id,
            "numeroSolicitud" to s.numeroSolicitud,
            "pedidoId" to s.pedidoId,
            "prendaCodigo" to s.prendaCodigo,
            "prendaNombre" to s.prendaNombre,
            "talla" to s.talla,
            "tipo" to s.tipo,
            "motivo" to s.motivo,
            "detalleOtro" to s.detalleOtro,
            "fotosUrls" to s.fotosUrls,
            "tuvoPruebaVirtual" to s.tuvoPruebaVirtual,
            "estado" to s.estado,
            "resolucion" to s.resolucion,
            "creadaEnMillis" to s.creadaEnMillis,
        )
    }

    private companion object {
        const val CLIENTES = "clientes"
        const val PEDIDOS = "pedidos"
        const val METODOS_PAGO = "metodosPago"
    }
}
