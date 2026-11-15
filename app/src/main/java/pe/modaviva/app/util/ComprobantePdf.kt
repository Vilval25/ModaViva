package pe.modaviva.app.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import pe.modaviva.app.domain.model.Pedido
import pe.modaviva.app.ui.format.formatearSoles
import java.io.File
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * HU-21 CA-08 — Boleta de venta electrónica del pedido en PDF.
 *
 * Se dibuja con [PdfDocument] (sin librerías externas), se guarda en Descargas/ModaViva
 * y se abre con el visor de PDF del celular. Es un comprobante de demostración.
 */
object ComprobantePdf {

    private const val ANCHO = 595 // A4 en puntos
    private const val ALTO = 842
    private const val MARGEN = 40f
    private const val IGV = 0.18

    private val fechaFormato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-PE"))
        .withZone(ZoneId.of("America/Lima"))

    fun nombreArchivo(pedido: Pedido) = "Boleta-${pedido.numeroPedido}.pdf"

    /** Genera el PDF, lo guarda en Descargas y devuelve el Uri para abrirlo. */
    fun generarYGuardar(context: Context, pedido: Pedido): Uri {
        val documento = dibujar(pedido)
        try {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                guardarEnDescargas(context, nombreArchivo(pedido)) { documento.writeTo(it) }
            } else {
                val carpeta = File(context.getExternalFilesDir(null), "Download").apply { mkdirs() }
                val archivo = File(carpeta, nombreArchivo(pedido))
                archivo.outputStream().use { documento.writeTo(it) }
                FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo)
            }
        } finally {
            documento.close()
        }
    }

    /** Intent para abrir el PDF con el visor que tenga el celular. */
    fun intentAbrir(uri: Uri): Intent =
        Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/pdf")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun guardarEnDescargas(context: Context, nombre: String, escribir: (OutputStream) -> Unit): Uri {
        val resolver = context.contentResolver
        val valores = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, nombre)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/ModaViva")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores)
            ?: throw IllegalStateException("No se pudo crear el archivo en Descargas.")
        try {
            resolver.openOutputStream(uri)?.use(escribir)
                ?: throw IllegalStateException("No se pudo escribir el comprobante.")
            valores.clear()
            valores.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, valores, null, null)
            return uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    private fun dibujar(pedido: Pedido): PdfDocument {
        val doc = PdfDocument()
        val pagina = doc.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, 1).create())
        val c = pagina.canvas

        val titulo = texto(20f, bold = true, color = Color.rgb(0x6B, 0x2D, 0x5C))
        val negrita = texto(10f, bold = true)
        val normal = texto(10f)
        val gris = texto(9f, color = Color.DKGRAY)
        val derecha = texto(10f).apply { textAlign = Paint.Align.RIGHT }
        val derechaNegrita = texto(10f, bold = true).apply { textAlign = Paint.Align.RIGHT }
        val linea = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

        val bordeDerecho = ANCHO - MARGEN
        var y = 60f

        // Encabezado: emisor y recuadro del comprobante.
        c.drawText("ModaViva", MARGEN, y, titulo)
        c.drawText("ModaViva S.A.C. — RUC 20601234567", MARGEN, y + 18, gris)
        c.drawText("Av. Larco 345, Miraflores, Lima", MARGEN, y + 31, gris)

        val caja = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1.5f; color = Color.DKGRAY }
        c.drawRect(bordeDerecho - 200, y - 22, bordeDerecho, y + 38, caja)
        val centro = bordeDerecho - 100
        c.drawText("BOLETA DE VENTA", centro, y - 4, negrita.centrado())
        c.drawText("ELECTRÓNICA", centro, y + 10, negrita.centrado())
        c.drawText(serieNumero(pedido), centro, y + 28, texto(11f, bold = true).centrado())

        y += 70
        c.drawLine(MARGEN, y, bordeDerecho, y, linea)
        y += 20

        // Datos del cliente y del pedido.
        val fecha = fechaFormato.format(Instant.ofEpochMilli(pedido.creadoEnMillis))
        fila(c, "Cliente:", pedido.destinatario.ifBlank { "Cliente ModaViva" }, y, negrita, normal); y += 16
        fila(c, "Fecha de emisión:", fecha, y, negrita, normal); y += 16
        fila(c, "Pedido:", pedido.numeroPedido, y, negrita, normal); y += 16
        fila(c, "Medio de pago:", pedido.resumenMedioPago, y, negrita, normal); y += 16
        val entrega = if (pedido.esRecojoEnTienda) {
            "Recojo en ${pedido.tiendaRecojoNombre ?: "tienda"}"
        } else {
            "Envío a ${pedido.direccionEntrega}, ${pedido.distrito}"
        }
        fila(c, "Entrega:", recortar(entrega, normal, bordeDerecho - MARGEN - 110), y, negrita, normal)
        y += 28

        // Tabla de prendas.
        val colTalla = 330f
        val colCant = 380f
        val colPu = 460f
        val fondo = Paint().apply { color = Color.rgb(0xF2, 0xEC, 0xF0) }
        c.drawRect(MARGEN, y - 13, bordeDerecho, y + 6, fondo)
        c.drawText("Descripción", MARGEN + 4, y, negrita)
        c.drawText("Talla", colTalla, y, negrita)
        c.drawText("Cant.", colCant, y, negrita)
        c.drawText("P. unit.", colPu, y, derechaNegrita)
        c.drawText("Importe", bordeDerecho - 4, y, derechaNegrita)
        y += 22

        for (item in pedido.items) {
            c.drawText(recortar(item.nombre, normal, colTalla - MARGEN - 12), MARGEN + 4, y, normal)
            c.drawText(item.talla, colTalla, y, normal)
            c.drawText(item.cantidad.toString(), colCant + 8, y, normal)
            c.drawText(formatearSoles(item.precioUnitario), colPu, y, derecha)
            c.drawText(formatearSoles(item.subtotal), bordeDerecho - 4, y, derecha)
            y += 18
            if (y > ALTO - 200) break // una página alcanza para un pedido normal
        }

        c.drawLine(MARGEN, y, bordeDerecho, y, linea)
        y += 20

        // Totales: los precios incluyen IGV (18 %).
        val gravada = pedido.total / (1 + IGV)
        val etiquetaX = bordeDerecho - 120
        fun total(etiqueta: String, monto: String, bold: Boolean = false) {
            c.drawText(etiqueta, etiquetaX, y, if (bold) derechaNegrita else derecha)
            c.drawText(monto, bordeDerecho - 4, y, if (bold) derechaNegrita else derecha)
            y += 16
        }
        total("Subtotal", formatearSoles(pedido.subtotal))
        if (pedido.descuentoTotal > 0) {
            total("Descuento${pedido.cuponAplicado?.let { " ($it)" } ?: ""}", "- ${formatearSoles(pedido.descuentoTotal)}")
        }
        total("Envío", formatearSoles(pedido.costoEnvio))
        total("Op. gravada", formatearSoles(gravada))
        total("IGV (18 %)", formatearSoles(pedido.total - gravada))
        y += 4
        c.drawLine(etiquetaX - 80, y - 10, bordeDerecho, y - 10, linea)
        total("TOTAL", formatearSoles(pedido.total), bold = true)

        // Pie.
        val pie = texto(8f, color = Color.GRAY).centrado()
        c.drawText("Comprobante de demostración generado en modo prueba (sandbox). No tiene valor tributario.", ANCHO / 2f, ALTO - 50f, pie)
        c.drawText("Gracias por comprar en ModaViva.", ANCHO / 2f, ALTO - 38f, pie)

        doc.finishPage(pagina)
        return doc
    }

    /** B001-000123: serie de boletas + correlativo del pedido. */
    private fun serieNumero(pedido: Pedido): String {
        val digitos = pedido.numeroPedido.filter(Char::isDigit).takeLast(8).ifBlank { "0" }
        return "B001-${digitos.padStart(6, '0')}"
    }

    private fun fila(c: Canvas, etiqueta: String, valor: String, y: Float, pE: Paint, pV: Paint) {
        c.drawText(etiqueta, MARGEN, y, pE)
        c.drawText(valor, MARGEN + 110, y, pV)
    }

    private fun recortar(texto: String, paint: Paint, ancho: Float): String {
        if (paint.measureText(texto) <= ancho) return texto
        val n = paint.breakText(texto, true, ancho - paint.measureText("…"), null)
        return texto.take(n).trimEnd() + "…"
    }

    private fun texto(size: Float, bold: Boolean = false, color: Int = Color.BLACK) = Paint().apply {
        isAntiAlias = true
        textSize = size
        this.color = color
        typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    /** Copia centrada; no modifica el Paint original. */
    private fun Paint.centrado() = Paint(this).apply { textAlign = Paint.Align.CENTER }
}
