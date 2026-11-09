package pe.modaviva.app.domain.model

import java.time.Instant

/**
 * Constancia que recibe el cliente al eliminar su foto del probador (HU-17 CA-03).
 * [eliminadaEn] es el momento en que terminó la eliminación.
 */
data class ConstanciaEliminacionFoto(val eliminadaEn: Instant)

/**
 * El perfil tal como queda tras eliminar la foto (HU-17 CA-05 y CA-06): sin la
 * foto ni las proporciones del cuerpo que se calcularon con ella, de modo que el
 * probador queda deshabilitado hasta registrar otra. Todo lo demás se conserva,
 * incluida la altura, que el cliente ingresó a mano.
 */
fun UserProfile.sinFotoProbador(): UserProfile =
    copy(fotoProbadorUrl = null, ratioHombro = null, ratioCadera = null)
