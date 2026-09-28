package pe.modaviva.app.data.remote

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Espera el resultado de una Task de Google Play Services sin bloquear el
 * hilo, y propaga la excepción original si la Task falla.
 *
 * Firebase no ofrece variantes suspend de sus APIs, así que esta es la
 * única forma de usarlas desde coroutines sin bloquear.
 */
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        val error = task.exception
        if (error != null) {
            continuation.resumeWithException(error)
        } else {
            continuation.resume(task.result)
        }
    }
    addOnCanceledListener { continuation.cancel() }
}
