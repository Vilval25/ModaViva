package pe.modaviva.app.data.repository

/**
 * Qué toca la eliminación de la foto del probador (HU-17) en Storage y en el
 * documento `clientes/{uid}`. Solo campos del probador: la cuenta, los pedidos
 * y el carrito no se tocan (CA-06).
 */
internal object EliminacionFotoFirestore {

    /** Archivos de `clientes/{uid}/probador/`: el actual (WebP) y el formato anterior. */
    val ARCHIVOS_FOTO = listOf("foto_cuerpo_completo.webp", "foto_cuerpo_completo.jpg")

    /** La foto y las proporciones del cuerpo calculadas con ella, en sus dos ubicaciones. */
    val CAMPOS_BORRADOS = listOf(
        "fotoProbadorUrl",
        "ratioHombro",
        "ratioCadera",
        "probador.fotoUrl",
        "probador.fotoActualizadaEn",
        "probador.ratioHombro",
        "probador.ratioCadera",
    )

    /** Registro de la eliminación (hora del servidor). */
    const val CAMPO_REGISTRO = "probador.fotoEliminadaEn"
}
