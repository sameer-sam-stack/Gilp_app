package com.glip.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

object LocationTracker {
    private const val TAG = "LocationTracker"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _currentAddress = MutableStateFlow<String?>(null)
    val currentAddress: StateFlow<String?> = _currentAddress.asStateFlow()

    private val _currentCoordinates = MutableStateFlow<Pair<Double, Double>?>(null)
    val currentCoordinates: StateFlow<Pair<Double, Double>?> = _currentCoordinates.asStateFlow()

    fun isGpsEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        val gps = try { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (e: Exception) { false }
        val net = try { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (e: Exception) { false }
        return gps || net
    }

    fun promptEnableGps(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening location settings", e)
        }
    }

    private fun updateLocationAndAddress(context: Context, location: Location) {
        _currentCoordinates.value = Pair(location.latitude, location.longitude)
        scope.launch {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val list = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val addr = list?.firstOrNull()
                if (addr != null) {
                    val fullLine = addr.getAddressLine(0)
                    val shortLoc = addr.subLocality ?: addr.locality ?: addr.featureName
                    _currentAddress.value = when {
                        !shortLoc.isNullOrBlank() -> shortLoc
                        !fullLine.isNullOrBlank() -> fullLine.split(",").take(2).joinToString(",").trim()
                        else -> "My Location (${String.format(Locale.US, "%.3f, %.3f", location.latitude, location.longitude)})"
                    }
                    return@launch
                }
            } catch (_: Exception) {}
            if (_currentAddress.value == null) {
                _currentAddress.value = "My Location (${String.format(Locale.US, "%.3f, %.3f", location.latitude, location.longitude)})"
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshLocation(context: Context) {
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return

            // 1. Instant check from last known GPS/Network location
            val lastLocation: Location? = when {
                try { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (_: Exception) { false } ->
                    try { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
                try { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { false } ->
                    try { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }
                else -> null
            }

            if (lastLocation != null) {
                updateLocationAndAddress(context, lastLocation)
            }

            // 2. Request a fresh real-time single fix from GPS/Network provider
            val provider = when {
                try { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (_: Exception) { false } -> LocationManager.GPS_PROVIDER
                try { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { false } -> LocationManager.NETWORK_PROVIDER
                else -> null
            }

            if (provider != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val executor = ContextCompat.getMainExecutor(context)
                    try {
                        lm.getCurrentLocation(provider, null, executor) { loc ->
                            if (loc != null) updateLocationAndAddress(context, loc)
                        }
                    } catch (_: Exception) {}
                } else {
                    try {
                        lm.requestSingleUpdate(provider, object : LocationListener {
                            override fun onLocationChanged(location: Location) {
                                updateLocationAndAddress(context, location)
                            }
                            @Deprecated("Deprecated in Java")
                            override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
                            override fun onProviderEnabled(p: String) {}
                            override fun onProviderDisabled(p: String) {}
                        }, Looper.getMainLooper())
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing location", e)
        }
    }

    /**
     * Reverse-geocodes given latitude and longitude into the most detailed readable address.
     */
    fun reverseGeocode(context: Context, latitude: Double, longitude: Double): String {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val list = geocoder.getFromLocation(latitude, longitude, 1)
            val addr = list?.firstOrNull()
            if (addr != null) {
                val fullLine = addr.getAddressLine(0)
                if (!fullLine.isNullOrBlank()) {
                    return fullLine
                }
                val parts = listOfNotNull(
                    addr.featureName,
                    addr.subLocality,
                    addr.locality,
                    addr.subAdminArea,
                    addr.adminArea
                ).distinct()
                if (parts.isNotEmpty()) {
                    return parts.joinToString(", ")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reverse geocode failed", e)
        }
        return "Pin Location (${String.format(Locale.US, "%.4f, %.4f", latitude, longitude)})"
    }
}
