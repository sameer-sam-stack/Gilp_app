package com.glip.app.ui.personal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.glip.app.data.places.AutocompletePlace
import com.glip.app.data.places.PlacesSearchService
import com.glip.app.domain.model.UserProfile
import com.glip.app.ui.home.LocationPickerScreen
import com.glip.app.ui.home.PlaceAutocompleteDropdown
import com.glip.app.util.LocationTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalVehicleScreen(
    userProfile: UserProfile?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        onNavigateBack()
    }

    // Live coordinates and address
    val liveAddress by LocationTracker.currentAddress.collectAsState()
    val liveCoords by LocationTracker.currentCoordinates.collectAsState()

    var pickupLocation by remember {
        mutableStateOf(liveAddress ?: "SH5, Gol Bazar, Mamta Nagar, Rajnandgaon")
    }
    var pickupLat by remember { mutableDoubleStateOf(liveCoords?.first ?: 21.0970) }
    var pickupLng by remember { mutableDoubleStateOf(liveCoords?.second ?: 81.0315) }

    var dropLocation by remember { mutableStateOf("") }
    var dropLat by remember { mutableDoubleStateOf(0.0) }
    var dropLng by remember { mutableDoubleStateOf(0.0) }

    // Selected vehicle option: "erickshaw", "auto", "cab"
    var selectedVehicleType by remember { mutableStateOf("erickshaw") }

    // Search autocomplete states
    var isEditingPickup by remember { mutableStateOf(false) }
    var pickupSuggestions by remember { mutableStateOf<List<AutocompletePlace>>(emptyList()) }
    var isSearchingPickup by remember { mutableStateOf(false) }
    var pickupJob by remember { mutableStateOf<Job?>(null) }

    var isEditingDrop by remember { mutableStateOf(false) }
    var dropSuggestions by remember { mutableStateOf<List<AutocompletePlace>>(emptyList()) }
    var isSearchingDrop by remember { mutableStateOf(false) }
    var dropJob by remember { mutableStateOf<Job?>(null) }

    // Map picker overlay state
    var selectingLocationMode by remember { mutableStateOf<String?>(null) }

    // Dialog state
    var showSuccessDialog by remember { mutableStateOf(false) }
    var bookingErrorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Local-only bookings list
    var bookingsHistory by remember {
        mutableStateOf(PersonalBookingRepository.getBookings(context))
    }

    val vehicleOptions = listOf(
        VehicleChoice(
            id = "erickshaw",
            name = "E - Rickshaw",
            fare = 15.0,
            etaMinutes = 4,
            capacity = "4 Seats",
            tag = "Eco Friendly",
            description = "Silent, smooth 4-seater electric local ride"
        ),
        VehicleChoice(
            id = "auto",
            name = "Auto",
            fare = 20.0,
            etaMinutes = 3,
            capacity = "5 Seats",
            tag = "Most Popular",
            description = "Private 5-seater auto ride directly to your destination"
        ),
        VehicleChoice(
            id = "cab",
            name = "CAB",
            fare = 25.0,
            etaMinutes = 5,
            capacity = "4 Seats",
            tag = "AC Comfort",
            description = "Comfortable 4-seater air-conditioned private cab"
        )
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Personal Vehicle",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Book Private Ride",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // BOOKING FORM
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "Private Ride (No Co-Passengers)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // PICKUP & DROPOFF SECTION
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Vertical Indicator
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 10.dp, end = 12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(48.dp)
                                            .background(Color(0xFFCBD5E1))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444))
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    // 1. Enter Pickup Location
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BasicTextField(
                                            value = pickupLocation,
                                            onValueChange = { query ->
                                                pickupLocation = query
                                                pickupJob?.cancel()
                                                if (query.trim().length >= 2) {
                                                    isSearchingPickup = true
                                                    isEditingPickup = true
                                                    pickupJob = coroutineScope.launch {
                                                        delay(250)
                                                        pickupSuggestions = PlacesSearchService.searchNearbyPlaces(
                                                            context = context,
                                                            query = query,
                                                            userLat = pickupLat,
                                                            userLng = pickupLng
                                                        )
                                                        isSearchingPickup = false
                                                    }
                                                } else {
                                                    pickupSuggestions = emptyList()
                                                    isSearchingPickup = false
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .onFocusChanged { focusState ->
                                                    if (focusState.isFocused) isEditingPickup = true
                                                },
                                            textStyle = TextStyle(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF0F172A)
                                            ),
                                            cursorBrush = SolidColor(Color(0xFF16A34A)),
                                            decorationBox = { innerTextField ->
                                                if (pickupLocation.isEmpty()) {
                                                    Text(
                                                        text = "Enter Pickup Location",
                                                        fontSize = 14.sp,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        )

                                        // Green Target Icon (Opens map picker)
                                        IconButton(
                                            onClick = {
                                                liveAddress?.let { pickupLocation = it }
                                                liveCoords?.let {
                                                    pickupLat = it.first
                                                    pickupLng = it.second
                                                }
                                                selectingLocationMode = "pickup"
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MyLocation,
                                                contentDescription = "Select Pickup on Map",
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(
                                        color = Color(0xFFF1F5F9),
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )

                                    // 2. Enter Dropoff Location
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BasicTextField(
                                            value = dropLocation,
                                            onValueChange = { query ->
                                                dropLocation = query
                                                dropJob?.cancel()
                                                if (query.trim().length >= 2) {
                                                    isSearchingDrop = true
                                                    isEditingDrop = true
                                                    dropJob = coroutineScope.launch {
                                                        delay(250)
                                                        dropSuggestions = PlacesSearchService.searchNearbyPlaces(
                                                            context = context,
                                                            query = query,
                                                            userLat = pickupLat,
                                                            userLng = pickupLng
                                                        )
                                                        isSearchingDrop = false
                                                    }
                                                } else {
                                                    dropSuggestions = emptyList()
                                                    isSearchingDrop = false
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .onFocusChanged { focusState ->
                                                    if (focusState.isFocused) isEditingDrop = true
                                                },
                                            textStyle = TextStyle(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF0F172A)
                                            ),
                                            cursorBrush = SolidColor(Color(0xFF16A34A)),
                                            decorationBox = { innerTextField ->
                                                if (dropLocation.isEmpty()) {
                                                    Text(
                                                        text = "Enter Drop Location",
                                                        fontSize = 14.sp,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        )

                                        // Red Target Icon (Opens map picker)
                                        IconButton(
                                            onClick = {
                                                selectingLocationMode = "drop"
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MyLocation,
                                                contentDescription = "Select Drop on Map",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Autocomplete dropdowns
                            if (isEditingPickup && (pickupSuggestions.isNotEmpty() || isSearchingPickup)) {
                                PlaceAutocompleteDropdown(
                                    suggestions = pickupSuggestions,
                                    isLoading = isSearchingPickup,
                                    searchQuery = pickupLocation,
                                    activeField = "pickup",
                                    onPlaceSelected = { place ->
                                        pickupLocation = place.fullAddress
                                        pickupLat = place.latitude
                                        pickupLng = place.longitude
                                        isEditingPickup = false
                                        pickupSuggestions = emptyList()
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            if (isEditingDrop && (dropSuggestions.isNotEmpty() || isSearchingDrop)) {
                                PlaceAutocompleteDropdown(
                                    suggestions = dropSuggestions,
                                    isLoading = isSearchingDrop,
                                    searchQuery = dropLocation,
                                    activeField = "drop",
                                    onPlaceSelected = { place ->
                                        dropLocation = place.fullAddress
                                        dropLat = place.latitude
                                        dropLng = place.longitude
                                        isEditingDrop = false
                                        dropSuggestions = emptyList()
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // VEHICLE OPTIONS SELECTION
                    Text(
                        text = "Choose Your Vehicle",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        vehicleOptions.forEach { opt ->
                            val isSelected = selectedVehicleType == opt.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedVehicleType = opt.id },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) Color(0xFFF0FDF4) else Color.White,
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) Color(0xFF16A34A) else Color(0xFFE2E8F0)
                                ),
                                shadowElevation = if (isSelected) 2.dp else 1.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (opt.id) {
                                                "bike" -> Icons.Default.TwoWheeler
                                                "cab" -> Icons.Default.DirectionsCar
                                                else -> Icons.Default.ElectricRickshaw
                                            },
                                            contentDescription = opt.name,
                                            tint = if (isSelected) Color(0xFF16A34A) else Color(0xFF64748B),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = opt.name,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFFEF3C7)
                                            ) {
                                                Text(
                                                    text = opt.tag,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB45309),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${opt.etaMinutes} mins away • ${opt.capacity}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "₹${opt.fare.toInt()}",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "Fixed fare",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF16A34A),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Error banner if any
                    bookingErrorMessage?.let { err ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEE2E2),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                        ) {
                            Text(
                                text = err,
                                fontSize = 13.sp,
                                color = Color(0xFFB91C1C),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // SUBMIT BUTTON (Book Personal Vehicle)
                    val selectedChoice = vehicleOptions.first { it.id == selectedVehicleType }
                    Button(
                        onClick = {
                            if (pickupLocation.isBlank()) {
                                bookingErrorMessage = "Please enter your pickup location"
                                return@Button
                            }
                            if (dropLocation.isBlank()) {
                                bookingErrorMessage = "Please enter your drop location"
                                return@Button
                            }
                            bookingErrorMessage = null
                            isSubmitting = true

                            coroutineScope.launch {
                                delay(300)
                                // Save strictly locally to app SharedPreferences (NOT in database)
                                val newBooking = PersonalBookingItem(
                                    vehicleName = selectedChoice.name,
                                    vehicleType = selectedChoice.id,
                                    pickupLocation = pickupLocation.trim(),
                                    dropoffLocation = dropLocation.trim(),
                                    fare = selectedChoice.fare,
                                    etaMinutes = selectedChoice.etaMinutes,
                                    status = "Driver Assigned"
                                )
                                PersonalBookingRepository.saveBooking(context, newBooking)
                                bookingsHistory = PersonalBookingRepository.getBookings(context)

                                isSubmitting = false
                                showSuccessDialog = true
                            }
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF16A34A),
                            contentColor = Color.White
                        )
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Book ${selectedChoice.name} (₹${selectedChoice.fare.toInt()})",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
        }
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onNavigateBack()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Personal Vehicle Booked!",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "A driver has been assigned to your private ride.",
                    fontSize = 14.sp,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF16A34A),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "View in History", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Interactive Map Picker Screen Overlay
    if (selectingLocationMode != null) {
        val isPickup = selectingLocationMode == "pickup"
        val title = if (isPickup) "Select Pickup on Map" else "Select Drop on Map"
        val currentLat = if (isPickup) {
            if (pickupLat != 0.0) pickupLat else 21.0970
        } else {
            if (dropLat != 0.0) dropLat else 21.0970
        }
        val currentLng = if (isPickup) {
            if (pickupLng != 0.0) pickupLng else 81.0315
        } else {
            if (dropLng != 0.0) dropLng else 81.0315
        }
        val otherAddress = if (isPickup) dropLocation else pickupLocation
        val otherCoords = if (isPickup) {
            if (dropLat != 0.0 && dropLng != 0.0) Pair(dropLat, dropLng) else null
        } else {
            if (pickupLat != 0.0 && pickupLng != 0.0) Pair(pickupLat, pickupLng) else null
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f)
        ) {
            LocationPickerScreen(
                title = title,
                initialLat = currentLat,
                initialLng = currentLng,
                otherLocationAddress = otherAddress.ifBlank { null },
                otherLocationCoords = otherCoords,
                onLocationConfirmed = { detailedAddress, lat, lng ->
                    if (isPickup) {
                        pickupLocation = detailedAddress
                        pickupLat = lat
                        pickupLng = lng
                        isEditingPickup = false
                        pickupSuggestions = emptyList()
                    } else {
                        dropLocation = detailedAddress
                        dropLat = lat
                        dropLng = lng
                        isEditingDrop = false
                        dropSuggestions = emptyList()
                    }
                    selectingLocationMode = null
                },
                onDismiss = {
                    selectingLocationMode = null
                }
            )
        }
    }
}

private data class VehicleChoice(
    val id: String,
    val name: String,
    val fare: Double,
    val etaMinutes: Int,
    val capacity: String,
    val tag: String,
    val description: String
)

@Composable
fun PersonalBookingCard(item: PersonalBookingItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Vehicle name & status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (item.vehicleType) {
                                "bike" -> Icons.Default.TwoWheeler
                                "cab" -> Icons.Default.DirectionsCar
                                else -> Icons.Default.ElectricRickshaw
                            },
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.vehicleName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "${item.driverName} • ${item.vehicleNumber}",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (item.status) {
                        "Completed" -> Color(0xFFE2E8F0)
                        else -> Color(0xFFDCFCE7)
                    }
                ) {
                    Text(
                        text = item.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (item.status) {
                            "Completed" -> Color(0xFF475569)
                            else -> Color(0xFF15803D)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(
                color = Color(0xFFF1F5F9),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Route: Pickup & Drop
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 4.dp, end = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Box(
                        modifier = Modifier
                            .width(1.5.dp)
                            .height(26.dp)
                            .background(Color(0xFFCBD5E1))
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.pickupLocation,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = item.dropoffLocation,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Row: Fare & Booking timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fare: ₹${item.fare.toInt()} (Private)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF15803D)
                )
                Text(
                    text = item.bookingDate,
                    fontSize = 11.5.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}
