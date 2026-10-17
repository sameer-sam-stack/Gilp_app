package com.glip.app.ui.home

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

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
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
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler(enabled = onCancel != null) {
        onCancel?.invoke()
    }

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
            modifier = Modifier.fillMaxSize()
        )

        // 2. Frosted Blur Glass Scrim (Gradient acrylic glass layer over blurred map)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A).copy(alpha = 0.40f),
                            Color(0xFF1E293B).copy(alpha = 0.50f),
                            Color(0xFF0F172A).copy(alpha = 0.65f)
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
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP SECTION: Top Hero Ad Banner (Square shape, moved to the very top)
            AdBannerBox(
                slotId = AdConfig.SLOT_FIRST_SCREEN_TOP_BANNER,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(20.dp)
            )

            // CENTER: Floating Pickup & Drop Card (Matching 2nd image) + Ride Button
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                    // Vertical Route Indicator + Two Address Rows (Matching 2nd image exactly)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Vertical Line with Green Dot at Top and Red Dot at Bottom
                        Column(
                            modifier = Modifier.width(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top Green Dot Box (matching pickup row height 48.dp, centered at 24.dp)
                            Box(
                                modifier = Modifier.height(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .fillMaxHeight()
                                        .padding(top = 24.dp)
                                        .background(Color(0xFFCBD5E1))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                            }

                            // Connecting segment across divider (thickness 1.dp)
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(1.dp)
                                    .background(Color(0xFFCBD5E1))
                            )

                            // Bottom Red Dot Box (matching drop row height 48.dp, centered at 24.dp)
                            Box(
                                modifier = Modifier.height(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .fillMaxHeight()
                                        .padding(bottom = 24.dp)
                                        .background(Color(0xFFCBD5E1))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Right: Pickup Row, Divider, Drop Row
                        Column(modifier = Modifier.weight(1f)) {
                            // Row 1: Pickup
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = pickup,
                                    onValueChange = { query ->
                                        pickup = query
                                        isPickupSelected = false
                                        pickupCoordinates = null
                                        activeField = "pickup"
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused && activeField != "pickup") {
                                                activeField = "pickup"
                                            }
                                        },
                                    textStyle = TextStyle(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF0F172A)
                                    ),
                                    decorationBox = { innerTextField ->
                                        if (pickup.isEmpty()) {
                                            Text(
                                                text = "Enter Pickup Location",
                                                fontSize = 15.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                        innerTextField()
                                    }
                                )

                                // Green Target Icon (Matching Image 2)
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
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = Color(0xFFF1F5F9),
                                thickness = 1.dp
                            )

                            // Row 2: Drop
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = drop,
                                    onValueChange = { query ->
                                        drop = query
                                        isDropSelected = false
                                        dropCoordinates = null
                                        activeField = "drop"
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused && activeField != "drop") {
                                                activeField = "drop"
                                            }
                                        },
                                    textStyle = TextStyle(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF0F172A)
                                    ),
                                    decorationBox = { innerTextField ->
                                        if (drop.isEmpty()) {
                                            Text(
                                                text = "Enter Drop Location",
                                                fontSize = 15.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                        innerTextField()
                                    }
                                )

                                // Red Target Icon (Matching Image 2)
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
                                        modifier = Modifier.size(20.dp)
                                    )
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
                            disabledContainerColor = Color(0xFFCBD5E1),
                            contentColor = Color.White,
                            disabledContentColor = Color.White
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

            // BOTTOM SECTION: Empty Bottom Ad Container Box (Low height 72dp, white card with blue border)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, Color(0xFF2563EB)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                AdBannerBox(
                    slotId = AdConfig.SLOT_FIRST_SCREEN_BOTTOM_STRIP,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(16.dp)
                )
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
