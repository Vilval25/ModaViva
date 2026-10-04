package pe.modaviva.app.ui.navigation

/**
 * Rutas que no son pestañas. Las pestañas están en [TopLevelDestination] y
 * estas pantallas se abren encima de ellas, sin barra inferior.
 */
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val EMAIL_VERIFY = "email_verify/{email}"
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    const val NOTIFICATIONS = "notifications"
    const val CART = "cart"
    const val PRENDA = "prenda/{codigo}"

    fun prenda(codigo: String) = "prenda/$codigo"
}
