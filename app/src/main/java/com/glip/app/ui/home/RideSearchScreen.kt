package com.glip.app.ui.home

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.app.ui.theme.GlipGreenPrimary
import com.glip.app.util.LocationTracker
import com.glip.app.ads.AdBannerBox
import com.glip.app.ads.AdConfig

import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.zIndex
import com.glip.app.data.places.AutocompletePlace
import com.glip.app.data.places.PlacesConfig
import com.glip.app.data.places.PlacesSearchService
import kotlinx.coroutines.delay

/**
 * RideSearchScreen:
 * Landing screen when app opens.
 * 1. Top: Top Ad Hero Banner (Enlarged)
 * 2. Center: Pickup and Drop address inputs (with real-time autocomplete + map picker) + "Ride ->" button
 * 3. Bottom: Empty Ad Container Box (72dp) placed at the very bottom
 * 4. Background: Real OSM street map (Non-interactive, touch blocked)
 * 5. GPS auto-detects user location and pre-fills into Pickup box
 * 6. Map picker modal with fixed center pin for exact location selection
 */
@Composable
fun RideSearchScreen(
    initialPickup: String = "",
    initialDrop: String = "",
    initialPickupLat: Double = 0.0,
    initialPickupLng: Double = 0.0,
    initialDropLat: Double = 0.0,
    initialDropLng: Double = 0.0,
    onConfirmRideSearch: (
        pickup: String,
        drop: String,
        pickupLat: Double,
        pickupLng: Double,
        dropLat: Double,
        dropLng: Double
    ) -> Unit,
    onNavigateToServices: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val liveAddress by LocationTracker.currentAddress.collectAsState()
    val liveCoords by LocationTracker.currentCoordinates.collectAsState()

    var pickup by remember { mutableStateOf(initialPickup) }
    var drop by remember { mutableStateOf(initialDrop) }

    // Coordinates state - initialized from passed coordinates if available
    var pickupCoordinates by remember {
        mutableStateOf<Pair<Double, Double>?>(
            if (initialPickupLat != 0.0 && initialPickupLng != 0.0) Pair(initialPickupLat, initialPickupLng) else null
        )
    }
    var dropCoordinates by remember {
        mutableStateOf<Pair<Double, Double>?>(
            if (initialDropLat != 0.0 && initialDropLng != 0.0) Pair(initialDropLat, initialDropLng) else null
        )
    }

    // Selection validity tracking
    var isPickupSelected by remember { mutableStateOf(initialPickup.isNotBlank() || (initialPickupLat != 0.0 && initialPickupLng != 0.0)) }
    var isDropSelected by remember { mutableStateOf(initialDrop.isNotBlank() || (initialDropLat != 0.0 && initialDropLng != 0.0)) }

    // Autocomplete active field: null, "pickup", or "drop"
    var activeField by remember { mutableStateOf<String?>(null) }
    var suggestions by remember { mutableStateOf<List<AutocompletePlace>>(emptyList()) }
    var isSearchingPlaces by remember { mutableStateOf(false) }

    // Map selection modal state: null = closed, "pickup" = selecting pickup, "drop" = selecting drop
    var selectingLocationMode by remember { mutableStateOf<String?>(null) }
    var showSameLocationDialog by remember { mutableStateOf(false) }

    // Auto-fill pickup with live GPS location as soon as detected
    LaunchedEffect(liveAddress, liveCoords) {
        if (!liveAddress.isNullOrBlank() && pickup.isBlank()) {
            pickup = liveAddress ?: ""
            pickupCoordinates = liveCoords
            isPickupSelected = true
        }
    }

    // Debounced real-time place search based on user's current GPS coordinates
    val currentQuery = when (activeField) {
        "pickup" -> pickup
        "drop" -> drop
        else -> ""
    }

    LaunchedEffect(currentQuery, activeField, liveCoords) {
        if (activeField == null || currentQuery.trim().length < 2) {
            suggestions = emptyList()
            isSearchingPlaces = false
            return@LaunchedEffect
        }

        isSearchingPlaces = true
        delay(PlacesConfig.DEBOUNCE_MILLIS)

        try {
            val results = PlacesSearchService.searchNearbyPlaces(
                context = context,
                query = currentQuery,
                userLat = liveCoords?.first,
                userLng = liveCoords?.second
            )
            suggestions = results
        } catch (e: Exception) {
            suggestions = emptyList()
        } finally {
            isSearchingPlaces = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // 1. Real OpenStreetMap background (Hardware Blur + Diffused soft tone)
        InteractiveDemoMap(
            vehicles = emptyList(),
            selectedVehicle = null,
            onVehicleSelect = {},
            onRecenter = {},
            isInteractive = false,
            isBlurred = true,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(22.dp)
                    } else {
                        Modifier
                    }
                )
        )

        // 2. Frosted Blur Glass Scrim (Gradient acrylic glass layer over blurred map)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A).copy(alpha = 0.35f),
                            Color(0xFF1E293B).copy(alpha = 0.45f),
                            Color(0xFF0F172A).copy(alpha = 0.60f)
                        )
                    )
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    // Strictly consumes touch events so user cannot touch the background map. Does NOT navigate!
                    activeField = null
                    suggestions = emptyList()
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
        )

        // Main UI Column: SpaceBetween layout puts Top at the top, Center in center, and Bottom Ad at the very bottom
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP SECTION: Top Hero Ad Banner (Enlarged downwards per user requirement)
            AdBannerBox(
                slotId = AdConfig.SLOT_FIRST_SCREEN_TOP_BANNER,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .height(285.dp),
                shape = RoundedCornerShape(18.dp)
            )

            // CENTER: Floating Pickup & Drop Card + Ride Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {

                    // SCREENSHOT 1 PICKUP & DROP LOCATION SELECTOR
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Timeline Indicator: Green dot -> Vertical Line -> Red dot
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Box(
                                    modifier = Modifier
                                        .width(1.5.dp)
                                        .height(36.dp)
                                        .background(Color(0xFFCBD5E1))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Right Inputs: Pickup row -> Divider -> Drop row
                            Column(modifier = Modifier.weight(1f)) {
                                // 1. Top: Pickup Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        BasicTextField(
                                            value = pickup,
                                            onValueChange = {
                                                pickup = it
                                                isPickupSelected = false
                                                pickupCoordinates = null
                                                activeField = "pickup"
                                            },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF0F172A)
                                            ),
                                            decorationBox = { innerTextField ->
                                                if (pickup.isEmpty()) {
                                                    Text(
                                                        text = "Enter Pickup Location",
                                                        fontSize = 14.5.sp,
                                                        fontWeight = FontWeight.Normal,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                }
                                                innerTextField()
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onFocusChanged { focusState ->
                                                    if (focusState.isFocused && activeField != "pickup") {
                                                        activeField = "pickup"
                                                    }
                                                }
                                        )
                                    }

                                    if (pickup.isNotEmpty()) {
                                        IconButton(
                                            onClick = {
                                                pickup = ""
                                                pickupCoordinates = null
                                                isPickupSelected = false
                                                if (activeField == "pickup") suggestions = emptyList()
                                            },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }

                                    // Green Target / MyLocation Icon (Clicking opens Map for pickup)
                                    IconButton(
                                        onClick = {
                                            activeField = null
                                            selectingLocationMode = "pickup"
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MyLocation,
                                            contentDescription = "Pick Pickup on Map",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    color = Color(0xFFF1F5F9),
                                    thickness = 1.dp,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // 2. Bottom: Drop Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        BasicTextField(
                                            value = drop,
                                            onValueChange = {
                                                drop = it
                                                isDropSelected = false
                                                dropCoordinates = null
                                                activeField = "drop"
                                            },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                fontSize = 14.5.sp,
                                                fontWeight = if (drop.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                                                color = Color(0xFF0F172A)
                                            ),
                                            decorationBox = { innerTextField ->
                                                if (drop.isEmpty()) {
                                                    Text(
                                                        text = "Enter Drop Location",
                                                        fontSize = 14.5.sp,
                                                        fontWeight = FontWeight.Normal,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                }
                                                innerTextField()
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onFocusChanged { focusState ->
                                                    if (focusState.isFocused && activeField != "drop") {
                                                        activeField = "drop"
                                                    }
                                                }
                                        )
                                    }

                                    if (drop.isNotEmpty()) {
                                        IconButton(
                                            onClick = {
                                                drop = ""
                                                dropCoordinates = null
                                                isDropSelected = false
                                                if (activeField == "drop") suggestions = emptyList()
                                            },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }

                                    // Red Target / MyLocation Icon (Clicking opens Map for drop)
                                    IconButton(
                                        onClick = {
                                            activeField = null
                                            selectingLocationMode = "drop"
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MyLocation,
                                            contentDescription = "Pick Drop on Map",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Real-time Autocomplete Suggestions Dropdown
                    if (activeField != null && (currentQuery.trim().length >= 2 || isSearchingPlaces || suggestions.isNotEmpty())) {
                        Spacer(modifier = Modifier.height(10.dp))
                        PlaceAutocompleteDropdown(
                            suggestions = suggestions,
                            isLoading = isSearchingPlaces,
                            searchQuery = currentQuery,
                            activeField = activeField!!,
                            onPlaceSelected = { place ->
                                if (activeField == "pickup") {
                                    pickup = place.primaryText
                                    pickupCoordinates = Pair(place.latitude, place.longitude)
                                    isPickupSelected = true
                                } else {
                                    drop = place.primaryText
                                    dropCoordinates = Pair(place.latitude, place.longitude)
                                    isDropSelected = true
                                }
                                activeField = null
                                suggestions = emptyList()
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val isRideEnabled = isPickupSelected && isDropSelected && pickup.isNotBlank() && drop.isNotBlank()

                    // RIDE BUTTON (Clickable ONLY when both valid pickup and drop locations are selected)
                    Button(
                        onClick = {
                            if (isRideEnabled) {
                                activeField = null
                                suggestions = emptyList()
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                if (isSameLocation(pickup, drop, pickupCoordinates, dropCoordinates)) {
                                    showSameLocationDialog = true
                                } else {
                                    val pLat = pickupCoordinates?.first ?: if (initialPickupLat != 0.0) initialPickupLat else (liveCoords?.first ?: 28.6139)
                                    val pLng = pickupCoordinates?.second ?: if (initialPickupLng != 0.0) initialPickupLng else (liveCoords?.second ?: 77.2090)
                                    val dLat = dropCoordinates?.first ?: if (initialDropLat != 0.0) initialDropLat else 28.5355
                                    val dLng = dropCoordinates?.second ?: if (initialDropLng != 0.0) initialDropLng else 77.3910
                                    onConfirmRideSearch(pickup.trim(), drop.trim(), pLat, pLng, dLat, dLng)
                                }
                            }
                        },
                        enabled = isRideEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF18181B),
                            disabledContainerColor = Color(0xFF94A3B8).copy(alpha = 0.5f),
                            contentColor = Color.White,
                            disabledContentColor = Color.White.copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Ride",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRideEnabled) Color.White else Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = if (isRideEnabled) GlipGreenPrimary else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                }
            }

            // BOTTOM SECTION: "Our Other Services" Navigation Card (Clickable container navigating to All Services)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(64.dp)
                    .clickable { onNavigateToServices?.invoke() },
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, Color(0xFF2563EB)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Our Other Services",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Parcel • School Van • Personal Ride",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Open All Services",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Location Picker Overlay (Opens map selection screen with fixed center pin)
        if (selectingLocationMode != null) {
            val isPickup = selectingLocationMode == "pickup"
            val title = if (isPickup) "Select Pickup on Map" else "Select Drop on Map"
            val currentLat = if (isPickup) pickupCoordinates?.first else dropCoordinates?.first
            val currentLng = if (isPickup) pickupCoordinates?.second else dropCoordinates?.second
            val otherAddress = if (isPickup) drop else pickup
            val otherCoords = if (isPickup) dropCoordinates else pickupCoordinates

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(100f)
            ) {
                LocationPickerScreen(
                    title = title,
                    initialLat = currentLat,
                    initialLng = currentLng,
                    otherLocationAddress = otherAddress,
                    otherLocationCoords = otherCoords,
                    onLocationConfirmed = { detailedAddress, lat, lng ->
                        if (isPickup) {
                            pickup = detailedAddress
                            pickupCoordinates = Pair(lat, lng)
                            isPickupSelected = true
                        } else {
                            drop = detailedAddress
                            dropCoordinates = Pair(lat, lng)
                            isDropSelected = true
                        }
                        activeField = null
                        suggestions = emptyList()
                        selectingLocationMode = null
                    },
                    onDismiss = {
                        selectingLocationMode = null
                    }
                )
            }
        }

        // Same Location Alert Dialog
        if (showSameLocationDialog) {
            SameLocationAlertDialog(
                onDismiss = { showSameLocationDialog = false }
            )
        }
    }
}
