package pe.modaviva.app.ui.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun StockTestScreen(
    viewModel: StockViewModel = hiltViewModel(),
) {
    val stock by viewModel.stock.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.observeStock(
            prendaId = "JN-1004",
            talla = "36",
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Prueba de disponibilidad",
        )

        when {
            stock == null -> {
                CircularProgressIndicator()
            }

            else -> {
                Text(
                    text = if (stock!!.estaAgotado) {
                        "Agotado"
                    } else {
                        "Disponible"
                    }
                )

                StockAvailability(
                    stockByStore = stock!!.porTienda,
                )
            }
        }
    }
}