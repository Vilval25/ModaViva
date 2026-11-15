package pe.modaviva.app.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import pe.modaviva.app.MainActivity
import pe.modaviva.app.R

/** Canal y notificaciones del sistema para los avisos push (HU-18 CA-05). */
object NotificacionesSistema {

    const val CANAL_AVISOS = "avisos"

    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(
            CANAL_AVISOS,
            "Avisos de ModaViva",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Pruebas virtuales listas, cambios en tus pedidos y solicitudes."
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(canal)
    }

    /** CA-06: en Android 13+ sin permiso no se muestra nada; el aviso igual queda en la bandeja. */
    fun puedeMostrar(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun mostrar(context: Context, aviso: AvisoPush, titulo: String, texto: String) {
        if (!puedeMostrar(context)) return

        val abrirApp = aviso.escribirEn(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        val pendingIntent = PendingIntent.getActivity(
            context,
            aviso.avisoId.hashCode(),
            abrirApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notificacion = NotificationCompat.Builder(context, CANAL_AVISOS)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(aviso.avisoId.hashCode(), notificacion)
        } catch (_: SecurityException) {
            // El permiso se revocó justo ahora: el aviso sigue en la bandeja.
        }
    }
}
