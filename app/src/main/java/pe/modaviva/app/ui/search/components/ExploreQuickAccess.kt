package pe.modaviva.app.ui.search.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pe.modaviva.app.R

/**
 * Vista inicial de exploración por colecciones y públicos cuando el usuario aún
 * no ingresó texto ni seleccionó un filtro en la pestaña Buscar.
 */
@Composable
fun ExploreQuickAccess(
    generos: List<String>,
    categorias: List<String>,
    onOfertasClick: () -> Unit,
    onGeneroClick: (String) -> Unit,
    onVerTodoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            BannerOferta(onClick = onOfertasClick)
        }

        itemsIndexed(generos) { index, genero ->
            val (fondo, texto) = coloresTarjeta(index)
            TarjetaGenero(
                titulo = genero.paraMostrar(),
                subtitulo = categorias
                    .take(3)
                    .joinToString(", ") { it.paraMostrar() },
                containerColor = fondo,
                contentColor = texto,
                imagenALaIzquierda = index % 2 == 1,
                imagen = imagenGenero(genero),
                onClick = { onGeneroClick(genero) },
            )
        }

        item {
            TextButton(
                onClick = onVerTodoClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Ver todas las prendas")
            }
        }
    }
}

@Composable
private fun BannerOferta(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp)
                .fillMaxWidth(0.6f),
        ) {
            Text(
                text = "Sale",
                style = MaterialTheme.typography.headlineLarge,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "Hasta 50% de descuento en prendas seleccionadas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Image(
            painter = painterResource(R.drawable.banner_sale),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(140.dp)
                .padding(end = 8.dp),
        )
    }
}

@Composable
private fun TarjetaGenero(
    titulo: String,
    subtitulo: String,
    containerColor: Color,
    contentColor: Color,
    imagenALaIzquierda: Boolean,
    @DrawableRes imagen: Int?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(MaterialTheme.shapes.large)
            .background(containerColor)
            .clickable(onClick = onClick),
    ) {
        val ladoImagen = if (imagenALaIzquierda) Alignment.CenterStart else Alignment.CenterEnd

        if (imagen != null) {
            Image(
                painter = painterResource(imagen),
                contentDescription = titulo,
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter,
                modifier = Modifier
                    .align(ladoImagen)
                    .fillMaxHeight()
                    .width(160.dp)
                    .padding(horizontal = 10.dp),
            )
        } else {
            Text(
                text = titulo.take(1).uppercase(),
                style = MaterialTheme.typography.displayLarge,
                color = contentColor.copy(alpha = 0.08f),
                modifier = Modifier
                    .align(ladoImagen)
                    .padding(horizontal = 28.dp),
            )
        }

        Column(
            modifier = Modifier
                .align(if (imagenALaIzquierda) Alignment.CenterEnd else Alignment.CenterStart)
                .fillMaxWidth(0.55f)
                .padding(horizontal = 20.dp),
            horizontalAlignment = if (imagenALaIzquierda) Alignment.End else Alignment.Start,
        ) {
            Text(
                text = titulo.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = contentColor,
            )
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.75f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (imagenALaIzquierda) TextAlign.End else TextAlign.Start,
            )
        }
    }
}

@DrawableRes
private fun imagenGenero(genero: String): Int? = when (genero.lowercase()) {
    "mujer", "mujeres" -> R.drawable.cat_mujer
    "hombre", "hombres" -> R.drawable.cat_hombre
    else -> null
}

@Composable
private fun coloresTarjeta(index: Int): Pair<Color, Color> {
    val colores = MaterialTheme.colorScheme
    return when (index % 3) {
        0 -> colores.secondaryContainer to colores.onSecondaryContainer
        1 -> colores.tertiaryContainer to colores.onTertiaryContainer
        else -> colores.primaryContainer to colores.onPrimaryContainer
    }
}

private fun String.paraMostrar(): String =
    replace("-", " ").replaceFirstChar { it.uppercase() }

