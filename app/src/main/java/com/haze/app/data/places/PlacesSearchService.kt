package com.haze.app.data.places

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Service providing real-time nearby location autocomplete.
 * Biases search results based on user's current GPS location coordinates.
 */
object PlacesSearchService {

    private const val TAG = "PlacesSearchService"

    suspend fun searchNearbyPlaces(
        context: Context,
        query: String,
        userLat: Double?,
        userLng: Double?
    ): List<AutocompletePlace> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()

        val results = mutableListOf<AutocompletePlace>()

        // 1. Google Places API (if configured)
        if (PlacesConfig.googlePlacesApiKey.isNotBlank()) {
            try {
                val googlePlaces = fetchGooglePlaces(cleanQuery, userLat, userLng)
                if (googlePlaces.isNotEmpty()) {
                    return@withContext googlePlaces
                }
            } catch (e: Exception) {
                Log.w(TAG, "Google Places API failed, falling back to OSM Photon", e)
            }
        }

        // 2. High-speed Location-Biased Photon API (Real-time OpenStreetMap landmarks, railway stations, bus stands, airports)
        try {
            val photonPlaces = fetchPhotonPlaces(cleanQuery, userLat, userLng)
            results.addAll(photonPlaces)
        } catch (e: Exception) {
            Log.w(TAG, "Photon location search failed", e)
        }

        // 3. Android Native Geocoder Fallback (biased to local bounding box)
        if (results.isEmpty()) {
            try {
                val nativePlaces = fetchNativeGeocoderPlaces(context, cleanQuery, userLat, userLng)
                results.addAll(nativePlaces)
            } catch (e: Exception) {
                Log.w(TAG, "Native Geocoder search failed", e)
            }
        }

        // 4. OpenStreetMap Nominatim Fallback
        if (results.isEmpty()) {
            try {
                val nominatimPlaces = fetchNominatimPlaces(cleanQuery, userLat, userLng)
                results.addAll(nominatimPlaces)
            } catch (e: Exception) {
                Log.w(TAG, "Nominatim search failed", e)
            }
        }

        // 5. Deduplicate and sort by distance
        val deduplicated = results.distinctBy {
            val roundedLat = String.format(Locale.US, "%.3f", it.latitude)
            val roundedLng = String.format(Locale.US, "%.3f", it.longitude)
            "${it.primaryText.lowercase()}-$roundedLat-$roundedLng"
        }

