package com.haze.app.ui.home

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.haze.app.util.GpsPromptHelper
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.haze.app.ui.map.MapStyles
import com.haze.app.ui.theme.HazeGreenPrimary
import com.haze.app.util.LocationTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/**
 * Model representing a geocoded address suggestion.
 */
data class SearchPlace(
    val title: String,
    val subtitle: String,
    val lat: Double,
    val lng: Double
)

/**
 * LocationPickerScreen:
 * Opens a full-screen interactive OSM map with a fixed center pin and search hero section.
 * - Top hero section features a real-time Search Bar with autocomplete suggestions (Android Geocoder + OSM Nominatim fallback).
 * - Tapping a search suggestion or submitting pans the map directly to that location.
 * - The user drags and zooms the map under the fixed center pin.
 * - Live center coordinates are reverse-geocoded to show the address preview.
 * - My Location GPS button is placed above the confirm location box on the right (Pickup only; hidden on Drop).
 * - Validates that Pickup and Drop are not the same location; shows alert dialog if same.
 */
@Composable
fun LocationPickerScreen(
    title: String,
    initialLat: Double?,
    initialLng: Double?,
    otherLocationAddress: String? = null,
    otherLocationCoords: Pair<Double, Double>? = null,
    onLocationConfirmed: (address: String, lat: Double, lng: Double) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    val startLat = initialLat ?: LocationTracker.currentCoordinates.value?.first ?: 28.6139
    val startLng = initialLng ?: LocationTracker.currentCoordinates.value?.second ?: 77.2090

    var centerLat by remember { mutableStateOf(startLat) }
    var centerLng by remember { mutableStateOf(startLng) }
    var addressPreview by remember { mutableStateOf("Locating...") }
    var isGeocoding by remember { mutableStateOf(false) }


    val gpsResolutionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            LocationTracker.refreshLocation(context) { loc ->
                centerLat = loc.latitude
                centerLng = loc.longitude
                mapViewRef?.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude))
            }
        }
    }
    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<SearchPlace>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }
    var showSameLocationAlert by remember { mutableStateOf(false) }

    // Auto-refresh GPS location on picker open if no valid coordinates
    LaunchedEffect(Unit) {
        if (initialLat == null || initialLat == 0.0 || initialLat == 28.6139) {
            LocationTracker.refreshLocation(context) { loc ->
                centerLat = loc.latitude
                centerLng = loc.longitude
                mapViewRef?.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude))
            }
        }
    }

    // Intercept hardware / system back button or gesture
    BackHandler(enabled = true) {
        when {
            showSameLocationAlert -> showSameLocationAlert = false
            showSuggestions -> showSuggestions = false
            else -> onDismiss()
        }
    }

    // Live Reverse-geocode whenever map center stabilizes
    LaunchedEffect(centerLat, centerLng) {
        isGeocoding = true
        val resolved = withContext(Dispatchers.IO) {
            LocationTracker.reverseGeocode(context, centerLat, centerLng)
        }
        addressPreview = resolved
        isGeocoding = false
    }

    // Debounced search for place suggestions
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.length < 3) {
            suggestions = emptyList()
            showSuggestions = false
            return@LaunchedEffect
        }
        delay(350)
        isSearching = true
        val results = withContext(Dispatchers.IO) {
            fetchAddressSuggestions(context, q)
        }
        suggestions = results
        showSuggestions = results.isNotEmpty()
        isSearching = false
    }

    // Smoothly fly map to selected search place
    fun selectPlace(place: SearchPlace) {
        searchQuery = place.title
        showSuggestions = false
        keyboardController?.hide()
        focusManager.clearFocus()
        centerLat = place.lat
        centerLng = place.lng
        mapViewRef?.controller?.animateTo(GeoPoint(place.lat, place.lng))
        mapViewRef?.controller?.setZoom(17.0)
    }

    DisposableEffect(Unit) {
        onDispose {
            mapViewRef?.onPause()
            mapViewRef?.onDetach()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // 1. Full-screen Interactive OSM Map
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
            factory = { ctx ->
                Configuration.getInstance().userAgentValue = ctx.packageName
                Configuration.getInstance().cacheMapTileCount = 16
                Configuration.getInstance().tileFileSystemCacheMaxBytes = 150L * 1024 * 1024
                Configuration.getInstance().tileFileSystemCacheTrimBytes = 120L * 1024 * 1024
                MapView(ctx).apply {
                    setTileSource(MapStyles.HAZE_CLEAN)
                    MapStyles.applyHazeTone(this)
                    zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    minZoomLevel = 4.0
                    maxZoomLevel = 20.0
                    isHorizontalMapRepetitionEnabled = false
                    isVerticalMapRepetitionEnabled = false
                    setDestroyMode(false)

                    controller.setZoom(16.5)
                    controller.setCenter(GeoPoint(startLat, startLng))

                    // Listen to map scrolling / panning under fixed center pin
                    addMapListener(DelayedMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            val mapCenter = mapCenter
                            centerLat = mapCenter.latitude
                            centerLng = mapCenter.longitude
                            return true
                        }

                        override fun onZoom(event: ZoomEvent?): Boolean {
                            val mapCenter = mapCenter
                            centerLat = mapCenter.latitude
                            centerLng = mapCenter.longitude
                            return true
                        }
                    }, 250))

                    mapViewRef = this
                }
            }
        )

        // 2. Fixed Center Pin (Stays stationary in dead-center while user drags map underneath)
        val isDrop = title.contains("Drop", ignoreCase = true)
        val pinThemeColor = if (isDrop) Color(0xFFEF4444) else Color(0xFF2563EB)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 36.dp), // Offset slightly so the needle tip points to exact dead center
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Floating prompt pill above pin
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A),
                    shadowElevation = 6.dp,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(pinThemeColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isDrop) "Set drop location" else "Set pickup location",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Pin Head & Needle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(pinThemeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
                // Small needle stem / shadow point
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 9.dp)
                        .background(Color.Black.copy(alpha = 0.7f))
                )
                // Small ground shadow dot
                Box(
                    modifier = Modifier
                        .size(width = 14.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                )
            }
        }

        // 3. Top Hero Section (Navigation Bar + Search Input Bar + Live Suggestions Card)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .align(Alignment.TopCenter)
        ) {
            // Row 1: Back Button & Title Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF18181B)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isDrop) Color(0xFFEF4444) else Color(0xFF22C55E))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF18181B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar Input Box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF0F172A),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    if (suggestions.isNotEmpty()) {
                                        selectPlace(suggestions.first())
                                    }
                                }
                            ),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search landmark, bus stand, address...",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF2563EB)
                        )
                    } else if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                suggestions = emptyList()
                                showSuggestions = false
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Suggestions Dropdown Card
            if (showSuggestions && suggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(suggestions) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectPlace(item) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEFF6FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.subtitle,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)
                        }
                    }
                }
            }
        }

        // 4. Bottom Section: Floating My Location FAB (Pickup only) + Floating Confirmation Card
        val themeBorderColor = if (title.contains("Drop", ignoreCase = true)) Color(0xFFEF4444) else Color(0xFF2563EB)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.End
        ) {
            // My Location FAB - ONLY visible for Pickup, placed ABOVE the confirm card on the right
            if (!isDrop) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .size(46.dp)
                ) {
                    IconButton(onClick = {
                        if (!LocationTracker.isGpsEnabled(context) && activity != null) {
                            GpsPromptHelper.requestEnableGps(activity, gpsResolutionLauncher)
                        }
                        val cachedCoords = LocationTracker.currentCoordinates.value
                        if (cachedCoords != null) {
                            mapViewRef?.controller?.animateTo(GeoPoint(cachedCoords.first, cachedCoords.second))
                            centerLat = cachedCoords.first
                            centerLng = cachedCoords.second
                        }
                        Toast.makeText(context, "Locating your position...", Toast.LENGTH_SHORT).show()
                        LocationTracker.refreshLocation(context) { loc ->
                            centerLat = loc.latitude
                            centerLng = loc.longitude
                            mapViewRef?.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude))
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "My Live Location",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Bottom Floating Card with Address Preview & "Confirm Location" Button
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, themeBorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDrop) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                            border = BorderStroke(0.8.dp, if (isDrop) Color(0xFFFCA5A5) else Color(0xFF86EFAC))
                        ) {
                            Text(
                                text = if (isDrop) "DROP LOCATION" else "PICKUP LOCATION",
                                color = if (isDrop) Color(0xFFB91C1C) else Color(0xFF15803D),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = "Drag map to adjust pin",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isGeocoding && addressPreview == "Locating...") "Locating address..." else addressPreview,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (!otherLocationAddress.isNullOrBlank() && isSameLocation(addressPreview, otherLocationAddress, Pair(centerLat, centerLng), otherLocationCoords)) {
                                showSameLocationAlert = true
                            } else {
                                onLocationConfirmed(addressPreview, centerLat, centerLng)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18181B))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = HazeGreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isDrop) "Confirm Drop Location" else "Confirm Pickup Location",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 5. Same Location Alert Dialog
        if (showSameLocationAlert) {
            SameLocationAlertDialog(
                onDismiss = { showSameLocationAlert = false }
            )
        }
    }
}

