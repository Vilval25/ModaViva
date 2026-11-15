package pe.modaviva.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import pe.modaviva.app.domain.model.DashboardActividadResumen
import pe.modaviva.app.domain.model.RangoFechasActividad
import pe.modaviva.app.domain.repository.AnalyticsRepository
import pe.modaviva.app.domain.repository.CatalogoRepository
import pe.modaviva.app.domain.repository.OrderRepository
import pe.modaviva.app.domain.usecase.ComputeCustomerDashboardUseCase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject

data class CustomerAnalyticsUiState(
    val cargando: Boolean = true,
    val rangoSeleccionado: RangoFechasActividad = RangoFechasActividad.ULTIMOS_6_MESES,
    val fechaDesdeMillis: Long? = null,
    val fechaHastaMillis: Long? = null,
    val resumen: DashboardActividadResumen = DashboardActividadResumen(),
)

private data class FiltroRango(
    val rango: RangoFechasActividad = RangoFechasActividad.ULTIMOS_6_MESES,
    val desdeMillis: Long? = calcularInicioPreset(RangoFechasActividad.ULTIMOS_6_MESES),
    val hastaMillis: Long? = null,
)

private val ZONA_LIMA: ZoneId = ZoneId.of("America/Lima")

private fun calcularInicioPreset(rango: RangoFechasActividad): Long? {
    val meses = rango.mesesAtras ?: return null
    return LocalDate.now(ZONA_LIMA)
        .minusMonths(meses)
        .atStartOfDay(ZONA_LIMA)
        .toInstant()
        .toEpochMilli()
}

@HiltViewModel
class CustomerAnalyticsViewModel @Inject constructor(
    orderRepository: OrderRepository,
    catalogoRepository: CatalogoRepository,
    analyticsRepository: AnalyticsRepository,
    private val computeDashboard: ComputeCustomerDashboardUseCase,
) : ViewModel() {

    private val filtro = MutableStateFlow(FiltroRango())

    val uiState: StateFlow<CustomerAnalyticsUiState> = combine(
        orderRepository.observarPedidos(),
        catalogoRepository.prendas,
        analyticsRepository.observarPruebasVirtuales(),
        filtro,
    ) { pedidos, prendas, pruebas, filtroActual ->
        val catalogoPorCodigo = prendas.associateBy { it.codigo }
        val resumen = computeDashboard(
            pedidos = pedidos,
            catalogoPorCodigo = catalogoPorCodigo,
            pruebasVirtuales = pruebas,
            desdeMillis = filtroActual.desdeMillis,
            hastaMillis = filtroActual.hastaMillis,
            zoneId = ZONA_LIMA,
        )
        CustomerAnalyticsUiState(
            cargando = false,
            rangoSeleccionado = filtroActual.rango,
            fechaDesdeMillis = filtroActual.desdeMillis,
            fechaHastaMillis = filtroActual.hastaMillis,
            resumen = resumen,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CustomerAnalyticsUiState(),
    )

    fun seleccionarRangoPreset(rango: RangoFechasActividad) {
        if (rango == RangoFechasActividad.PERSONALIZADO) return
        filtro.value = FiltroRango(
            rango = rango,
            desdeMillis = calcularInicioPreset(rango),
            hastaMillis = null,
        )
    }

    /**
     * Convierte milisegundos UTC entregados por `DateRangePicker` a límites de día
     * completos en la zona horaria de Lima para no perder compras del último día.
     */
    fun aplicarRangoPersonalizado(utcStartMillis: Long, utcEndMillis: Long) {
        val inicioDia = Instant.ofEpochMilli(utcStartMillis)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
        val finDia = Instant.ofEpochMilli(utcEndMillis)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()

        val desde = inicioDia.atStartOfDay(ZONA_LIMA).toInstant().toEpochMilli()
        val hasta = finDia.plusDays(1).atStartOfDay(ZONA_LIMA).toInstant().toEpochMilli() - 1L

        filtro.value = FiltroRango(
            rango = RangoFechasActividad.PERSONALIZADO,
            desdeMillis = desde,
            hastaMillis = hasta,
        )
    }
}

