package com.fletaba.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.fletaba.core.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/** Ubicación actual del teléfono (sin Play Services) y su dirección aproximada. */
object LocationHelper {
    private const val TAG = "LocationHelper"

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Última ubicación conocida reciente, o una lectura nueva (hasta 12 s). */
    @Suppress("MissingPermission", "DEPRECATION")
    suspend fun current(context: Context): LatLng? {
        if (!hasPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        val recent = providers.mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
            .filter { System.currentTimeMillis() - it.time < 2 * 60_000 }
            .maxByOrNull { it.time }
        if (recent != null) return LatLng(recent.latitude, recent.longitude)
        val provider = providers.firstOrNull { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) } ?: return null
        val fresh = withTimeoutOrNull(12_000) {
            suspendCancellableCoroutine<Location?> { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) { if (cont.isActive) cont.resume(location) }
                    @Deprecated("Deprecated in Java") override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) { if (cont.isActive) cont.resume(null) }
                }
                try {
                    lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo pedir la ubicación: ${e.message}"); if (cont.isActive) cont.resume(null)
                }
                cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
            }
        }
        return fresh?.let { LatLng(it.latitude, it.longitude) }
    }

    /** "Calle 123, Barrio, Ciudad" para un punto, o null si el teléfono no puede resolverlo. */
    @Suppress("DEPRECATION")
    suspend fun describe(context: Context, p: LatLng): String? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !Geocoder.isPresent()) return@withContext null
            val list = Geocoder(context, Locale("es", "AR")).getFromLocation(p.lat, p.lng, 1)
            val a = list?.firstOrNull() ?: return@withContext null
            val street = listOfNotNull(a.thoroughfare, a.subThoroughfare).joinToString(" ").trim()
            val area = listOfNotNull(a.subLocality, a.locality).filter { it.isNotBlank() }.distinct()
            val primary = street.ifBlank { a.featureName ?: "" }
            if (primary.isBlank() && area.isEmpty()) null else buildLabel(primary.ifBlank { area.first() }, area.joinToString(", "))
        } catch (e: Exception) {
            Log.w(TAG, "Geocoder: ${e.message}"); null
        }
    }
}
