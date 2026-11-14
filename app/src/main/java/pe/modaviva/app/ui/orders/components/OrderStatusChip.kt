package pe.modaviva.app.ui.orders.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.modaviva.app.domain.model.EstadoPedido

/** Insignia visual con el estado actual de un pedido. */
@Composable
fun ChipEstadoPedido(estado: EstadoPedido) {
    val (fondo, texto) = when (estado) {
        EstadoPedido.PENDIENTE -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) to MaterialTheme.colorScheme.error
        EstadoPedido.APROBADO -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        EstadoPedido.EN_PREPARACION -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        EstadoPedido.DESPACHADO -> Color(0xFFE0F2FE) to Color(0xFF0369A1)
        EstadoPedido.ENTREGADO -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        EstadoPedido.ANULADO -> Color(0xFFF3F4F6) to Color(0xFF6B7280)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = fondo,
    ) {
        Text(
            text = estado.etiqueta,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = texto,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

