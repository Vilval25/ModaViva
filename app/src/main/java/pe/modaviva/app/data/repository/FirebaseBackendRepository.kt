package pe.modaviva.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import pe.modaviva.app.data.remote.await
import pe.modaviva.app.domain.model.BackendPing
import pe.modaviva.app.domain.model.CuponAplicado
import pe.modaviva.app.domain.model.ItemPedidoCalculado
import pe.modaviva.app.domain.model.ResultadoCalculoPedido
import pe.modaviva.app.domain.model.SolicitudCalculoPedido
import pe.modaviva.app.domain.model.TarifaEnvio
import pe.modaviva.app.domain.repository.BackendRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Acceso a las Cloud Functions del proyecto (functions/ en la raíz del repo)
 * y lectura de tarifas de envío desde Firestore.
 */
@Singleton
class FirebaseBackendRepository @Inject constructor(
    private val functions: FirebaseFunctions,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : BackendRepository {

    override suspend fun ping(): Result<BackendPing> = runCatching {
        val data = functions.getHttpsCallable(PING).call().await().getData() as? Map<*, *>
        check(data?.get("ok") == true) { "Respuesta inesperada de $PING: $data" }
        BackendPing(
            message = data["message"] as? String ?: "",
            serverTime = data["serverTime"] as? String ?: "",
        )
    }

    override suspend fun calcularPedido(solicitud: SolicitudCalculoPedido): Result<ResultadoCalculoPedido> = runCatching {
        val payload = mapOf(
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
        )

        val res = functions.getHttpsCallable(CALCULAR_PEDIDO).call(payload).await().getData() as? Map<*, *>
            ?: throw IllegalStateException("Respuesta vacía de $CALCULAR_PEDIDO")

        val rawItems = (res["items"] as? List<*>) ?: emptyList<Any>()
        val itemsParsed = rawItems.mapNotNull { itemRaw ->
            val m = itemRaw as? Map<*, *> ?: return@mapNotNull null
            ItemPedidoCalculado(
                prendaId = m["prendaId"] as? String ?: "",
                codigo = m["codigo"] as? String ?: "",
                nombre = m["nombre"] as? String ?: "",
                fotoUrl = m["fotoUrl"] as? String ?: "",
                talla = m["talla"] as? String ?: "",
                cantidadOriginal = (m["cantidadOriginal"] as? Number)?.toInt() ?: 0,
                cantidadAprobada = (m["cantidadAprobada"] as? Number)?.toInt() ?: 0,
                precioUnitario = (m["precioUnitario"] as? Number)?.toDouble() ?: 0.0,
                subtotal = (m["subtotal"] as? Number)?.toDouble() ?: 0.0,
                agotado = m["agotado"] as? Boolean ?: false,
                cambioPrecio = m["cambioPrecio"] as? Boolean ?: false,
                precioAnterior = (m["precioAnterior"] as? Number)?.toDouble(),
            )
        }

        val rawCupones = (res["cuponesAplicados"] as? List<*>) ?: emptyList<Any>()
        val cuponesParsed = rawCupones.mapNotNull { cupRaw ->
            val m = cupRaw as? Map<*, *> ?: return@mapNotNull null
            CuponAplicado(
                codigo = m["codigo"] as? String ?: "",
                tipo = m["tipo"] as? String ?: "monto",
                valor = (m["valor"] as? Number)?.toDouble() ?: 0.0,
                descuentoCalculado = (m["descuentoCalculado"] as? Number)?.toDouble() ?: 0.0,
            )
        }

        val erroresCupon = ((res["erroresCupon"] as? List<*>)?.mapNotNull { it as? String }) ?: emptyList()
        val alertas = ((res["alertas"] as? List<*>)?.mapNotNull { it as? String }) ?: emptyList()

        ResultadoCalculoPedido(
            subtotal = (res["subtotal"] as? Number)?.toDouble() ?: 0.0,
            descuentoTotal = (res["descuentoTotal"] as? Number)?.toDouble() ?: 0.0,
            costoEnvio = (res["costoEnvio"] as? Number)?.toDouble() ?: 0.0,
            total = (res["total"] as? Number)?.toDouble() ?: 0.0,
            tipoEntrega = res["tipoEntrega"] as? String ?: solicitud.tipoEntrega,
            distrito = res["distrito"] as? String ?: "",
            plazoDias = (res["plazoDias"] as? Number)?.toInt() ?: 0,
            items = itemsParsed,
            cuponesAplicados = cuponesParsed,
            erroresCupon = erroresCupon,
            alertas = alertas,
            totalPrendasAprobadas = (res["totalPrendasAprobadas"] as? Number)?.toInt() ?: 0,
        )
    }

    override suspend fun obtenerTarifasEnvio(): Result<List<TarifaEnvio>> = runCatching {
        val snapshot = firestore.collection("tarifasEnvio").get().await()
        if (snapshot.isEmpty) {
            // Fallback estándar si aún no se han cargado tarifas en Firestore
            distritosFallback()
        } else {
            snapshot.documents.map { doc ->
                TarifaEnvio(
                    id = doc.id,
                    distrito = doc.getString("distrito") ?: doc.id,
                    cobertura = doc.getBoolean("cobertura") ?: true,
                    costo = doc.getDouble("costo") ?: 10.0,
                    plazoDias = doc.getLong("plazoDias")?.toInt() ?: 3,
                )
            }.sortedBy { it.distrito }
        }
    }.recover {
        distritosFallback()
    }

    private fun distritosFallback(): List<TarifaEnvio> = listOf(
        TarifaEnvio("miraflores", "Miraflores", true, 8.0, 2),
        TarifaEnvio("san-isidro", "San Isidro", true, 8.0, 2),
        TarifaEnvio("surco", "Santiago de Surco", true, 10.0, 3),
        TarifaEnvio("san-borja", "San Borja", true, 9.0, 2),
        TarifaEnvio("barranco", "Barranco", true, 8.0, 2),
        TarifaEnvio("la-molina", "La Molina", true, 12.0, 3),
        TarifaEnvio("jesus-maria", "Jesús María", true, 9.0, 2),
        TarifaEnvio("magdalena", "Magdalena del Mar", true, 9.0, 2),
        TarifaEnvio("san-miguel", "San Miguel", true, 10.0, 3),
        TarifaEnvio("lince", "Lince", true, 9.0, 2),
        TarifaEnvio("pueblo-libre", "Pueblo Libre", true, 9.0, 2),
        TarifaEnvio("lima-cercado", "Lima Cercado", true, 10.0, 3),
    )

    private companion object {
        const val PING = "ping"
        const val CALCULAR_PEDIDO = "calcularPedido"
    }
}
