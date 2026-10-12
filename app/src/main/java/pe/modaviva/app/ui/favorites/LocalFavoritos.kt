package pe.modaviva.app.ui.favorites

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf

/**
 * Favoritos de la cuenta y la acción del corazón (HU-08 CA-01). La navegación
 * los provee para que cualquier tarjeta o ficha muestre el corazón sin pasar
 * parámetros por cada pantalla. Si un invitado toca el corazón, [alternar]
 * pide iniciar sesión (CA-08).
 */
@Immutable
data class Favoritos(
    val codigos: Set<String>,
    val alternar: (codigo: String) -> Unit,
) {
    fun contiene(codigo: String) = codigo in codigos
}

/** null: sin corazón (p. ej. en las vistas previas). */
val LocalFavoritos = compositionLocalOf<Favoritos?> { null }
