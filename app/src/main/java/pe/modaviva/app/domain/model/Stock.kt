package pe.modaviva.app.domain.model

data class Stock(
    val prendaId: String = "",
    val talla: String = "",
    val porTienda: Map<String, Int> = emptyMap(),
    val total: Int = 0,
) {
    val estaAgotado: Boolean
        get() = total <= 0
}