package pe.modaviva.app.data.repository

import pe.modaviva.app.domain.model.TipoNotificacion
import pe.modaviva.app.domain.repository.NotificacionRepository
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import pe.modaviva.app.data.seed.PedidosSeedLoader
import pe.modaviva.app.domain.model.DatosTarjetaNueva
import pe.modaviva.app.domain.model.EstadoPedido
import pe.modaviva.app.domain.model.HitoPedido
import pe.modaviva.app.domain.model.ItemPedido
import pe.modaviva.app.domain.model.Pedido
import pe.modaviva.app.domain.model.ResultadoPago
import pe.modaviva.app.domain.model.ResultadoRegistroTarjeta
import pe.modaviva.app.domain.model.SolicitudPago
import pe.modaviva.app.domain.model.TarjetaGuardada
import pe.modaviva.app.domain.model.TipoMedioPago
import pe.modaviva.app.domain.repository.CartRepository
import pe.modaviva.app.domain.repository.CatalogoRepository
import pe.modaviva.app.domain.repository.OrderRepository
import pe.modaviva.app.domain.repository.SessionRepository
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
    private val notificaciones: NotificacionRepository,
    private val functions: FirebaseFunctions,
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

    override suspend fun pagarPedido(solicitud: SolicitudPago): Result<ResultadoPago> = runCatching {
        val tarjetaNueva = solicitud.tarjetaNueva
        val payload = mapOf(
            "idempotencyKey" to solicitud.idempotencyKey,
            "items" to solicitud.items.map {
                mapOf(
                    "prendaId" to it.prendaId,
                    "talla" to it.talla,
                    "cantidad" to it.cantidad,
                    "precioVisto" to it.precioVisto,
                )
            },
            "tipoEntrega" to solicitud.tipoEntrega,
            "distritoSlug" to solicitud.distritoSlug,
            "cupones" to solicitud.cupones,
            "entrega" to mapOf(
                "direccion" to solicitud.direccion,
                "referencia" to solicitud.referencia,
                "distrito" to solicitud.distrito,
                "tiendaRecojoId" to solicitud.tiendaRecojoId,
                "destinatario" to solicitud.destinatario,
                "telefono" to solicitud.telefono,
            ),
            "medioPago" to solicitud.medioPago.codigo,
            "pago" to mapOf(
                "tarjetaId" to solicitud.tarjetaGuardadaId,
                "tarjetaNueva" to tarjetaNueva?.let {
                    mapOf(
                        "numero" to it.numero,
                        "vencimiento" to it.vencimiento,
                        "cvv" to it.cvv,
                        "titular" to it.titular,
                    )
                },
                "guardarTarjeta" to solicitud.guardarTarjeta,
            ),
            "prendasProbadas" to solicitud.prendasProbadas,
            "totalEsperado" to solicitud.totalEsperado,
        )

        val res = try {
            functions.getHttpsCallable(CREAR_PEDIDO).call(payload).await().getData() as? Map<*, *>
        } catch (e: FirebaseFunctionsException) {
            // Los errores de validación del servidor traen un mensaje listo para mostrar.
            throw IllegalStateException(e.message ?: "No se pudo procesar el pago.", e)
        } ?: throw IllegalStateException("Respuesta vacía del servidor de pagos.")

        val pedidoId = res["pedidoId"] as? String
        val numero = res["numeroPedido"] as? String
        val resultado = when (res["estado"] as? String) {
            "APROBADO" -> ResultadoPago.Aprobado(pedidoId!!, numero!!)
            "PENDIENTE" -> ResultadoPago.Pendiente(pedidoId!!, numero!!)
            else -> ResultadoPago.Rechazado(
                res["motivo"] as? String ?: "La pasarela rechazó el pago.",
            )
        }

        // CA-07: con el pedido creado se vacía el carrito. Si el pago fue rechazado, se conserva (CA-09).
        if (resultado !is ResultadoPago.Rechazado) {
            cartRepository.vaciarCarrito()
        }
        resultado
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
        avisarCambioDeEstado(pedidoActual, pedidoActualizado)
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
        avisarCambioDeEstado(pedidoActual, pedidoActualizado)

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
        avisarCambioDeEstado(pedidoActual, pedidoActualizado)
        pedidoActualizado
    }

    /** HU-18: aviso cuando un pedido cambia de estado (solo si el estado cambió). */
    private fun avisarCambioDeEstado(anterior: Pedido, actual: Pedido) {
        if (anterior.estado == actual.estado) return
        val texto = when (actual.estado) {
            EstadoPedido.APROBADO -> "Tu pago fue confirmado."
            EstadoPedido.EN_PREPARACION -> if (actual.esRecojoEnTienda) {
                "Tu pedido está listo para recoger en ${actual.tiendaRecojoNombre ?: "tienda"}."
            } else {
                "Estamos preparando tus prendas."
            }
            EstadoPedido.DESPACHADO -> "Tu pedido va en camino."
            EstadoPedido.ENTREGADO -> "Tu pedido fue entregado. Tienes 7 días para solicitar cambio o devolución."
            EstadoPedido.ANULADO -> "La reserva venció sin pago y el pedido se anuló."
            EstadoPedido.PENDIENTE -> "Tu pedido está pendiente de pago."
        }
        notificaciones.crear(
            tipo = TipoNotificacion.PEDIDO,
            titulo = "Pedido ${actual.numeroPedido}: ${actual.estado.etiqueta}",
            texto = texto,
            refId = actual.id,
        )
    }

    override suspend fun registrarTarjeta(datos: DatosTarjetaNueva): Result<ResultadoRegistroTarjeta> = runCatching {
        val payload = mapOf(
            "numero" to datos.numero,
            "vencimiento" to datos.vencimiento,
            "cvv" to datos.cvv,
            "titular" to datos.titular,
            "tipo" to datos.tipo,
        )
        val res = try {
            functions.getHttpsCallable(REGISTRAR_TARJETA).call(payload).await().getData() as? Map<*, *>
        } catch (e: FirebaseFunctionsException) {
            throw IllegalStateException(e.message ?: "No se pudo registrar la tarjeta.", e)
        } ?: throw IllegalStateException("Respuesta vacía de la pasarela.")

        if (res["ok"] == true) {
            val t = res["tarjeta"] as? Map<*, *> ?: emptyMap<String, Any>()
            ResultadoRegistroTarjeta.Guardada(
                tarjetaId = t["id"] as? String ?: "",
                etiqueta = "${t["marca"] ?: ""} •••• ${t["ultimos4"] ?: ""}",
            )
        } else {
            ResultadoRegistroTarjeta.Rechazada(res["motivo"] as? String ?: "La pasarela rechazó la tarjeta.")
        }
    }

    override suspend fun establecerTarjetaPredeterminada(tarjetaId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado")
        val collection = firestore.collection(CLIENTES).document(uid).collection(METODOS_PAGO)
        val existentes = collection.get().await()
        val batch = firestore.batch()
        for (doc in existentes.documents) {
            val debeSer = doc.id == tarjetaId
            if (doc.getBoolean("esPredeterminada") != debeSer) {
                batch.update(doc.reference, "esPredeterminada", debeSer)
            }
        }
        batch.commit().await()
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
        )
    }

    private companion object {
        const val CLIENTES = "clientes"
        const val PEDIDOS = "pedidos"
        const val METODOS_PAGO = "metodosPago"
        const val CREAR_PEDIDO = "crearPedido"
        const val REGISTRAR_TARJETA = "registrarTarjeta"
    }
}
