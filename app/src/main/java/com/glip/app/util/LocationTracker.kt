package com.glip.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

object LocationTracker {
    private val _currentAddress = MutableStateFlow<String?>(null)
    val currentAddress: StateFlow<String?> = _currentAddress.asStateFlow()

    private val _currentCoordinates = MutableStateFlow<Pair<Double, Double>?>(null)
    val currentCoordinates: StateFlow<Pair<Double, Double>?> = _currentCoordinates.asStateFlow()

    fun isGpsEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun promptEnableGps(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("LocationTracker", "Error opening location settings", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshLocation(context: Context) {
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            val lastLocation: Location? = when {
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                else -> null
            }

            if (lastLocation != null) {
                _currentCoordinates.value = Pair(lastLocation.latitude, lastLocation.longitude)
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val list = geocoder.getFromLocation(lastLocation.latitude, lastLocation.longitude, 1)
                    val loc = list?.firstOrNull()?.locality ?: list?.firstOrNull()?.subLocality ?: list?.firstOrNull()?.featureName
                    if (!loc.isNullOrBlank()) {
                        _currentAddress.value = loc
                        return
                    }
                } catch (_: Exception) {}
                _currentAddress.value = "Current Location (${String.format(Locale.US, "%.3f, %.3f", lastLocation.latitude, lastLocation.longitude)})"
            }
        } catch (e: Exception) {
            Log.e("LocationTracker", "Error refreshing location", e)
        }
    }
}
