package com.haze.app.util

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
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object LocationTracker {
    private const val TAG = "LocationTracker"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _currentAddress = MutableStateFlow<String?>(null)
    val currentAddress: StateFlow<String?> = _currentAddress.asStateFlow()

    private val _currentCoordinates = MutableStateFlow<Pair<Double, Double>?>(null)
    val currentCoordinates: StateFlow<Pair<Double, Double>?> = _currentCoordinates.asStateFlow()

    private val _currentCity = MutableStateFlow<String?>("Rajnandgaon")
    val currentCity: StateFlow<String?> = _currentCity.asStateFlow()

    private val _currentArea = MutableStateFlow<String?>(null)
    val currentArea: StateFlow<String?> = _currentArea.asStateFlow()

    // In-memory reverse geocode cache (~110m precision) to avoid rate limits and save battery
    private val reverseGeocodeCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private var lastNominatimCallTime = 0L

    fun setManualCity(cityName: String) {
        if (cityName.isNotBlank()) {
            _currentCity.value = cityName
            _currentArea.value = null
        }
    }

    fun isGpsEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        val gps = try { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (e: Exception) { false }
        val net = try { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (e: Exception) { false }
        return gps || net
    }

    fun promptEnableGps(context: Context) {
        val activity = context as? android.app.Activity
        if (activity != null) {
            GpsPromptHelper.requestEnableGps(activity)
        } else {
            GpsPromptHelper.openSettingsFallback(context)
        }
    }

    fun updateLocationAndAddress(context: Context, location: Location) {
        _currentCoordinates.value = Pair(location.latitude, location.longitude)
        scope.launch {
            // 1. Try Android Native Geocoder
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val list = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val addr = list?.firstOrNull()
                if (addr != null) {
                    val fullLine = addr.getAddressLine(0)
                    val shortLoc = addr.subLocality ?: addr.locality ?: addr.featureName
                    val detectedCity = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                    val detectedArea = addr.subLocality ?: addr.thoroughfare ?: addr.featureName
                    if (!detectedCity.isNullOrBlank()) {
                        _currentCity.value = detectedCity
                    }
                    if (!detectedArea.isNullOrBlank() && !detectedArea.equals(detectedCity, ignoreCase = true) && !detectedArea.matches(Regex("^[0-9+]+$"))) {
                        _currentArea.value = detectedArea
                    } else {
                        _currentArea.value = null
                    }
                    _currentAddress.value = when {
                        !shortLoc.isNullOrBlank() -> shortLoc
                        !fullLine.isNullOrBlank() -> fullLine.split(",").take(2).joinToString(",").trim()
                        else -> "Current Location"
                    }
                    return@launch
                }
            } catch (_: Exception) {}

            val cacheKey = "${(location.latitude * 1000).toInt()}_${(location.longitude * 1000).toInt()}"
            val cachedAddress = reverseGeocodeCache[cacheKey]
            if (!cachedAddress.isNullOrBlank()) {
                _currentAddress.value = cachedAddress
                return@launch
            }

            // 2. HTTP Fallback: OpenStreetMap Nominatim reverse geocode (rate-limited to 1 call per 2.5s)
            val now = System.currentTimeMillis()
            if (now - lastNominatimCallTime >= 2500L) {
                lastNominatimCallTime = now
                try {
                    val url = URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=${location.latitude}&lon=${location.longitude}&zoom=18&addressdetails=1")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.setRequestProperty("User-Agent", "HazeGoApp/2.0 (contact@hazego.com)")
                    conn.connectTimeout = 3500
                    conn.readTimeout = 3500
                    if (conn.responseCode == 200) {
                        val text = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(text)
                        val name = json.optString("name")
                        val displayName = json.optString("display_name")
                        val parts = displayName.split(",")
                        val addressObj = json.optJSONObject("address")
                        if (addressObj != null) {
                            val cityCandidate = listOf(
                                addressObj.optString("city"),
                                addressObj.optString("town"),
                                addressObj.optString("village"),
                                addressObj.optString("municipality"),
                                addressObj.optString("county"),
                                addressObj.optString("state_district")
                            ).firstOrNull { it.isNotBlank() }
                            if (!cityCandidate.isNullOrBlank()) {
                                _currentCity.value = cityCandidate
                            }
                            val areaCandidate = listOf(
                                addressObj.optString("suburb"),
                                addressObj.optString("neighbourhood"),
                                addressObj.optString("residential"),
                                addressObj.optString("subdistrict"),
                                addressObj.optString("road")
                            ).firstOrNull { it.isNotBlank() }
                            if (!areaCandidate.isNullOrBlank() && !areaCandidate.equals(cityCandidate, ignoreCase = true)) {
                                _currentArea.value = areaCandidate
                            } else {
                                _currentArea.value = null
                            }
                        }
                        val readable = if (name.isNotBlank()) name else parts.take(2).joinToString(",").trim()
                        if (readable.isNotBlank()) {
                            _currentAddress.value = readable
                            if (reverseGeocodeCache.size > 300) reverseGeocodeCache.clear()
                            reverseGeocodeCache[cacheKey] = readable
                            return@launch
                        }
                    }
                    conn.disconnect()
                } catch (_: Exception) {}
            }

            if (_currentAddress.value == null) {
                _currentAddress.value = "Current Location (${String.format(Locale.US, "%.3f, %.3f", location.latitude, location.longitude)})"
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshLocation(context: Context, onLocationFound: ((Location) -> Unit)? = null) {
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return

            // 1. Search all providers for the best cached last known location
            val providers = listOfNotNull(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) LocationManager.FUSED_PROVIDER else null,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.GPS_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )

            var bestLocation: Location? = null
            for (p in providers) {
                try {
                    val loc = lm.getLastKnownLocation(p)
                    if (loc != null) {
                        if (bestLocation == null || loc.time > bestLocation.time) {
                            bestLocation = loc
                        }
                    }
                } catch (_: Exception) {}
            }

            if (bestLocation != null) {
                updateLocationAndAddress(context, bestLocation)
                onLocationFound?.invoke(bestLocation)
            }

            // 2. Request fresh real-time single fix from ALL active providers simultaneously
            val activeProviders = providers.filter { p ->
                try { lm.isProviderEnabled(p) } catch (_: Exception) { false }
            }

            for (p in activeProviders) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val executor = ContextCompat.getMainExecutor(context)
                    try {
                        lm.getCurrentLocation(
                            p,
                            null as android.os.CancellationSignal?,
                            executor,
                            java.util.function.Consumer { loc ->
                                if (loc != null) {
                                    updateLocationAndAddress(context, loc)
                                    onLocationFound?.invoke(loc)
                                }
                            }
                        )
                    } catch (_: Exception) {}
                } else {
                    try {
                        lm.requestSingleUpdate(p, object : LocationListener {
                            override fun onLocationChanged(location: Location) {
                                updateLocationAndAddress(context, location)
                                onLocationFound?.invoke(location)
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
    suspend fun reverseGeocode(context: Context, latitude: Double, longitude: Double): String {
        return withContext(Dispatchers.IO) {
            // 1. Android Geocoder
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val list = geocoder.getFromLocation(latitude, longitude, 1)
                val addr = list?.firstOrNull()
                if (addr != null) {
                    val fullLine = addr.getAddressLine(0)
                    if (!fullLine.isNullOrBlank()) {
                        return@withContext fullLine
                    }
                    val parts = listOfNotNull(
                        addr.featureName,
                        addr.subLocality,
                        addr.locality,
                        addr.subAdminArea,
                        addr.adminArea
                    ).distinct()
                    if (parts.isNotEmpty()) {
                        return@withContext parts.joinToString(", ")
                    }
                }
            } catch (_: Exception) {}

            // 2. OSM Nominatim Fallback
            try {
                val url = URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude&zoom=18&addressdetails=1")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "HazeGoApp/1.0 (contact@hazego.com)")
                conn.connectTimeout = 3500
                conn.readTimeout = 3500
                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(text)
                    val displayName = json.optString("display_name")
                    if (displayName.isNotBlank()) {
                        return@withContext displayName
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {}

            return@withContext "Pin Location (${String.format(Locale.US, "%.4f, %.4f", latitude, longitude)})"
        }
    }
}
