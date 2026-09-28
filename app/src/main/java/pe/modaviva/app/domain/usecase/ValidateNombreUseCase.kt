package pe.modaviva.app.domain.usecase

import javax.inject.Inject

/**
 * Nombres y apellidos: solo letras (con acentes y ñ), espacios, apostrófos y
 * guiones para nombres compuestos.
 *
 * El caso vacío devuelve null a propósito porque el mensaje de vacío es distinto
 * para nombres y para apellidos, y se maneja en el ViewModel.
 */
class ValidateNombreUseCase @Inject constructor() {

    private companion object {
        val NOMBRE_REGEX = Regex(
            "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?:[ '’\\-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$"
        )
    }

    operator fun invoke(nombre: String): String? {
        val value = nombre.trim()
        if (value.isEmpty()) return null
        if (!NOMBRE_REGEX.matches(value)) return "Solo letras, espacios y guiones"
        return null
    }

    fun isValid(nombre: String): Boolean = invoke(nombre) == null

}
