package pe.modaviva.app.domain.model

/**
 * Resultado del cálculo de recomendación de talla (HU-15).
 */
sealed interface RecomendacionTalla {

    /** Talla única recomendada con alto grado de ajuste (CA-01). */
    data class TallaExacta(
        val talla: String,
        val pechoEstimado: Double,
        val cinturaEstimada: Double,
        val caderaEstimada: Double,
    ) : RecomendacionTalla {
        val descripcion: String get() = "Tu talla: $talla"
    }

    /** Medidas intermedias entre dos tallas consecutivas (CA-03). */
    data class EntreDosTallas(
        val tallaMenor: String,
        val tallaMayor: String,
        val pechoEstimado: Double,
        val cinturaEstimada: Double,
        val caderaEstimada: Double,
    ) : RecomendacionTalla {
        val descripcion: String get() = "Entre $tallaMenor y $tallaMayor"
    }

    /** La foto no permitió detectar puntos anatómicos fiables (CA-05). */
    data object FotoInvalidaParaMedidas : RecomendacionTalla

    /** Falta foto o altura para poder calcular (fallback a tabla genérica). */
    data object SinDatosSuficientes : RecomendacionTalla
}