        if (userLat != null && userLng != null) {
            // Sort nearby matching places first
            deduplicated.sortedBy { it.distanceMeters ?: Float.MAX_VALUE }
                .take(PlacesConfig.MAX_SUGGESTIONS)
        } else {
            deduplicated.take(PlacesConfig.MAX_SUGGESTIONS)
        }
    }

    /**
     * Queries Photon Geocoder API with location bias.
     * Photon calculates distance weighting using the passed lat & lon parameters.
     */
    private fun fetchPhotonPlaces(
        query: String,
        userLat: Double?,
        userLng: Double?
    ): List<AutocompletePlace> {
        val places = mutableListOf<AutocompletePlace>()
        val encoded = URLEncoder.encode(query, "UTF-8")
        val urlBuilder = StringBuilder("https://photon.komoot.io/api/?q=$encoded&limit=10&lang=en")

        if (userLat != null && userLng != null) {
            urlBuilder.append("&lat=$userLat&lon=$userLng")
        }

        val url = URL(urlBuilder.toString())
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "HazeTransportationApp/1.0 (contact@hazeapp.com)")
        conn.connectTimeout = 5000
        conn.readTimeout = 5000

        try {
            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(responseText)
                val features = root.optJSONArray("features") ?: return emptyList()

                for (i in 0 until features.length()) {
                    val feature = features.getJSONObject(i)
                    val properties = feature.optJSONObject("properties") ?: continue
                    val geometry = feature.optJSONObject("geometry") ?: continue
                    val coords = geometry.optJSONArray("coordinates") ?: continue

                    val lng = coords.getDouble(0)
                    val lat = coords.getDouble(1)

                    val name = properties.optString("name").takeIf { it.isNotBlank() }
                        ?: properties.optString("street").takeIf { it.isNotBlank() }
                        ?: query

                    val district = properties.optString("district").takeIf { it.isNotBlank() }
                    val locality = properties.optString("locality").takeIf { it.isNotBlank() }
                    val city = properties.optString("city").takeIf { it.isNotBlank() }
                    val state = properties.optString("state").takeIf { it.isNotBlank() }
                    val country = properties.optString("country").takeIf { it.isNotBlank() }

                    val subParts = listOfNotNull(locality, district, city, state)
                        .distinct()
                        .filter { !it.equals(name, ignoreCase = true) }

                    val secondaryText = if (subParts.isNotEmpty()) {
                        subParts.joinToString(", ")
                    } else {
                        listOfNotNull(state, country).joinToString(", ")
                    }

                    val fullAddress = listOfNotNull(name, locality, district, city, state, country)
                        .distinct()
                        .joinToString(", ")

                    val (distMeters, distFormatted) = calculateDistance(userLat, userLng, lat, lng)

                    places.add(
                        AutocompletePlace(
                            primaryText = name,
                            secondaryText = secondaryText,
                            fullAddress = fullAddress,
                            latitude = lat,
                            longitude = lng,
                            distanceMeters = distMeters,
                            distanceFormatted = distFormatted
                        )
                    )
                }
            }
        } finally {
            conn.disconnect()
        }

        return places
    }

    /**
     * Fallback query to Android Native Geocoder with location bounding box.
     */
    private fun fetchNativeGeocoderPlaces(
        context: Context,
        query: String,
        userLat: Double?,
        userLng: Double?
    ): List<AutocompletePlace> {
        val places = mutableListOf<AutocompletePlace>()
        val geocoder = Geocoder(context, Locale.getDefault())

        val addresses = if (userLat != null && userLng != null) {
            // ~50km bounding box around user
            val delta = 0.45
            try {
                geocoder.getFromLocationName(
                    query,
                    6,
                    userLat - delta,
                    userLng - delta,
                    userLat + delta,
                    userLng + delta
                )
            } catch (_: Exception) {
                geocoder.getFromLocationName(query, 6)
            }
        } else {
            geocoder.getFromLocationName(query, 6)
        }

        addresses?.forEach { addr ->
            val title = addr.featureName ?: addr.subLocality ?: addr.locality ?: query
            val fullLine = addr.getAddressLine(0) ?: listOfNotNull(
                addr.subLocality,
                addr.locality,
                addr.adminArea
            ).joinToString(", ")

            val secondary = listOfNotNull(addr.subLocality, addr.locality, addr.adminArea)
                .filter { !it.equals(title, ignoreCase = true) }
                .joinToString(", ")
                .ifBlank { fullLine }

            val (distMeters, distFormatted) = calculateDistance(userLat, userLng, addr.latitude, addr.longitude)

            places.add(
                AutocompletePlace(
                    primaryText = title,
                    secondaryText = secondary,
                    fullAddress = fullLine,
                    latitude = addr.latitude,
                    longitude = addr.longitude,
                    distanceMeters = distMeters,
                    distanceFormatted = distFormatted
                )
            )
        }

        return places
    }

    /**
     * Fallback to OpenStreetMap Nominatim with optional viewbox.
     */
    private fun fetchNominatimPlaces(
        query: String,
        userLat: Double?,
        userLng: Double?
    ): List<AutocompletePlace> {
        val places = mutableListOf<AutocompletePlace>()
        val encoded = URLEncoder.encode(query, "UTF-8")
        val urlBuilder = StringBuilder("https://nominatim.openstreetmap.org/search?format=json&q=$encoded&countrycodes=in&limit=8&addressdetails=1")

        if (userLat != null && userLng != null) {
            val delta = 0.50
            urlBuilder.append("&viewbox=${userLng - delta},${userLat + delta},${userLng + delta},${userLat - delta}&bounded=0")
        }

        val url = URL(urlBuilder.toString())
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "HazeTransportationApp/1.0 (contact@hazeapp.com)")
        conn.connectTimeout = 4000
        conn.readTimeout = 4000

        try {
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(text)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val lat = obj.getDouble("lat")
                    val lon = obj.getDouble("lon")
                    val displayName = obj.getString("display_name")
                    val parts = displayName.split(",")
                    val placeTitle = parts.firstOrNull()?.trim() ?: displayName
                    val placeSub = parts.drop(1).take(3).joinToString(", ").trim()

                    val (distMeters, distFormatted) = calculateDistance(userLat, userLng, lat, lon)

                    places.add(
                        AutocompletePlace(
                            primaryText = placeTitle,
                            secondaryText = placeSub.ifBlank { displayName },
                            fullAddress = displayName,
                            latitude = lat,
                            longitude = lon,
                            distanceMeters = distMeters,
                            distanceFormatted = distFormatted
                        )
                    )
                }
            }
        } finally {
            conn.disconnect()
        }

        return places
    }

    /**
     * Optional Google Places Autocomplete API Integration.
     */
    private fun fetchGooglePlaces(
        query: String,
        userLat: Double?,
        userLng: Double?
    ): List<AutocompletePlace> {
        val places = mutableListOf<AutocompletePlace>()
        val encoded = URLEncoder.encode(query, "UTF-8")
        val urlBuilder = StringBuilder("https://maps.googleapis.com/maps/api/place/autocomplete/json?input=$encoded&key=${PlacesConfig.googlePlacesApiKey}&components=country:in")

        if (userLat != null && userLng != null) {
            urlBuilder.append("&location=$userLat,$userLng&radius=${PlacesConfig.DEFAULT_SEARCH_RADIUS_METERS}&strictbounds=false")
        }

        val url = URL(urlBuilder.toString())
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 4000
        conn.readTimeout = 4000

        try {
            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(responseText)
                val predictions = root.optJSONArray("predictions") ?: return emptyList()

                for (i in 0 until predictions.length()) {
                    val item = predictions.getJSONObject(i)
                    val placeId = item.optString("place_id")
                    val structuredFormat = item.optJSONObject("structured_formatting")
                    val mainText = structuredFormat?.optString("main_text") ?: item.optString("description")
                    val secondaryText = structuredFormat?.optString("secondary_text") ?: ""
                    val description = item.optString("description")

                    // Resolve coordinates from Place Details if placeId is present
                    val coords = if (placeId.isNotBlank()) fetchGooglePlaceCoordinates(placeId) else null
                    val lat = coords?.first ?: (userLat ?: 0.0)
                    val lng = coords?.second ?: (userLng ?: 0.0)

                    val (distMeters, distFormatted) = calculateDistance(userLat, userLng, lat, lng)

                    places.add(
                        AutocompletePlace(
                            primaryText = mainText,
                            secondaryText = secondaryText,
                            fullAddress = description,
                            latitude = lat,
                            longitude = lng,
                            distanceMeters = distMeters,
                            distanceFormatted = distFormatted
                        )
                    )
                }
            }
        } finally {
            conn.disconnect()
        }

        return places
    }

    private fun fetchGooglePlaceCoordinates(placeId: String): Pair<Double, Double>? {
        try {
            val url = URL("https://maps.googleapis.com/maps/api/place/details/json?place_id=$placeId&fields=geometry&key=${PlacesConfig.googlePlacesApiKey}")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(text)
                val location = root.optJSONObject("result")?.optJSONObject("geometry")?.optJSONObject("location")
                if (location != null) {
                    val lat = location.getDouble("lat")
                    val lng = location.getDouble("lng")
                    return Pair(lat, lng)
                }
            }
        } catch (_: Exception) {}
        return null
    }

    /**
     * Calculates distance between user GPS coordinates and target place coordinates.
     */
    fun calculateDistance(
        userLat: Double?,
        userLng: Double?,
        targetLat: Double,
        targetLng: Double
    ): Pair<Float?, String?> {
        if (userLat == null || userLng == null || (userLat == 0.0 && userLng == 0.0)) {
            return Pair(null, null)
        }
        val results = FloatArray(1)
        Location.distanceBetween(userLat, userLng, targetLat, targetLng, results)
        val meters = results[0]

        val formatted = if (meters < 1000f) {
            "${meters.roundToInt()} m"
        } else {
            val km = meters / 1000f
            if (km < 10f) {
                String.format(Locale.US, "%.1f km", km)
            } else {
                "${km.roundToInt()} km"
            }
        }
        return Pair(meters, formatted)
    }
}
