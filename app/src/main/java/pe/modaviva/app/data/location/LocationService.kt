
package pe.modaviva.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

sealed class ResultadoUbicacion {
    object Cargando : ResultadoUbicacion()
    object PermisoDenegado : ResultadoUbicacion()
    object GpsDesactivado : ResultadoUbicacion()
    object NoDisponible : ResultadoUbicacion()

    data class Exito(
        val lat: Double,
        val lng: Double,
        val distritoDetectado: String?,
        val direccionDetectada: String?,
        val referenciaDetectada: String?,
        val componentesDistrito: List<String> = emptyList(),
    ) : ResultadoUbicacion()
}

@Singleton
class LocationService @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun tienePermisoUbicacion(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    fun esGpsActivado(): Boolean {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return false

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    suspend fun obtenerUbicacionActual(): ResultadoUbicacion =
        withContext(Dispatchers.IO) {
            if (!tienePermisoUbicacion()) {
                return@withContext ResultadoUbicacion.PermisoDenegado
            }

            if (!esGpsActivado()) {
                return@withContext ResultadoUbicacion.GpsDesactivado
            }

            try {
                var location: Location? = runCatching {
                    fusedLocationClient.lastLocation.await()
                }.getOrNull()

                if (location == null) {
                    val cts = CancellationTokenSource()
                    location = runCatching {
                        fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            cts.token,
                        ).await()
                    }.getOrNull()
                }

                if (location == null) {
                    return@withContext ResultadoUbicacion.NoDisponible
                }

                val lat = location.latitude
                val lng = location.longitude
                val datos = detectarDatosDireccion(lat, lng)

                ResultadoUbicacion.Exito(
                    lat = lat,
                    lng = lng,
                    distritoDetectado = datos.referencia,
                    direccionDetectada = datos.direccion,
                    referenciaDetectada = datos.referencia,
                    componentesDistrito = datos.componentesDistrito,
                )
            } catch (_: Exception) {
                ResultadoUbicacion.NoDisponible
            }
        }

    private data class DatosDireccion(
        val direccion: String?,
        val referencia: String?,
        val componentesDistrito: List<String>,
    )

    private fun detectarDatosDireccion(lat: Double, lng: Double): DatosDireccion {
        return try {
            @Suppress("DEPRECATION")
            val geocoder = Geocoder(context, Locale("es", "PE"))

            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)

            val addr = addresses?.firstOrNull()
                ?: return DatosDireccion(null, null, emptyList())

            // Son candidatos, no distritos confirmados.
            // El distrito real se resolverá comparándolos con TarifaEnvio.
            val componentes = listOfNotNull(
                addr.subAdminArea,
                addr.locality,
                addr.subLocality,
                addr.adminArea,
            ).distinct()

            val direccionCalle = listOfNotNull(
                addr.subThoroughfare,
                addr.thoroughfare,
            ).joinToString(" ").ifBlank { null }

            val direccion = direccionCalle
                ?: addr.getAddressLine(0)?.takeIf { it.isNotBlank() }

            DatosDireccion(
                direccion = direccion,
                referencia = addr.subLocality?.takeIf { it.isNotBlank() },
                componentesDistrito = componentes,
            )
        } catch (_: Exception) {
            DatosDireccion(null, null, emptyList())
        }
    }
}
