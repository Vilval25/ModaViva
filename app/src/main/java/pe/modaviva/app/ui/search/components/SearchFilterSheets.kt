package pe.modaviva.app.ui.search.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Hoja inferior para seleccionar el criterio de ordenamiento de resultados. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSortSheet(
    ordenSeleccionado: String,
    onSelectOrden: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val opciones = listOf(
        "Relevancia",
        "Precio menor a mayor",
        "Precio mayor a menor",
        "Más recientes",
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Ordenar por",
                style = MaterialTheme.typography.titleLarge,
            )

            opciones.forEach { opcion ->
                val seleccionada = ordenSeleccionado == opcion
                if (seleccionada) {
                    Button(
                        onClick = { onSelectOrden(opcion) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(opcion) }
                } else {
                    OutlinedButton(
                        onClick = { onSelectOrden(opcion) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(opcion) }
                }
            }
        }
    }
}

/** Hoja inferior de filtros avanzados (marca, rango de precio, talla, color). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFiltersSheet(
    sheetState: SheetState,
    marcas: List<String>,
    marcaSeleccionada: String?,
    onSelectMarca: (String?) -> Unit,
    precioMinimo: Double,
    precioMaximo: Double,
    rangoPrecio: ClosedFloatingPointRange<Float>,
    onRangoPrecioChange: (ClosedFloatingPointRange<Float>) -> Unit,
    tallas: List<String>,
    tallaSeleccionada: String?,
    onSelectTalla: (String?) -> Unit,
    colores: List<String>,
    colorSeleccionado: String?,
    onSelectColor: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FilterChipsSection(
                titulo = "Marca",
                opcionTodas = "Todas",
                opciones = marcas,
                seleccionada = marcaSeleccionada,
                onSelect = onSelectMarca,
            )

            if (precioMaximo > precioMinimo) {
                Text(
                    text = "Rango de precio",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "S/ ${"%.2f".format(rangoPrecio.start)} - S/ ${"%.2f".format(rangoPrecio.endInclusive)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                RangeSlider(
                    value = rangoPrecio,
                    onValueChange = onRangoPrecioChange,
                    valueRange = precioMinimo.toFloat()..precioMaximo.toFloat(),
                )
            }

            FilterChipsSection(
                titulo = "Talla",
                opcionTodas = "Todas",
                opciones = tallas,
                seleccionada = tallaSeleccionada,
                onSelect = onSelectTalla,
            )

            FilterChipsSection(
                titulo = "Color",
                opcionTodas = "Todos",
                opciones = colores,
                seleccionada = colorSeleccionado,
                onSelect = onSelectColor,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Aplicar filtros")
            }
        }
    }
}

@Composable
private fun FilterChipsSection(
    titulo: String,
    opcionTodas: String,
    opciones: List<String>,
    seleccionada: String?,
    onSelect: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = seleccionada == null,
                onClick = { onSelect(null) },
                label = { Text(opcionTodas) },
            )
            opciones.forEach { opcion ->
                FilterChip(
                    selected = seleccionada == opcion,
                    onClick = { onSelect(if (seleccionada == opcion) null else opcion) },
                    label = { Text(opcion) },
                )
            }
        }
    }
}