/**
 * Searches places/landmarks via Android Geocoder with OSM Nominatim fallback for Indian locations.
 */
private fun fetchAddressSuggestions(context: Context, query: String): List<SearchPlace> {
    val results = mutableListOf<SearchPlace>()

    // 1. Android Native Geocoder
    try {
        val geocoder = android.location.Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocationName(query, 5)
        addresses?.forEach { addr ->
            val title = addr.featureName ?: addr.subLocality ?: addr.locality ?: query
            val fullLine = addr.getAddressLine(0) ?: listOfNotNull(addr.subLocality, addr.locality, addr.adminArea).joinToString(", ")
            results.add(
                SearchPlace(
                    title = title,
                    subtitle = fullLine,
                    lat = addr.latitude,
                    lng = addr.longitude
                )
            )
        }
    } catch (_: Exception) {}

    // 2. OpenStreetMap Nominatim Fallback / Augment
    if (results.isEmpty()) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://nominatim.openstreetmap.org/search?format=json&q=$encoded&countrycodes=in&limit=5&addressdetails=1")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "HazeGoApp/1.0 (contact@hazego.com)")
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
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
                    results.add(
                        SearchPlace(
                            title = placeTitle,
                            subtitle = if (placeSub.isNotEmpty()) placeSub else displayName,
                            lat = lat,
                            lng = lon
                        )
                    )
                }
            }
            conn.disconnect()
        } catch (_: Exception) {}
    }

    return results.distinctBy { "${it.lat},${it.lng}" }
}

/**
 * Compares two locations by normalized text and GPS distance to verify distinct Pickup and Drop.
 */
fun isSameLocation(
    addr1: String,
    addr2: String,
    coords1: Pair<Double, Double>?,
    coords2: Pair<Double, Double>?
): Boolean {
    val a1 = addr1.trim()
    val a2 = addr2.trim()
    if (a1.isNotBlank() && a2.isNotBlank()) {
        if (a1.equals(a2, ignoreCase = true)) return true
        val norm1 = a1.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
        val norm2 = a2.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
        if (norm1.isNotEmpty() && norm1 == norm2) return true
    }
    if (coords1 != null && coords2 != null && coords1.first != 0.0 && coords2.first != 0.0) {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            coords1.first, coords1.second,
            coords2.first, coords2.second,
            results
        )
        if (results[0] < 60f) {
            return true
        }
    }
    return false
}

/**
 * Reusable modal alert dialog shown when Pickup and Drop locations are identical.
 */
@Composable
fun SameLocationAlertDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEE2E2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Invalid Location",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Pickup and drop locations cannot be the same.\nPlease select a different destination.",
                    fontSize = 14.sp,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18181B))
                ) {
                    Text(
                        text = "OK, Change Location",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
