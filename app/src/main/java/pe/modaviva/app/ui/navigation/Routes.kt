package pe.modaviva.app.ui.navigation

/**
 * Rutas que no son pestañas. Las pestañas están en [TopLevelDestination] y
 * estas pantallas se abren encima de ellas, sin barra inferior.
 */
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val EMAIL_VERIFY = "email_verify/{email}"
    const val FAVORITES = "favorites"
    const val NOTIFICATIONS = "notifications"
    const val CART = "cart"
    const val CHECKOUT_SUMMARY = "checkout_summary"
    const val CHECKOUT_DELIVERY = "checkout_delivery"
    const val CHECKOUT_PAYMENT = "checkout_payment"
    const val ORDER_CONFIRMED = "order_confirmed/{pedidoId}"
    const val ORDER_DETAIL = "order_detail/{pedidoId}"
    const val ADDRESSES = "addresses"
    const val MY_ACTIVITY = "my_activity"
    const val PAYMENT_METHODS = "payment_methods"
    const val PRENDA = "prenda/{codigo}"
    const val CHAT = "chat?prendaCodigo={prendaCodigo}"
    const val CONSTANCIA_FOTO = "constancia_foto/{eliminadaEnMillis}"

    fun prenda(codigo: String) = "prenda/$codigo"
    fun chat(prendaCodigo: String? = null) =
        if (prendaCodigo.isNullOrBlank()) "chat" else "chat?prendaCodigo=$prendaCodigo"
    fun orderConfirmed(pedidoId: String) = "order_confirmed/$pedidoId"
    fun orderDetail(pedidoId: String) = "order_detail/$pedidoId"
    fun constanciaFoto(eliminadaEnMillis: Long) = "constancia_foto/$eliminadaEnMillis"
}
