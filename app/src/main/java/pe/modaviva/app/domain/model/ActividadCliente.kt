package pe.modaviva.app.domain.model

import java.time.YearMonth

/**
 * Evento de prueba virtual registrado para el cliente (HU-25 CA-02).
 */
data class PruebaVirtualRegistro(
    val prendaCodigo: String,
    val prendaNombre: String,
    val categoria: String = "",
    val fechaEpochMillis: Long,
    val comprada: Boolean = false,
)

/**
 * Presets rápidos de rango temporal para filtrar el dashboard (HU-25 CA-04).
 */
enum class RangoFechasActividad(val etiqueta: String, val mesesAtras: Long?) {
    ULTIMO_MES("Último mes", 1L),
    ULTIMOS_3_MESES("3 meses", 3L),
    ULTIMOS_6_MESES("6 meses", 6L),
    TODO("Todo", null),
    PERSONALIZADO("Personalizado", null),
}

/**
 * Serie mensual consolidada para los gráficos de Vico (HU-25).
 */
data class ResumenMensualActividad(
    val anioMes: YearMonth,
    val etiquetaMes: String,
    val montoComprado: Double,
    val cantidadPedidos: Int,
    val ahorroTotal: Double,
    val prendasProbadas: Int,
    val prendasCompradasConPrueba: Int,
)

/**
 * Métricas agregadas del dashboard personal del cliente para el rango elegido.
 */
data class DashboardActividadResumen(
    val meses: List<ResumenMensualActividad> = emptyList(),
    val totalGastado: Double = 0.0,
    val totalPedidosAprobados: Int = 0,
    val totalAhorrado: Double = 0.0,
    val ahorroPorPromociones: Double = 0.0,
    val ahorroPorCupones: Double = 0.0,
    val totalPrendasProbadas: Int = 0,
    val totalPrendasCompradas: Int = 0,
    val totalPrendasCompradasConPrueba: Int = 0,
    val pedidosAnuladosExcluidos: Int = 0,
    val sinDatosEnRango: Boolean = true,
) {
    val porcentajeConversionProbador: Int
        get() = if (totalPrendasProbadas <= 0) {
            0
        } else {
            ((totalPrendasCompradasConPrueba * 100.0) / totalPrendasProbadas).toInt().coerceIn(0, 100)
        }
}

