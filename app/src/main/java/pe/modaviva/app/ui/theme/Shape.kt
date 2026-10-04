package pe.modaviva.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Escala de formas de Material 3. Material Theme Builder no exporta formas,
 * así que se declaran aquí con los valores de la guía para que cada
 * componente las tome del tema (MaterialTheme.shapes) y no fije radios a mano.
 *
 *  - extraSmall: chips pequeños, insignias
 *  - small: chips, menús
 *  - medium: campos de texto, tarjetas
 *  - large: hojas inferiores, diálogos grandes
 *  - extraLarge: diálogos, buscador
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
