package pe.modaviva.app.domain.usecase

import pe.modaviva.app.domain.model.DashboardActividadResumen
import pe.modaviva.app.domain.model.EstadoPedido
import pe.modaviva.app.domain.model.Pedido
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.domain.model.PruebaVirtualRegistro
import pe.modaviva.app.domain.model.ResumenMensualActividad
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/**
 * Agrega compras aprobadas, pruebas virtuales y ahorro acumulado en promociones
 * y cupones dentro de un rango de fechas (HU-25).
 */
class ComputeCustomerDashboardUseCase @Inject constructor() {

    operator fun invoke(
        pedidos: List<Pedido>,
        catalogoPorCodigo: Map<String, Prenda>,
        pruebasVirtuales: List<PruebaVirtualRegistro>,
        desdeMillis: Long?,
        hastaMillis: Long?,
        zoneId: ZoneId = ZoneId.of("America/Lima"),
    ): DashboardActividadResumen {
        val pedidosEnRango = pedidos.filter { it.creadoEnMillis.enRango(desdeMillis, hastaMillis) }
        val pedidosAnulados = pedidosEnRango.count { it.estado == EstadoPedido.ANULADO }
        val pedidosAprobados = pedidosEnRango.filter {
            it.estado != EstadoPedido.ANULADO && it.estado != EstadoPedido.PENDIENTE
        }

        val pruebasFiltradas = pruebasVirtuales.filter {
            it.fechaEpochMillis.enRango(desdeMillis, hastaMillis)
        }

        val codigosComprados = pedidosAprobados
            .flatMap { it.items }
            .map { it.codigo }
            .toSet()

        // Si existen pedidos marcados con usoProbadorVirtual sin un evento en historial,
        // se incorporan como pruebas efectivas para mantener consistencia (CA-02).
        val codigosConRegistro = pruebasFiltradas.map { it.prendaCodigo }.toSet()
        val pruebasDerivadas = pedidosAprobados.flatMap { pedido ->
            pedido.items
                .filter { it.usoProbadorVirtual && it.codigo !in codigosConRegistro }
                .map { item ->
                    PruebaVirtualRegistro(
                        prendaCodigo = item.codigo,
                        prendaNombre = item.nombre,
                        categoria = catalogoPorCodigo[item.codigo]?.categoria.orEmpty(),
                        fechaEpochMillis = pedido.creadoEnMillis,
                        comprada = true,
                    )
                }
        }

        val todasLasPruebas = (pruebasFiltradas + pruebasDerivadas).map { prueba ->
            prueba.copy(comprada = prueba.comprada || prueba.prendaCodigo in codigosComprados)
        }

        if (pedidosAprobados.isEmpty() && todasLasPruebas.isEmpty()) {
            return DashboardActividadResumen(
                pedidosAnuladosExcluidos = pedidosAnulados,
                sinDatosEnRango = true,
            )
        }

        val acumuladoMensual = mutableMapOf<YearMonth, AcumuladorMes>()
        var ahorroPromosTotal = 0.0
        var ahorroCuponesTotal = 0.0
        var gastoTotal = 0.0
        var prendasCompradasTotal = 0

        for (pedido in pedidosAprobados) {
            val mes = Instant.ofEpochMilli(pedido.creadoEnMillis).atZone(zoneId).let {
                YearMonth.of(it.year, it.month)
            }
            val acc = acumuladoMensual.getOrPut(mes) { AcumuladorMes() }

            val ahorroPromoPedido = pedido.items.sumOf { item ->
                val precioOriginal = catalogoPorCodigo[item.codigo]?.precio
                    ?.takeIf { it > item.precioUnitario }
                    ?: item.precioUnitario
                ((precioOriginal - item.precioUnitario).coerceAtLeast(0.0)) * item.cantidad
            }
            val ahorroCuponPedido = pedido.descuentoTotal.coerceAtLeast(0.0)

            ahorroPromosTotal += ahorroPromoPedido
            ahorroCuponesTotal += ahorroCuponPedido
            gastoTotal += pedido.total
            prendasCompradasTotal += pedido.totalPrendas

            acc.montoComprado += pedido.total
            acc.cantidadPedidos += 1
            acc.ahorroTotal += (ahorroPromoPedido + ahorroCuponPedido)
        }

        for (prueba in todasLasPruebas) {
            val mes = Instant.ofEpochMilli(prueba.fechaEpochMillis).atZone(zoneId).let {
                YearMonth.of(it.year, it.month)
            }
            val acc = acumuladoMensual.getOrPut(mes) { AcumuladorMes() }
            acc.prendasProbadas += 1
            if (prueba.comprada) {
                acc.prendasCompradasConPrueba += 1
            }
        }

        val mesesOrdenados = acumuladoMensual.keys.sorted()
        val abarcaVariosAnios = mesesOrdenados.map { it.year }.distinct().size > 1
        val resumenesMensuales = mesesOrdenados.map { ym ->
            val acc = acumuladoMensual.getValue(ym)
            ResumenMensualActividad(
                anioMes = ym,
                etiquetaMes = formatearMes(ym, abarcaVariosAnios),
                montoComprado = acc.montoComprado,
                cantidadPedidos = acc.cantidadPedidos,
                ahorroTotal = acc.ahorroTotal,
                prendasProbadas = acc.prendasProbadas,
                prendasCompradasConPrueba = acc.prendasCompradasConPrueba,
            )
        }

        return DashboardActividadResumen(
            meses = resumenesMensuales,
            totalGastado = gastoTotal,
            totalPedidosAprobados = pedidosAprobados.size,
            totalAhorrado = ahorroPromosTotal + ahorroCuponesTotal,
            ahorroPorPromociones = ahorroPromosTotal,
            ahorroPorCupones = ahorroCuponesTotal,
            totalPrendasProbadas = todasLasPruebas.size,
            totalPrendasCompradas = prendasCompradasTotal,
            totalPrendasCompradasConPrueba = todasLasPruebas.count { it.comprada },
            pedidosAnuladosExcluidos = pedidosAnulados,
            sinDatosEnRango = false,
        )
    }

    private fun Long.enRango(desde: Long?, hasta: Long?): Boolean {
        if (desde != null && this < desde) return false
        if (hasta != null && this > hasta) return false
        return true
    }

    private fun formatearMes(ym: YearMonth, incluirAnio: Boolean): String {
        val nombre = MESES_ES.getOrElse(ym.monthValue - 1) { ym.month.name.take(3) }
        return if (incluirAnio) "$nombre '${ym.year % 100}" else nombre
    }

    private class AcumuladorMes {
        var montoComprado: Double = 0.0
        var cantidadPedidos: Int = 0
        var ahorroTotal: Double = 0.0
        var prendasProbadas: Int = 0
        var prendasCompradasConPrueba: Int = 0
    }

    private companion object {
        val MESES_ES = listOf(
            "Ene", "Feb", "Mar", "Abr", "May", "Jun",
            "Jul", "Ago", "Sep", "Oct", "Nov", "Dic",
        )
    }
}

