package pe.modaviva.app.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.modaviva.app.R
import pe.modaviva.app.ui.analytics.components.AnalyticsFilterSection
import pe.modaviva.app.ui.analytics.components.AnalyticsKpiGrid
import pe.modaviva.app.ui.analytics.components.AnalyticsVicoCharts
import pe.modaviva.app.ui.components.MvEmptyState
import pe.modaviva.app.ui.components.MvScreen

/**
 * Pantalla independiente Perfil > Mi actividad (HU-25).
 */
@Composable
fun CustomerAnalyticsScreen(
    onBack: () -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomerAnalyticsViewModel = hiltViewModel(),
) {
    MvScreen(
        title = stringResource(R.string.analytics_title),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        AnalyticsDashboardContent(
            onExploreClick = onExploreClick,
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

/**
 * Contenido reutilizable del dashboard personal: usado tanto desde
 * `Perfil > Mi actividad` como desde la vista resumen en `Pedidos`.
 */
@Composable
fun AnalyticsDashboardContent(
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomerAnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        state.cargando -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "filtros") {
                    AnalyticsFilterSection(
                        rangoSeleccionado = state.rangoSeleccionado,
                        fechaDesdeMillis = state.fechaDesdeMillis,
                        fechaHastaMillis = state.fechaHastaMillis,
                        onSelectPreset = viewModel::seleccionarRangoPreset,
                        onApplyCustomRange = viewModel::aplicarRangoPersonalizado,
                    )
                }

                if (state.resumen.sinDatosEnRango) {
                    item(key = "vacio") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            MvEmptyState(
                                icon = Icons.Outlined.Insights,
                                title = stringResource(R.string.analytics_empty_title),
                                message = stringResource(R.string.analytics_empty_body),
                            ) {
                                Button(
                                    onClick = onExploreClick,
                                    modifier = Modifier.fillMaxWidth(0.75f),
                                ) {
                                    Text("Explorar catálogo")
                                }
                            }
                        }
                    }
                } else {
                    item(key = "kpis") {
                        AnalyticsKpiGrid(resumen = state.resumen)
                    }

                    item(key = "graficos") {
                        AnalyticsVicoCharts(
                            meses = state.resumen.meses,
                            pedidosAnuladosExcluidos = state.resumen.pedidosAnuladosExcluidos,
                        )
                    }
                }
            }
        }
    }
}

