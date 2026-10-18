package com.glip.app.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.app.domain.model.Vehicle
import com.glip.app.domain.model.VehicleCategory
import com.glip.app.ui.theme.GlipGreenDark
import com.glip.app.ui.theme.GlipGreenPrimary
import com.glip.app.ui.theme.GlipOrangeAccent
import com.glip.app.ads.AdBannerBox
import com.glip.app.ads.AdConfig
import com.glip.app.ui.components.RouteLoader
import com.glip.app.util.FareCalculationEngine
import com.glip.app.util.PersonalFareEstimate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import com.glip.app.data.places.AutocompletePlace
import com.glip.app.data.places.PlacesConfig
import com.glip.app.data.places.PlacesSearchService
import com.glip.app.util.LocationTracker
import com.glip.app.util.PersonalVehicleType
import kotlinx.coroutines.delay
import java.util.Locale
import org.osmdroid.util.GeoPoint

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    pickupLocation: String = "",
    dropLocation: String = "",
    pickupLat: Double = 0.0,
    pickupLng: Double = 0.0,
    dropLat: Double = 0.0,
    dropLng: Double = 0.0,
    hasConfirmedRoute: Boolean = false,
    userName: String = "",
    userPhone: String = "",
    isPersonalSearchFromServices: Boolean = false,
    onConfirmRideSearch: (
        pickup: String,
        drop: String,
        pickupLat: Double,
        pickupLng: Double,
        dropLat: Double,
        dropLng: Double
    ) -> Unit = { _, _, _, _, _, _ -> },
    onChangeLocation: () -> Unit = {},
    onEditPickup: () -> Unit = {},
    onEditDrop: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val liveAddress by LocationTracker.currentAddress.collectAsState()
    val liveCoords by LocationTracker.currentCoordinates.collectAsState()

    var pickup by remember(pickupLocation) { mutableStateOf(pickupLocation) }
    var drop by remember(dropLocation) { mutableStateOf(dropLocation) }

    var pickupCoordinates by remember(pickupLat, pickupLng) {
        mutableStateOf<Pair<Double, Double>?>(
            if (pickupLat != 0.0 && pickupLng != 0.0) Pair(pickupLat, pickupLng) else null
        )
    }
    var dropCoordinates by remember(dropLat, dropLng) {
        mutableStateOf<Pair<Double, Double>?>(
            if (dropLat != 0.0 && dropLng != 0.0) Pair(dropLat, dropLng) else null
        )
    }

    var isPickupSelected by remember(pickup, pickupCoordinates) {
        mutableStateOf(pickup.isNotBlank() || pickupCoordinates != null)
    }
    var isDropSelected by remember(drop, dropCoordinates) {
        mutableStateOf(drop.isNotBlank() || dropCoordinates != null)
    }

    // Auto-fill pickup with live GPS location as soon as detected if empty
    LaunchedEffect(liveAddress, liveCoords) {
        if (!liveAddress.isNullOrBlank() && pickup.isBlank()) {
            pickup = liveAddress ?: ""
            pickupCoordinates = liveCoords
            isPickupSelected = true
        }
    }

    var activeField by remember { mutableStateOf<String?>(null) }
    var suggestions by remember { mutableStateOf<List<AutocompletePlace>>(emptyList()) }
    var isSearchingPlaces by remember { mutableStateOf(false) }
    var showSameLocationDialog by remember { mutableStateOf(false) }

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

    // Fixed Top AD Height across all screens & states
    val topAdHeight = 250.dp

    // Sync search query if passed from search card
    LaunchedEffect(pickupLocation, dropLocation) {
        if (pickupLocation.isNotBlank()) viewModel.onPickupQueryChange(pickupLocation)
        if (dropLocation.isNotBlank()) viewModel.onDropQueryChange(dropLocation)
    }

    val effectivePickupLat = pickupCoordinates?.first ?: if (uiState.pickupLat != 0.0) uiState.pickupLat else pickupLat
    val effectivePickupLng = pickupCoordinates?.second ?: if (uiState.pickupLng != 0.0) uiState.pickupLng else pickupLng
    val effectiveDropLat = dropCoordinates?.first ?: if (uiState.dropLat != 0.0) uiState.dropLat else dropLat
    val effectiveDropLng = dropCoordinates?.second ?: if (uiState.dropLng != 0.0) uiState.dropLng else dropLng

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        // 1. Top AD Banner Box: Fixed 250dp height across all screens & states
        AdBannerBox(
            slotId = AdConfig.SLOT_HOME_TOP_BANNER,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 6.dp, bottom = 8.dp)
                .height(topAdHeight),
            shape = RoundedCornerShape(12.dp)
        )

        // 2. Main Content: When !hasConfirmedRoute, NO MAP is shown (Only Pickup & Drop Card + Ride Button)
        if (!hasConfirmedRoute) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // SCREENSHOT 1 PICKUP & DROP LOCATION SELECTOR
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Timeline Indicator: Green dot -> Vertical Line -> Red dot
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 10.dp)
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
                                        .height(34.dp)
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
                                        .height(44.dp),
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
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            activeField = null
                                            onEditPickup()
                                        },
                                        modifier = Modifier.size(34.dp)
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
                                    thickness = 1.dp,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // 2. Bottom: Drop Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
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
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            activeField = null
                                            onEditDrop()
                                        },
                                        modifier = Modifier.size(34.dp)
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
                    }

                    // Real-time Autocomplete Suggestions Dropdown
                    if (activeField != null && (currentQuery.trim().length >= 2 || isSearchingPlaces || suggestions.isNotEmpty())) {
                        Spacer(modifier = Modifier.height(8.dp))
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

                    Spacer(modifier = Modifier.height(12.dp))

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
                                    val pLat = pickupCoordinates?.first ?: if (pickupLat != 0.0) pickupLat else (liveCoords?.first ?: 28.6139)
                                    val pLng = pickupCoordinates?.second ?: if (pickupLng != 0.0) pickupLng else (liveCoords?.second ?: 77.2090)
                                    val dLat = dropCoordinates?.first ?: if (dropLat != 0.0) dropLat else 28.5355
                                    val dLng = dropCoordinates?.second ?: if (dropLng != 0.0) dropLng else 77.3910
                                    onConfirmRideSearch(pickup.trim(), drop.trim(), pLat, pLng, dLat, dLng)
                                }
                            }
                        },
                        enabled = isRideEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
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

            if (showSameLocationDialog) {
                SameLocationAlertDialog(onDismiss = { showSameLocationDialog = false })
            }
        } else {
            // ROUTE CONFIRMED: Active OSM Map + Status Cards
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            ) {
                if (!uiState.isFallbackListVisible) {
                    InteractiveDemoMap(
                        vehicles = if (uiState.rideBookingStatus == "ACCEPTED" || uiState.rideBookingStatus == "IN_PROGRESS") listOfNotNull(uiState.acceptedVehicle) else uiState.filteredVehicles,
                        selectedVehicle = uiState.selectedVehicle,
                        onVehicleSelect = { viewModel.selectVehicle(it) },
                        onRecenter = { viewModel.recenterDemoArea() },
                        passengerRoutePoints = uiState.passengerRoutePoints,
                        isRideAccepted = uiState.rideBookingStatus == "ACCEPTED" || uiState.rideBookingStatus == "IN_PROGRESS",
                        rideBookingStatus = uiState.rideBookingStatus,
                        pickupGeoPoint = if (effectivePickupLat != 0.0) GeoPoint(effectivePickupLat, effectivePickupLng) else null,
                        dropGeoPoint = if (effectiveDropLat != 0.0) GeoPoint(effectiveDropLat, effectiveDropLng) else null,
                        pickupLocation = pickup.ifBlank { pickupLocation.ifBlank { uiState.pickupQuery } },
                        dropLocation = drop.ifBlank { dropLocation.ifBlank { uiState.dropQuery } },
                        onEditPickup = {
                            onChangeLocation()
                            onEditPickup()
                        },
                        onEditDrop = {
                            onChangeLocation()
                            onEditDrop()
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.filteredVehicles) { vehicle ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                VehicleCard(
                                    vehicle = vehicle,
                                    isSelected = vehicle.vehicleId == uiState.selectedVehicle?.vehicleId,
                                    onClick = { viewModel.selectVehicle(vehicle) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (uiState.rideBookingStatus == "ACCEPTED") {
                // ACCEPTED RIDE: Show dedicated single driver card with live ETA and Call
                val driverVeh = uiState.acceptedVehicle ?: uiState.selectedVehicle
                Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DRIVER ARRIVING",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF15803D)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFDCFCE7)
                        ) {
                            Text(
                                text = "${driverVeh?.etaMinutes ?: 2} mins away",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = driverVeh?.driver?.fullName ?: "Driver Partner",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "${driverVeh?.licensePlateMasked ?: "DL 01"} • ${driverVeh?.modelName ?: "Auto"}",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        // Call Driver Button
                        Surface(
                            shape = CircleShape,
                            color = GlipGreenPrimary,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(38.dp)
                                .clickable {
                                    val phoneToCall = driverVeh?.driverPhone?.ifBlank { "9874561239" } ?: "9874561239"
                                    val callIntent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:$phoneToCall")
                                    }
                                    context.startActivity(callIntent)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call Driver",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4-Digit Ride OTP Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "RIDE OTP",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFB45309)
                                )
                                Text(
                                    text = "Share OTP with driver upon boarding",
                                    fontSize = 9.5.sp,
                                    color = Color(0xFF78350F)
                                )
                            }
                            Text(
                                text = uiState.rideOtp.ifBlank { "4821" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB45309),
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { viewModel.cancelCurrentRide() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Cancel Ride",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else if (uiState.rideBookingStatus == "IN_PROGRESS") {
            // ON TRIP / REACHING DROP LOCATION (Matches user screenshot exactly)
            val driverVeh = uiState.acceptedVehicle ?: uiState.selectedVehicle
            val etaMins = if (uiState.liveEtaMinutes > 0) uiState.liveEtaMinutes else (driverVeh?.etaMinutes ?: 6)
            val distKm = if (uiState.liveTripDistanceKm > 0.0) uiState.liveTripDistanceKm else (uiState.tripDistanceKm.takeIf { it > 0 } ?: 2.6)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Reaching drop location in ",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "$etaMins mins",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Destination is $distKm km away",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        if (uiState.isRidePooled) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFDCFCE7),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC))
                            ) {
                                Text(
                                    text = "Pooled ₹${uiState.liveFareAmount.toInt()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = driverVeh?.driver?.fullName ?: "Driver Partner",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "${driverVeh?.licensePlateMasked ?: "DL 01"} • ${driverVeh?.modelName ?: "Vehicle"}",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF2F2),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                            modifier = Modifier.clickable {
                                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:112")
                                }
                                context.startActivity(dialIntent)
                            }
                        ) {
                            Text(
                                text = "🚨 SOS / Help",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        } else if (uiState.rideBookingStatus == "REQUESTED") {
            // SEARCHING IN PROGRESS: Pipeline yellow progress line with (X) cancel button on right
            SearchingInProgressCard(
                onCancel = { viewModel.cancelCurrentRide() }
            )
        } else {
            val isSearchingRoute = uiState.isSearchingRouteVehicles && uiState.filteredVehicles.isEmpty()
            val isPersonalMode = uiState.isPersonalRideMode || (!uiState.isSearchingRouteVehicles && uiState.filteredVehicles.isEmpty())

            if (isSearchingRoute) {
                // 60-Second Route Vehicle Search Radar Card (Auto-switches to Personal if no route driver joins)
                val effectiveKm = if (uiState.tripDistanceKm > 0.0) uiState.tripDistanceKm else 5.4
                RouteVehicleSearchingCard(
                    pickupLocation = pickupLocation,
                    dropLocation = dropLocation,
                    onChangeLocation = onChangeLocation,
                    secondsRemaining = uiState.routeSearchSecondsRemaining,
                    distanceKm = effectiveKm,
                    onSkipToPersonal = { viewModel.skipRouteSearchAndOpenPersonal() }
                )
            } else {
                // Mode Selector Tabs (only shown when vehicles exist along the corridor)
                if (uiState.filteredVehicles.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tab 1: Shared Pooling
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setPersonalRideMode(false) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (!isPersonalMode) GlipGreenPrimary.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (!isPersonalMode) GlipGreenPrimary else Color(0xFFE2E8F0)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Shared Pooling",
                                fontSize = 11.sp,
                                fontWeight = if (!isPersonalMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isPersonalMode) GlipGreenDark else Color(0xFF475569)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = "Save 48%",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534),
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    // Tab 2: Personal Vehicle
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setPersonalRideMode(true) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isPersonalMode) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isPersonalMode) Color(0xFFF59E0B) else Color(0xFFE2E8F0)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Personal Ride",
                                fontSize = 11.sp,
                                fontWeight = if (isPersonalMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (isPersonalMode) Color(0xFF92400E) else Color(0xFF475569)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFEF08A)
                            ) {
                                Text(
                                    text = "Solo",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF854D0E),
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (isPersonalMode) {
                // RAPIDO / UBER STYLE PERSONAL VEHICLE SELECTOR
                val effectiveKm = if (uiState.tripDistanceKm > 0.0) uiState.tripDistanceKm else 5.4
                val personalOptions = if (uiState.personalOptions.isNotEmpty()) {
                    uiState.personalOptions
                } else {
                    FareCalculationEngine.getPersonalOptions(effectiveKm)
                }
                val selectedPersonal = uiState.selectedPersonalOption ?: personalOptions.firstOrNull { it.type == PersonalVehicleType.BIKE } ?: personalOptions.firstOrNull()

                PersonalRideOptionsCard(
                    pickupLocation = pickupLocation,
                    dropLocation = dropLocation,
                    onChangeLocation = onChangeLocation,
                    options = personalOptions,
                    selectedOption = selectedPersonal,
                    onSelectOption = { viewModel.selectPersonalOption(it) },
                    onBookClick = { opt ->
                        viewModel.bookPersonalRide(opt, userName, userPhone)
                    },
                    canSwitchToPooling = uiState.filteredVehicles.isNotEmpty(),
                    onSwitchToPooling = { viewModel.setPersonalRideMode(false) },
                    distanceKm = effectiveKm,
                    isRequested = uiState.rideBookingStatus == "REQUESTED",
                    isCorridorEmpty = uiState.filteredVehicles.isEmpty()
                )
            } else {
                // SHARED POOLING VEHICLES LIST
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    // Vehicle Details Boxes
                    items(uiState.filteredVehicles) { vehicle ->
                        val isSelected = vehicle.vehicleId == (uiState.selectedVehicle?.vehicleId ?: uiState.filteredVehicles.firstOrNull()?.vehicleId)
                        VehicleCard(
                            vehicle = vehicle,
                            isSelected = isSelected,
                            onClick = { viewModel.selectVehicle(vehicle) },
                            onBookClick = {
                                viewModel.bookSelectedVehicle(vehicle, userName, userPhone)
                            },
                            modifier = Modifier.width(120.dp)
                        )
                    }
                }

                // Quick Request Button for Currently Selected Vehicle
                val sel = uiState.selectedVehicle ?: uiState.filteredVehicles.firstOrNull()
                if (sel != null && uiState.rideBookingStatus == "NONE") {
                    Button(
                        onClick = {
                            viewModel.bookSelectedVehicle(sel, userName, userPhone)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 2.dp)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GlipGreenPrimary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Request Seat with ${sel.driver.fullName} (₹${sel.farePerKmEstimate.toInt()})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }


                }
            }
            }
        }
        }
    }
}
}

