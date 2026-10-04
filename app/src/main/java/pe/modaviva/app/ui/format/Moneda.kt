package pe.modaviva.app.ui.format

import java.util.Locale

/** "S/ 129.90": formato de precios en soles usado en toda la app. */
fun formatearSoles(monto: Double): String = String.format(Locale.US, "S/ %.2f", monto)
