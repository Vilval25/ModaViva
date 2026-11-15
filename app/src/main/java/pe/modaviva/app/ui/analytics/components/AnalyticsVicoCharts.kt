package pe.modaviva.app.ui.analytics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import pe.modaviva.app.domain.model.ResumenMensualActividad
import kotlin.math.abs
import kotlin.math.roundToInt

private val LabelsMesKey = ExtraStore.Key<List<String>>()

/**
 * Tres gráficos Vico para el dashboard personal del cliente (HU-25):
 * 1) Barras de compras aprobadas por mes (`S/`).
 * 2) Barras comparativas de prendas probadas vs. compradas.
 * 3) Línea de ahorro mensual en promociones y cupones (`S/`).
 */
@Composable
fun AnalyticsVicoCharts(
    meses: List<ResumenMensualActividad>,
    pedidosAnuladosExcluidos: Int,
    modifier: Modifier = Modifier,
) {
    ProvideVicoTheme(theme = rememberM3VicoTheme()) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PurchasesByMonthChartCard(
                meses = meses,
                pedidosAnuladosExcluidos = pedidosAnuladosExcluidos,
            )
            TryOnVsPurchasedChartCard(meses = meses)
            MonthlySavingsLineChartCard(meses = meses)
        }
    }
}

@Composable
private fun PurchasesByMonthChartCard(
    meses: List<ResumenMensualActividad>,
    pedidosAnuladosExcluidos: Int,
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val etiquetas = remember(meses) { meses.map { it.etiquetaMes } }

    LaunchedEffect(meses) {
        if (meses.isEmpty()) return@LaunchedEffect
        modelProducer.runTransaction {
            columnSeries {
                series(meses.map { it.montoComprado })
            }
            extras { store ->
                store[LabelsMesKey] = etiquetas
            }
        }
    }

    val barColor = MaterialTheme.colorScheme.primary
    val columnProvider = remember(barColor) {
        ColumnCartesianLayer.ColumnProvider.series(
            LineComponent(
                fill = Fill(barColor),
                thickness = 20.dp,
                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
            ),
        )
    }

    val subtitulo = if (pedidosAnuladosExcluidos > 0) {
        "Solo pedidos aprobados (se excluyen $pedidosAnuladosExcluidos anulados)"
    } else {
        "Monto total en S/ de pedidos aprobados"
    }

    ChartCardContainer(
        titulo = "Compras por mes",
        subtitulo = subtitulo,
    ) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(columnProvider = columnProvider),
                startAxis = VerticalAxis.rememberStart(
                    valueFormatter = solesAxisFormatter(),
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = monthAxisFormatter(),
                ),
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
        )
    }
}

@Composable
private fun TryOnVsPurchasedChartCard(
    meses: List<ResumenMensualActividad>,
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val etiquetas = remember(meses) { meses.map { it.etiquetaMes } }

    LaunchedEffect(meses) {
        if (meses.isEmpty()) return@LaunchedEffect
        modelProducer.runTransaction {
            columnSeries {
                series(meses.map { it.prendasProbadas })
                series(meses.map { it.prendasCompradasConPrueba })
            }
            extras { store ->
                store[LabelsMesKey] = etiquetas
            }
        }
    }

    val probadasColor = MaterialTheme.colorScheme.secondary
    val compradasColor = MaterialTheme.colorScheme.tertiary

    val columnProvider = remember(probadasColor, compradasColor) {
        ColumnCartesianLayer.ColumnProvider.series(
            LineComponent(
                fill = Fill(probadasColor),
                thickness = 12.dp,
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
            ),
            LineComponent(
                fill = Fill(compradasColor),
                thickness = 12.dp,
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
            ),
        )
    }

    ChartCardContainer(
        titulo = "Probadas vs. compradas",
        subtitulo = "Comparativa mensual entre pruebas virtuales y compras concretadas",
        headerExtra = {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(color = probadasColor, texto = "Probadas")
                LegendDot(color = compradasColor, texto = "Compradas")
            }
        },
    ) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(columnProvider = columnProvider),
                startAxis = VerticalAxis.rememberStart(
                    valueFormatter = integerAxisFormatter(),
                    itemPlacer = remember { VerticalAxis.ItemPlacer.step(step = { 1.0 }) },
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = monthAxisFormatter(),
                ),
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
        )
    }
}

@Composable
private fun MonthlySavingsLineChartCard(
    meses: List<ResumenMensualActividad>,
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val etiquetas = remember(meses) { meses.map { it.etiquetaMes } }

    LaunchedEffect(meses) {
        if (meses.isEmpty()) return@LaunchedEffect
        modelProducer.runTransaction {
            lineSeries {
                series(meses.map { it.ahorroTotal })
            }
            extras { store ->
                store[LabelsMesKey] = etiquetas
            }
        }
    }

    val lineColor = MaterialTheme.colorScheme.tertiary
    val lineProvider = LineCartesianLayer.LineProvider.series(
        LineCartesianLayer.rememberLine(
            fill = LineCartesianLayer.LineFill.single(Fill(lineColor)),
        ),
    )

    ChartCardContainer(
        titulo = "Ahorro mensual",
        subtitulo = "Descuentos por precio promocional y cupones aplicados (S/)",
    ) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(lineProvider = lineProvider),
                startAxis = VerticalAxis.rememberStart(
                    valueFormatter = solesAxisFormatter(),
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = monthAxisFormatter(),
                ),
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        )
    }
}

@Composable
private fun ChartCardContainer(
    titulo: String,
    subtitulo: String,
    headerExtra: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (headerExtra != null) {
                Spacer(modifier = Modifier.height(10.dp))
                headerExtra()
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun LegendDot(
    color: Color,
    texto: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun monthAxisFormatter(): CartesianValueFormatter =
    CartesianValueFormatter { context, value, _ ->
        val labels = context.model.extraStore.getOrNull(LabelsMesKey).orEmpty()
        val index = value.roundToInt()
        if (abs(value - index) > 0.01) {
            "–"
        } else {
            labels.getOrNull(index)?.takeIf { it.isNotBlank() } ?: "–"
        }
    }

private fun solesAxisFormatter(): CartesianValueFormatter =
    CartesianValueFormatter { _, value, _ ->
        "S/${value.roundToInt()}"
    }

private fun integerAxisFormatter(): CartesianValueFormatter =
    CartesianValueFormatter { _, value, _ ->
        value.roundToInt().toString()
    }