/**
 * Rapido / Uber Style Personal Vehicle Selection Card.
 * Displays vehicle categories (Bike, E - Rickshaw, Auto),
 * upfront calculated fares matching Screenshot 1, and Book button.
 */
@Composable
fun PersonalRideOptionsCard(
    pickupLocation: String = "",
    dropLocation: String = "",
    onChangeLocation: () -> Unit = {},
    options: List<PersonalFareEstimate>,
    selectedOption: PersonalFareEstimate?,
    onSelectOption: (PersonalFareEstimate) -> Unit,
    onBookClick: (PersonalFareEstimate) -> Unit,
    canSwitchToPooling: Boolean,
    onSwitchToPooling: () -> Unit,
    distanceKm: Double,
    isRequested: Boolean,
    isCorridorEmpty: Boolean,
    modifier: Modifier = Modifier
) {
    val displayTypes = listOf(
        PersonalVehicleType.BIKE,
        PersonalVehicleType.E_RICKSHAW,
        PersonalVehicleType.AUTO,
        PersonalVehicleType.CAB
    )
    val displayOptions = displayTypes.map { type ->
        options.find { it.type == type } ?: PersonalFareEstimate(
            type = type,
            fareAmount = FareCalculationEngine.calculatePersonalFare(distanceKm, type),
            etaMinutes = 3,
            dropTimeFormatted = "",
            distanceKm = distanceKm,
            isAvailable = true
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // "No shared vehicle on Your Route.." Text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "No shared vehicle on Your Route..",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )

                if (canSwitchToPooling) {
                    Text(
                        text = "Switch to Pooling",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlipGreenPrimary,
                        modifier = Modifier.clickable { onSwitchToPooling() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Vehicle Category Rows: Bike, Personal E Rickshaw, Auto matching Screenshot 1
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                displayOptions.forEach { opt ->
                    val isSelected = opt.type == (selectedOption?.type ?: PersonalVehicleType.BIKE)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectOption(opt) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFFF8FAFC) else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFF234B7D) else Color(0xFFF1F5F9)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: Vehicle Icon + Title
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (opt.type) {
                                            PersonalVehicleType.BIKE -> Icons.Default.TwoWheeler
                                            PersonalVehicleType.E_RICKSHAW -> Icons.Default.ElectricRickshaw
                                            PersonalVehicleType.AUTO -> Icons.Default.ElectricRickshaw
                                            else -> Icons.Default.DirectionsCar
                                        },
                                        contentDescription = opt.type.displayName,
                                        tint = if (isSelected) Color(0xFF234B7D) else Color(0xFF334155),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = when (opt.type) {
                                        PersonalVehicleType.BIKE -> "Bike"
                                        PersonalVehicleType.E_RICKSHAW -> "Personal E Rickshaw"
                                        PersonalVehicleType.AUTO -> "Auto"
                                        else -> opt.type.displayName
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF263959)
                                )
                            }

                            // Right: Upfront Fare Amount ONLY (e.g. ₹24, ₹48, ₹65)
                            Text(
                                text = "₹${opt.fareAmount}",
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF263959)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Booking Button (Rapido warm gold yellow #F5B838)
            val effectiveSel = selectedOption ?: displayOptions.firstOrNull()
            Button(
                onClick = {
                    if (effectiveSel != null && !isRequested) {
                        onBookClick(effectiveSel)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF5B838)
                ),
                enabled = !isRequested
            ) {
                Text(
                    text = if (isRequested) "Requesting Driver..." else "Book",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
            }
        }
    }
}

/**
 * Searching In Progress Card.
 * Shown under the map while waiting for driver acceptance.
 * Contains the yellow animated progress line (pipeline) with an (X) button on the right
 * to cancel the searching request and return back to the booking options card.
 */
@Composable
fun SearchingInProgressCard(
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Yellow animated progress line (pipeline)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFF5B838), // Yellow / Gold progress line
                    trackColor = Color(0xFFFEF3C7) // Light yellow track
                )
            }

            // (X) Cancel icon button to go 1 step back to booking card
            Surface(
                onClick = onCancel,
                shape = CircleShape,
                color = Color(0xFFF1F5F9),
                modifier = Modifier.size(28.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Search",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * 120-Second Route Vehicle Scanning Radar Card.
 * Sleek, compact radar with yellow progress indicator and single "Scanning Wait.." status.
 */
@Composable
fun RouteVehicleSearchingCard(
    pickupLocation: String = "",
    dropLocation: String = "",
    onChangeLocation: () -> Unit = {},
    secondsRemaining: Int,
    distanceKm: Double,
    onSkipToPersonal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(GlipGreenPrimary)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "FINDING SHARED VEHICLES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GlipGreenDark,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFEFF6FF)
                ) {
                    Text(
                        text = timeFormatted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(5.dp))


            // Yellow Progress Bar (Pila line jo aage badte rahta hai)
            LinearProgressIndicator(
                progress = { (120 - secondsRemaining) / 120f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFFF5B838),
                trackColor = Color(0xFFFEF3C7)
            )

            Spacer(modifier = Modifier.height(5.dp))

            // Skip & Book Personal Vehicle Button
            Button(
                onClick = onSkipToPersonal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5B838)),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Skip Wait • Book Personal Ride Now",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

/**
 * Compact Redesigned Vehicle Box:
 * Shows vehicle category, available seats, ETA, and upfront pooling fare.
 * Driver full personal name, phone and license plate are hidden until ride is accepted.
 */
@Composable
fun VehicleCard(
    vehicle: Vehicle,
    isSelected: Boolean,
    onClick: () -> Unit,
    onBookClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val categoryLabel = when (vehicle.category) {
        VehicleCategory.BIKE -> "Bike"
        VehicleCategory.AUTO_RICKSHAW -> "Auto"
        VehicleCategory.E_RICKSHAW -> "E-Rickshaw"
        else -> "Cab"
    }

    val colorInt = try {
        Color(android.graphics.Color.parseColor(vehicle.routeColorHex))
    } catch (_: Exception) {
        GlipGreenPrimary
    }

    Card(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.8.dp else 1.dp,
            color = if (isSelected) colorInt else Color(0xFFE2E8F0)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 7.dp, vertical = 6.dp)
        ) {
            // 1. Vehicle Category Title + Color Dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(colorInt)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = categoryLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // 2. Seat Info & ETA in Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (vehicle.seatAvailability.availableSeats > 0) Color(0xFFDCFCE7) else Color(0xFFFFEDD5)
                ) {
                    Text(
                        text = "${vehicle.seatAvailability.availableSeats}/${vehicle.seatAvailability.totalSeats} seats",
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (vehicle.seatAvailability.availableSeats > 0) GlipGreenDark else GlipOrangeAccent,
                        fontSize = 8.5.sp
                    )
                }

                Text(
                    text = "${vehicle.etaMinutes}m away",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7)
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // 3. Upfront Pooling Fare Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFEFF6FF)
                ) {
                    Text(
                        text = "₹${vehicle.farePerKmEstimate.toInt()}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1D4ED8),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Text(
                    text = "Save 48%",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF16A34A)
                )
            }
        }
    }
}
