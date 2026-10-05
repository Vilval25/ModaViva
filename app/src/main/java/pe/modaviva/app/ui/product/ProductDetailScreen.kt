package pe.modaviva.app.ui.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import java.time.Instant

@Composable
fun ProductDetailScreen(
    codigo: String,
    onBack: () -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel(),
) {
    val prenda by viewModel
        .obtenerPrenda(codigo)
        .collectAsStateWithLifecycle(initialValue = null)

    if (prenda == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val ahora = Instant.now()
        val fotoPrincipal = prenda!!.fotos.firstOrNull()

        if (fotoPrincipal != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(fotoPrincipal)
                    .build(),
                contentDescription = prenda!!.nombre,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentScale = ContentScale.Crop,
            )
        }

        Text(text = prenda!!.nombre)
        Text(text = "Código: ${prenda!!.codigo}")
        Text(text = "Marca: ${prenda!!.marca}")
        Text(text = "Categoría: ${prenda!!.categoria}")
        Text(text = "Subcategoría: ${prenda!!.subcategoria}")
        if (prenda!!.enPromocion(ahora)) {
            Text(text = "Precio: S/ ${prenda!!.precioVigente(ahora)}")
            Text(text = "Precio original: S/ ${prenda!!.precio}")
        } else {
            Text(text = "Precio: S/ ${prenda!!.precio}")
        }
    }
}