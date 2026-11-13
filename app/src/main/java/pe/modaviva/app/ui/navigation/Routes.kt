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
    const val CHECKOUT_SUMMARY = "checkout_summary"
    const val CHECKOUT_DELIVERY = "checkout_delivery"
    const val CHECKOUT_PAYMENT = "checkout_payment"
    const val ORDER_CONFIRMED = "order_confirmed/{pedidoId}"
    const val ADDRESSES = "addresses"
    const val PAYMENT_METHODS = "payment_methods"
    const val PRENDA = "prenda/{codigo}"

    fun prenda(codigo: String) = "prenda/$codigo"
    fun orderConfirmed(pedidoId: String) = "order_confirmed/$pedidoId"
}
