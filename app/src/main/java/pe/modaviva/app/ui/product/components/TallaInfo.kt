package pe.modaviva.app.ui.product.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.modaviva.app.domain.model.RecomendacionTalla
import java.util.Locale

/** Tabla de tallas genérica de ModaViva (cm de cuerpo), usada cuando no hay recomendación personal. */
private val TABLA_GENERICA = listOf(
    listOf("XS", "78–82", "60–64", "84–88"),
    listOf("S", "83–87", "65–69", "89–93"),
    listOf("M", "88–92", "70–74", "94–98"),
    listOf("L", "93–98", "75–80", "99–104"),
    listOf("XL", "99–105", "81–87", "105–111"),
)

/** HU-15 CA-05: tabla de tallas genérica (medidas del cuerpo en cm). */
@Composable
fun TablaTallasGenerica(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                "Tabla de tallas genérica (medidas de tu cuerpo en cm)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            FilaTabla(listOf("Talla", "Pecho", "Cintura", "Cadera"), encabezado = true)
            HorizontalDivider()
            TABLA_GENERICA.forEach { FilaTabla(it) }
        }
    }
}

@Composable
private fun FilaTabla(celdas: List<String>, encabezado: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        celdas.forEachIndexed { i, texto ->
            Text(
                text = texto,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (encabezado || i == 0) FontWeight.Bold else FontWeight.Normal,
                textAlign = if (i == 0) TextAlign.Start else TextAlign.Center,
                modifier = Modifier.weight(if (i == 0) 0.8f else 1f),
            )
        }
    }
}

/** HU-15 — Hoja '¿Cómo calculamos tu talla?' con los pasos y tus medidas estimadas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComoCalculamosTallaSheet(
    recomendacion: RecomendacionTalla,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "¿Cómo calculamos tu talla?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Paso(1, "Tu altura", "Usamos la altura que registraste en Perfil > Mis medidas.")
            Paso(
                2,
                "Tus proporciones",
                "En tu foto de cuerpo completo detectamos la posición de hombros y caderas para estimar su " +
                    "ancho en relación con tu altura. Esto se hace en tu celular.",
            )
            Paso(
                3,
                "Tus medidas estimadas",
                "Con la altura y esas proporciones estimamos tu pecho, cintura y cadera.",
            )
            Paso(
                4,
                "Comparación con la prenda",
                "Comparamos tus medidas con la tabla de medidas de esta prenda y elegimos la talla más cercana. " +
                    "Si quedas a menos de 2 cm entre dos tallas, te mostramos ambas.",
            )

            val medidas = when (recomendacion) {
                is RecomendacionTalla.TallaExacta ->
                    Triple(recomendacion.pechoEstimado, recomendacion.cinturaEstimada, recomendacion.caderaEstimada)
                is RecomendacionTalla.EntreDosTallas ->
                    Triple(recomendacion.pechoEstimado, recomendacion.cinturaEstimada, recomendacion.caderaEstimada)
                else -> null
            }
            medidas?.let { (pecho, cintura, cadera) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Tus medidas estimadas",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            "Pecho ${cm(pecho)} · Cintura ${cm(cintura)} · Cadera ${cm(cadera)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Recomendación aproximada, no garantiza el ajuste exacto. El corte y la tela de cada prenda " +
                        "también influyen. Si tu foto cambia, vuelve a registrarla para recalcular.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Paso(numero: Int, titulo: String, texto: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(28.dp)) {
            Text(
                numero.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun cm(valor: Double) = String.format(Locale.US, "%.0f cm", valor)
