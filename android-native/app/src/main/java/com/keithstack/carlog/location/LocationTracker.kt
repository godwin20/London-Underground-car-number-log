package com.keithstack.carlog.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LocationFix(val lat: Double, val lon: Double, val accuracyMeters: Int, val atMillis: Long)

sealed class LocationStatus {
    data object Idle : LocationStatus()
    data object Locating : LocationStatus()
    data class Available(val fix: LocationFix) : LocationStatus()
    data class Error(val message: String) : LocationStatus()
}

/** Wraps the plain Android LocationManager — no Play Services dependency. */
class LocationTracker(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _status = MutableStateFlow<LocationStatus>(LocationStatus.Idle)
    val status: StateFlow<LocationStatus> = _status.asStateFlow()

    private val listener = LocationListener { location -> onLocation(location) }

    private fun onLocation(location: Location) {
        _status.value = LocationStatus.Available(
            LocationFix(location.latitude, location.longitude, location.accuracy.toInt(), location.time)
        )
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun enabledProviders(): List<String> = listOfNotNull(
        LocationManager.GPS_PROVIDER.takeIf { runCatching { locationManager.isProviderEnabled(it) }.getOrDefault(false) },
        LocationManager.NETWORK_PROVIDER.takeIf { runCatching { locationManager.isProviderEnabled(it) }.getOrDefault(false) },
    )

    @SuppressLint("MissingPermission")
    fun start() {
        if (!hasPermission()) {
            _status.value = LocationStatus.Error("Location permission denied")
            return
        }
        val providers = enabledProviders()
        if (providers.isEmpty()) {
            _status.value = LocationStatus.Error("Location not available on this device")
            return
        }
        _status.value = LocationStatus.Locating
        providers.mapNotNull { runCatching { locationManager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.let(::onLocation)
        providers.forEach { provider ->
            runCatching { locationManager.requestLocationUpdates(provider, 15_000L, 20f, listener, Looper.getMainLooper()) }
        }
    }

    @SuppressLint("MissingPermission")
    fun refresh() {
        if (!hasPermission()) {
            _status.value = LocationStatus.Error("Location permission denied")
            return
        }
        val provider = enabledProviders().firstOrNull()
        if (provider == null) {
            _status.value = LocationStatus.Error("Location not available on this device")
            return
        }
        _status.value = LocationStatus.Locating
        runCatching { locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper()) }
    }

    fun stop() {
        runCatching { locationManager.removeUpdates(listener) }
    }
}
