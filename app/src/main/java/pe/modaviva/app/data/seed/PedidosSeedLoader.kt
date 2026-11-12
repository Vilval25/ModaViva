package pe.modaviva.app.data.seed

import android.content.Context
import pe.modaviva.app.domain.model.EstadoPedido
import pe.modaviva.app.domain.model.HitoPedido
import pe.modaviva.app.domain.model.ItemPedido
import pe.modaviva.app.domain.model.Pedido
import pe.modaviva.app.domain.model.Prenda
import pe.modaviva.app.domain.model.TarjetaGuardada
import pe.modaviva.app.domain.model.TipoMedioPago

data class DatosSemillaPedidos(
    val tarjetas: List<TarjetaGuardada>,
    val pedidos: List<Pedido>,
)

/**
 * Cargador de datos semilla desde el archivo JSON `assets/seed/pedidos_semilla.json`.
 * Incluye un parser JSON puro en Kotlin para que funcione tanto en el dispositivo Android
 * como en pruebas unitarias JVM sin depender de mocks de `org.json`.
 */
object PedidosSeedLoader {

    const val RUTA_ASSET_SEMILLA = "seed/pedidos_semilla.json"

    fun cargarDesdeAssets(
        context: Context,
        uidCliente: String,
        nombreCliente: String = "Cliente ModaViva",
        catalogoPorCodigo: Map<String, Prenda> = emptyMap(),
        ahoraMillis: Long = System.currentTimeMillis(),
    ): DatosSemillaPedidos {
        val jsonRaw = context.assets.open(RUTA_ASSET_SEMILLA).bufferedReader().use { it.readText() }
        return parsearJsonSemilla(
            jsonRaw = jsonRaw,
            uidCliente = uidCliente,
            nombreCliente = nombreCliente,
            catalogoPorCodigo = catalogoPorCodigo,
            ahoraMillis = ahoraMillis,
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun parsearJsonSemilla(
        jsonRaw: String,
        uidCliente: String,
        nombreCliente: String = "Cliente ModaViva",
        catalogoPorCodigo: Map<String, Prenda> = emptyMap(),
        ahoraMillis: Long = System.currentTimeMillis(),
    ): DatosSemillaPedidos {
        val root = SimpleJsonParser(jsonRaw).parseValue() as? Map<String, Any?>
            ?: return DatosSemillaPedidos(emptyList(), emptyList())

        val titularFinal = nombreCliente.ifBlank { "CLIENTE MODAVIVA" }.uppercase()

        val rawTarjetas = root["tarjetasSemilla"] as? List<Map<String, Any?>> ?: emptyList()
        val tarjetas = rawTarjetas.map { map ->
            TarjetaGuardada(
                id = map["id"] as? String ?: "",
                marca = map["marca"] as? String ?: "Visa",
                ultimos4 = map["ultimos4"] as? String ?: "0000",
                vencimiento = map["vencimiento"] as? String ?: "12/28",
                titular = titularFinal,
                tipo = map["tipo"] as? String ?: "CREDITO",
                tokenPasarela = map["tokenPasarela"] as? String ?: "",
                esPredeterminada = map["esPredeterminada"] as? Boolean ?: false,
            )
        }

        val rawPedidos = root["pedidosSemilla"] as? List<Map<String, Any?>> ?: emptyList()
        val pedidos = rawPedidos.map { map ->
            val horasDesdeCreacion = (map["horasDesdeCreacion"] as? Number)?.toLong() ?: 0L
            val minutosDesdeCreacion = (map["minutosDesdeCreacion"] as? Number)?.toLong() ?: 0L
            val creadoEn = ahoraMillis - (horasDesdeCreacion * 3_600_000L) - (minutosDesdeCreacion * 60_000L)

            val horasDesdeEntrega = (map["horasDesdeEntrega"] as? Number)?.toLong()
            val entregadoEn = horasDesdeEntrega?.let { ahoraMillis - (it * 3_600_000L) }

            val minutosParaVencer = (map["minutosParaVencer"] as? Number)?.toLong()
            val venceEn = minutosParaVencer?.let { ahoraMillis + (it * 60_000L) }

            val rawItems = map["items"] as? List<Map<String, Any?>> ?: emptyList()
            val items = rawItems.map { itemMap ->
                val codigo = itemMap["codigo"] as? String ?: (itemMap["prendaId"] as? String ?: "")
                val prendaReal = catalogoPorCodigo[codigo]
                val fotoReal = prendaReal?.fotos?.firstOrNull().orEmpty()
                val fotoJson = itemMap["fotoUrl"] as? String ?: ""

                ItemPedido(
                    prendaId = itemMap["prendaId"] as? String ?: codigo,
                    codigo = codigo,
                    nombre = prendaReal?.nombre ?: (itemMap["nombre"] as? String ?: "Prenda $codigo"),
                    fotoUrl = fotoReal.ifBlank { fotoJson },
                    talla = itemMap["talla"] as? String ?: "M",
                    cantidad = (itemMap["cantidad"] as? Number)?.toInt() ?: 1,
                    precioUnitario = (itemMap["precioUnitario"] as? Number)?.toDouble() ?: 0.0,
                    subtotal = (itemMap["subtotal"] as? Number)?.toDouble() ?: 0.0,
                    usoProbadorVirtual = itemMap["usoProbadorVirtual"] as? Boolean ?: false,
                )
            }

            val rawHitos = map["hitos"] as? List<Map<String, Any?>> ?: emptyList()
            val hitos = rawHitos.map { hitoMap ->
                val horasAtras = (hitoMap["horasAtras"] as? Number)?.toLong() ?: 0L
                val fechaHito = if (horasAtras == 0L && minutosDesdeCreacion > 0L) {
                    creadoEn
                } else {
                    ahoraMillis - (horasAtras * 3_600_000L)
                }
                HitoPedido(
                    estado = EstadoPedido.desdeCodigo(hitoMap["estado"] as? String),
                    titulo = hitoMap["titulo"] as? String ?: "",
                    descripcion = hitoMap["descripcion"] as? String ?: "",
                    fechaEpochMillis = fechaHito,
                )
            }

            val ultimoHitoMillis = hitos.maxOfOrNull { it.fechaEpochMillis } ?: creadoEn

            Pedido(
                id = map["id"] as? String ?: "",
                numeroPedido = map["numeroPedido"] as? String ?: "",
                uidCliente = uidCliente,
                estado = EstadoPedido.desdeCodigo(map["estado"] as? String),
                tipoEntrega = map["tipoEntrega"] as? String ?: "delivery",
                direccionEntrega = map["direccionEntrega"] as? String ?: "",
                referenciaEntrega = map["referenciaEntrega"] as? String,
                distrito = map["distrito"] as? String ?: "",
                distritoSlug = map["distritoSlug"] as? String ?: "",
                tiendaRecojoId = map["tiendaRecojoId"] as? String,
                tiendaRecojoNombre = map["tiendaRecojoNombre"] as? String,
                tiendaRecojoDireccion = map["tiendaRecojoDireccion"] as? String,
                tiendaRecojoHorario = map["tiendaRecojoHorario"] as? String,
                destinatario = nombreCliente.ifBlank { map["destinatario"] as? String ?: "Cliente ModaViva" },
                telefonoContacto = map["telefonoContacto"] as? String ?: "987654321",
                medioPago = TipoMedioPago.desdeCodigo(map["medioPago"] as? String),
                resumenMedioPago = map["resumenMedioPago"] as? String ?: "",
                items = items,
                subtotal = (map["subtotal"] as? Number)?.toDouble() ?: 0.0,
                descuentoTotal = (map["descuentoTotal"] as? Number)?.toDouble() ?: 0.0,
                costoEnvio = (map["costoEnvio"] as? Number)?.toDouble() ?: 0.0,
                total = (map["total"] as? Number)?.toDouble() ?: 0.0,
                cuponAplicado = map["cuponAplicado"] as? String,
                creadoEnMillis = creadoEn,
                actualizadoEnMillis = ultimoHitoMillis,
                venceEnMillis = venceEn,
                entregadoEnMillis = entregadoEn,
                empresaTransporte = map["empresaTransporte"] as? String,
                numeroSeguimiento = map["numeroSeguimiento"] as? String,
                listoParaRecoger = map["listoParaRecoger"] as? Boolean ?: false,
                hitos = hitos,
            )
        }

        return DatosSemillaPedidos(
            tarjetas = tarjetas,
            pedidos = pedidos,
        )
    }

    /**
     * Parser JSON ligero en Kotlin puro para lectura sin dependencias externas.
     */
    private class SimpleJsonParser(private val input: String) {
        private var pos = 0

        fun parseValue(): Any? {
            skipWhitespace()
            if (pos >= input.length) return null
            return when (val c = input[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't', 'f' -> parseBoolean()
                'n' -> parseNull()
                else -> if (c == '-' || c.isDigit()) parseNumber() else null
            }
        }

        private fun parseObject(): Map<String, Any?> {
            val map = LinkedHashMap<String, Any?>()
            pos++ // '{'
            skipWhitespace()
            if (pos < input.length && input[pos] == '}') {
                pos++
                return map
            }
            while (pos < input.length) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                if (pos < input.length && input[pos] == ':') pos++
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                if (pos < input.length && input[pos] == ',') {
                    pos++
                } else if (pos < input.length && input[pos] == '}') {
                    pos++
                    break
                } else {
                    break
                }
            }
            return map
        }

        private fun parseArray(): List<Any?> {
            val list = ArrayList<Any?>()
            pos++ // '['
            skipWhitespace()
            if (pos < input.length && input[pos] == ']') {
                pos++
                return list
            }
            while (pos < input.length) {
                list.add(parseValue())
                skipWhitespace()
                if (pos < input.length && input[pos] == ',') {
                    pos++
                } else if (pos < input.length && input[pos] == ']') {
                    pos++
                    break
                } else {
                    break
                }
            }
            return list
        }

        private fun parseString(): String {
            if (input[pos] == '"') pos++
            val sb = StringBuilder()
            while (pos < input.length) {
                val c = input[pos++]
                if (c == '"') break
                if (c == '\\' && pos < input.length) {
                    when (val esc = input[pos++]) {
                        '"', '\\', '/' -> sb.append(esc)
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (pos + 4 <= input.length) {
                                val hex = input.substring(pos, pos + 4)
                                sb.append(hex.toInt(16).toChar())
                                pos += 4
                            }
                        }
                        else -> sb.append(esc)
                    }
                } else {
                    sb.append(c)
                }
            }
            return sb.toString()
        }

        private fun parseBoolean(): Boolean {
            return if (input.startsWith("true", pos)) {
                pos += 4
                true
            } else {
                pos += 5
                false
            }
        }

        private fun parseNull(): Any? {
            pos += 4
            return null
        }

        private fun parseNumber(): Number {
            val start = pos
            while (pos < input.length && (input[pos].isDigit() || input[pos] in "-+.eE")) {
                pos++
            }
            val raw = input.substring(start, pos)
            return if (raw.contains('.') || raw.contains('e', ignoreCase = true)) {
                raw.toDoubleOrNull() ?: 0.0
            } else {
                raw.toLongOrNull() ?: 0L
            }
        }

        private fun skipWhitespace() {
            while (pos < input.length && input[pos].isWhitespace()) {
                pos++
            }
        }
    }
}

