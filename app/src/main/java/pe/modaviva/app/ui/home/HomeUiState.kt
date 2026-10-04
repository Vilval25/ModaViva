package pe.modaviva.app.ui.home

import java.time.Instant

data class HomeUiState(
    val contenido: ContenidoHome = ContenidoHome.CARGANDO,
    val prendas: List<PrendaUi> = emptyList(),
    /** Prendas en promoción y con stock, para la fila de destacados. */
    val destacados: List<PrendaUi> = emptyList(),
    val sinConexion: Boolean = false,
    /** Fecha de la copia local, para el aviso "Sin conexión – datos del…" (HU-04 CA-07). */
    val ultimaActualizacion: Instant? = null,
    val refrescando: Boolean = false,
)

enum class ContenidoHome {
    /** Primera carga, todavía sin datos. */
    CARGANDO,

    /** Hay prendas que mostrar (de la copia local o recién llegadas). */
    CATALOGO,

    /** El servidor respondió y no hay prendas publicadas. */
    VACIO,

    /** Sin conexión y sin copia local. */
    SIN_CONEXION,

    /** El servidor rechazó la consulta y no hay copia local. */
    ERROR,
}

/** Lo que muestra la tarjeta de una prenda (HU-04 CA-06). */
data class PrendaUi(
    val codigo: String,
    val nombre: String,
    val marca: String,
    val foto: String,
    /** Precio vigente ya formateado: el de promoción si corresponde. */
    val precio: String,
    /** Precio regular tachado, solo si hay promoción vigente. */
    val precioOriginal: String?,
    val agotada: Boolean,
)

enum class MensajeHome {
    NO_SE_PUDO_ACTUALIZAR,
}
