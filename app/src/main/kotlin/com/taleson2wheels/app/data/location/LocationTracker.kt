package com.taleson2wheels.app.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper

/**
 * Thin wrapper over the framework [LocationManager] for live-ride GPS capture —
 * no Google Play Services dependency required. The caller MUST hold a location
 * permission before [start]; this class is permission-agnostic and suppresses
 * the lint check accordingly.
 */
class LocationTracker(context: Context) {

    private val appContext = context.applicationContext
    private val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var listener: LocationListener? = null

    val isTracking: Boolean get() = listener != null

    @SuppressLint("MissingPermission")
    fun start(minTimeMs: Long = 3_000L, minDistanceM: Float = 5f, onLocation: (Location) -> Unit) {
        stop()
        val l = object : LocationListener {
            override fun onLocationChanged(location: Location) = onLocation(location)
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        listener = l
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
            .ifEmpty { listOf(LocationManager.GPS_PROVIDER) }
        providers.forEach { provider ->
            runCatching {
                lm.requestLocationUpdates(provider, minTimeMs, minDistanceM, l, Looper.getMainLooper())
            }
        }
    }

    fun stop() {
        listener?.let { runCatching { lm.removeUpdates(it) } }
        listener = null
    }
}
