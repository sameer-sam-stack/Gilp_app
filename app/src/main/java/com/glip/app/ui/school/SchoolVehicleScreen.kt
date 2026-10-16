package com.glip.app.ui.school

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.app.data.places.AutocompletePlace
import com.glip.app.data.places.PlacesSearchService
import com.glip.app.data.supabase.RideRequestClient
import com.glip.app.domain.model.UserProfile
import com.glip.app.ui.home.PlaceAutocompleteDropdown
import com.glip.app.util.LocationTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolVehicleScreen(
    userProfile: UserProfile?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        onNavigateBack()
    }

    // Tab state: 0 = "Book Vehicle", 1 = "History" (Image 5 style)
    var selectedTab by remember { mutableIntStateOf(0) }

    // Live coordinates and address
    val liveAddress by LocationTracker.currentAddress.collectAsState()
    val liveCoords by LocationTracker.currentCoordinates.collectAsState()

    // Form states (Image 1 & 2)
    var pickupLocation by remember {
        mutableStateOf(liveAddress ?: "SH5, Gol Bazar, Mamta Nagar")
    }
    var pickupLat by remember { mutableDoubleStateOf(liveCoords?.first ?: 21.0970) }
    var pickupLng by remember { mutableDoubleStateOf(liveCoords?.second ?: 81.0315) }

    var dropLocation by remember { mutableStateOf("") }
    var dropLat by remember { mutableDoubleStateOf(0.0) }
    var dropLng by remember { mutableDoubleStateOf(0.0) }

    var studentName by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("KG 2 (UKG)") }
    var isClassDropdownExpanded by remember { mutableStateOf(false) }

    var pickupTime by remember { mutableStateOf("06:00 AM") }
    var dropTime by remember { mutableStateOf("08:00 AM") }

    // Search autocomplete states
    var isEditingPickup by remember { mutableStateOf(false) }
    var pickupSearchQuery by remember { mutableStateOf("") }
    var pickupSuggestions by remember { mutableStateOf<List<AutocompletePlace>>(emptyList()) }
    var isSearchingPickup by remember { mutableStateOf(false) }
    var pickupJob by remember { mutableStateOf<Job?>(null) }

    var isEditingDrop by remember { mutableStateOf(false) }
    var dropSearchQuery by remember { mutableStateOf("") }
    var dropSuggestions by remember { mutableStateOf<List<AutocompletePlace>>(emptyList()) }
    var isSearchingDrop by remember { mutableStateOf(false) }
    var dropJob by remember { mutableStateOf<Job?>(null) }

    // Time Picker Bottom Sheet states (Image 3)
    var showTimePickerSheet by remember { mutableStateOf(false) }
    var isPickingPickupTime by remember { mutableStateOf(true) } // true = pickup, false = drop
    var pickerHour by remember { mutableIntStateOf(6) }
    var pickerMinute by remember { mutableIntStateOf(0) }
    var pickerAmPm by remember { mutableStateOf("AM") }

    // Request & confirmation state
    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var bookingErrorMessage by remember { mutableStateOf<String?>(null) }

    // Bookings history list
    var bookingsHistory by remember {
        mutableStateOf(SchoolBookingRepository.getBookings(context))
    }

    val classList = listOf(
        "Nursery",
        "KG 1 (LKG)",
        "KG 2 (UKG)",
        "Class 1",
        "Class 2",
        "Class 3",
        "Class 4",
        "Class 5",
        "Class 6",
        "Class 7",
        "Class 8",
        "Class 9",
        "Class 10",
        "Class 11",
        "Class 12"
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
                            text = "Go School Parent",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (selectedTab == 0) "Book Ride" else "Booking History",
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

            // TAB SWITCHER (Image 5 style: [ Book Vehicle | History ])
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Tab 0: Book Vehicle
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (selectedTab == 0) Color.White else Color.Transparent
                            )
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Book Vehicle",
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 0) Color(0xFF0284C7) else Color(0xFF64748B)
                        )
                    }

                    // Tab 1: History
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (selectedTab == 1) Color.White else Color.Transparent
                            )
                            .clickable {
                                bookingsHistory = SchoolBookingRepository.getBookings(context)
                                selectedTab = 1
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "History",
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 1) Color(0xFF0284C7) else Color(0xFF64748B)
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                // TAB 0: RIDE BOOKING FORM (Image 1, 2, 3)
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
                            // Section Header
                            Text(
                                text = "Ride Booking Form",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // PICKUP & DROPOFF SECTION (With vertical connecting line)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Vertical Indicator
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 10.dp, end = 12.dp)
                                ) {
                                    // Green Pin Dot
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    // Connecting vertical line
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(48.dp)
                                            .background(Color(0xFFCBD5E1))
                                    )
                                    // Red Pin Dot
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444))
                                    )
                                }

                                // Inputs Column
                                Column(modifier = Modifier.weight(1f)) {
                                    // 1. Enter Pickup Location (Home)
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
                                            modifier = Modifier.weight(1f),
                                            textStyle = TextStyle(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF0F172A)
                                            ),
                                            cursorBrush = SolidColor(Color(0xFF0284C7)),
                                            decorationBox = { innerTextField ->
                                                if (pickupLocation.isEmpty()) {
                                                    Text(
                                                        text = "Enter Pickup Location (Home)",
                                                        fontSize = 14.sp,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        )

                                        // GPS Target Icon (Auto-detect location)
                                        IconButton(
                                            onClick = {
                                                liveAddress?.let { pickupLocation = it }
                                                liveCoords?.let {
                                                    pickupLat = it.first
                                                    pickupLng = it.second
                                                }
                                                isEditingPickup = false
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MyLocation,
                                                contentDescription = "Current Location",
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

                                    // 2. Enter Dropoff Location (School)
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
                                            modifier = Modifier.weight(1f),
                                            textStyle = TextStyle(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF0F172A)
                                            ),
                                            cursorBrush = SolidColor(Color(0xFF0284C7)),
                                            decorationBox = { innerTextField ->
                                                if (dropLocation.isEmpty()) {
                                                    Text(
                                                        text = "Enter Dropoff Location (School)",
                                                        fontSize = 14.sp,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        )
                                    }
                                }
                            }

                            // Autocomplete Dropdowns if typing
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

                            Spacer(modifier = Modifier.height(18.dp))

                            // STUDENT NAME & CLASS ROW (Side by side - Image 1 & 2)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Left: Student Name
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Student Name",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "Student",
                                                tint = Color(0xFF0284C7),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            BasicTextField(
                                                value = studentName,
                                                onValueChange = { studentName = it },
                                                modifier = Modifier.weight(1f),
                                                textStyle = TextStyle(
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF0F172A)
                                                ),
                                                cursorBrush = SolidColor(Color(0xFF0284C7)),
                                                decorationBox = { innerTextField ->
                                                    if (studentName.isEmpty()) {
                                                        Text(
                                                            text = "Enter name",
                                                            fontSize = 13.sp,
                                                            color = Color(0xFF94A3B8)
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            )
                                        }
                                    }
                                }

                                // Right: Class (Optional) with Dropdown (Image 2)
                                Box(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isClassDropdownExpanded = true },
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isClassDropdownExpanded) Color(0xFF0284C7) else Color(0xFFCBD5E1)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "Class (Optional)",
                                                fontSize = 11.sp,
                                                color = if (isClassDropdownExpanded) Color(0xFF0284C7) else Color(0xFF64748B)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = selectedClass,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF0F172A),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Icon(
                                                    imageVector = if (isClassDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                    contentDescription = "Dropdown",
                                                    tint = Color(0xFF475569)
                                                )
                                            }
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = isClassDropdownExpanded,
                                        onDismissRequest = { isClassDropdownExpanded = false },
                                        modifier = Modifier
                                            .width(160.dp)
                                            .background(Color.White)
                                    ) {
                                        classList.forEach { cls ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = cls,
                                                        fontSize = 14.sp,
                                                        fontWeight = if (cls == selectedClass) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (cls == selectedClass) Color(0xFF0284C7) else Color(0xFF0F172A)
                                                    )
                                                },
                                                onClick = {
                                                    selectedClass = cls
                                                    isClassDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // PICKUP TIME & DROP TIME ROW (Side by side - Image 1 & 3)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Left: Pickup Time
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            isPickingPickupTime = true
                                            parseTimeToPicker(pickupTime, { h, m, a ->
                                                pickerHour = h
                                                pickerMinute = m
                                                pickerAmPm = a
                                            })
                                            showTimePickerSheet = true
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Pickup Time",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.DirectionsCar,
                                                contentDescription = "Pickup Car",
                                                tint = Color(0xFF0284C7),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = pickupTime,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                    }
                                }

                                // Right: Drop Time
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            isPickingPickupTime = false
                                            parseTimeToPicker(dropTime, { h, m, a ->
                                                pickerHour = h
                                                pickerMinute = m
                                                pickerAmPm = a
                                            })
                                            showTimePickerSheet = true
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Drop Time",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = "Drop Clock",
                                                tint = Color(0xFF0284C7),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = dropTime,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                    }
                                }
                            }

                            if (bookingErrorMessage != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = bookingErrorMessage!!,
                                    fontSize = 12.sp,
                                    color = Color(0xFFEF4444)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // REQUEST FOR DRIVER BUTTON (Image 1)
                            Button(
                                onClick = {
                                    if (pickupLocation.isBlank()) {
                                        bookingErrorMessage = "Please enter pickup location (Home)"
                                        return@Button
                                    }
                                    if (dropLocation.isBlank()) {
                                        bookingErrorMessage = "Please enter dropoff location (School)"
                                        return@Button
                                    }
                                    if (studentName.isBlank()) {
                                        bookingErrorMessage = "Please enter student name"
                                        return@Button
                                    }
                                    bookingErrorMessage = null
                                    isSubmitting = true

                                    coroutineScope.launch {
                                        val uId = userProfile?.userId ?: "user-${System.currentTimeMillis()}"
                                        val parentName = userProfile?.fullName?.ifBlank { "Parent" } ?: "Parent"
                                        val parentPhone = userProfile?.phoneNumber?.ifBlank { "+91 9876543210" } ?: "+91 9876543210"

                                        // 1. Submit ride request to Supabase
                                        RideRequestClient.createRideRequest(
                                            userId = uId,
                                            userName = "$studentName ($selectedClass) - Parent: $parentName",
                                            userPhone = parentPhone,
                                            pickupAddress = pickupLocation,
                                            pickupLat = pickupLat,
                                            pickupLng = pickupLng,
                                            dropAddress = "[School Vehicle] $dropLocation (Pickup: $pickupTime, Drop: $dropTime)",
                                            dropLat = dropLat,
                                            dropLng = dropLng,
                                            fare = 80.0
                                        )

                                        // 2. Save into local bookings repository
                                        val newBooking = SchoolBookingItem(
                                            studentName = studentName.trim(),
                                            studentClass = selectedClass,
                                            pickupLocation = pickupLocation.trim(),
                                            dropoffLocation = dropLocation.trim(),
                                            pickupTime = pickupTime,
                                            dropTime = dropTime,
                                            status = "Requested",
                                            fareEstimate = 80.0
                                        )
                                        SchoolBookingRepository.saveBooking(context, newBooking)
                                        bookingsHistory = SchoolBookingRepository.getBookings(context)

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
                                    containerColor = Color(0xFF1E88E5), // Vibrant primary blue
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsCar,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Request For Driver",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // TAB 1: HISTORY (List of School Vehicle Bookings)
                if (bookingsHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBus,
                                contentDescription = "No Bookings",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No School Vehicle Bookings Yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your daily school commute requests will appear here.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { selectedTab = 0 },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                            ) {
                                Text("Book a School Ride")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(bookingsHistory, key = { it.id }) { item ->
                            SchoolBookingCard(item = item)
                        }
                    }
                }
            }
        }
    }

    // TIME PICKER MODAL BOTTOM SHEET (Exact match to Image 3)
    if (showTimePickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTimePickerSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(44.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFCBD5E1))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title (Image 3: "Select Pickup Time" / "Select Drop Time")
                Text(
                    text = if (isPickingPickupTime) "Select Pickup Time" else "Select Drop Time",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Time picker container card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Hour Column (Up, Number, Down)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = {
                                    pickerHour = if (pickerHour >= 12) 1 else pickerHour + 1
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropUp,
                                    contentDescription = "Hour Up",
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = String.format("%02d", pickerHour),
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            IconButton(
                                onClick = {
                                    pickerHour = if (pickerHour <= 1) 12 else pickerHour - 1
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Hour Down",
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Colon separator
                        Text(
                            text = ":",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        // Minute Column (Up, Number, Down)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = {
                                    pickerMinute = (pickerMinute + 5) % 60
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropUp,
                                    contentDescription = "Minute Up",
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = String.format("%02d", pickerMinute),
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            IconButton(
                                onClick = {
                                    pickerMinute = if (pickerMinute <= 0) 55 else pickerMinute - 5
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Minute Down",
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        // AM/PM Toggle Button (Image 3: Emerald green pill button)
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    pickerAmPm = if (pickerAmPm == "AM") "PM" else "AM"
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981) // Emerald green
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pickerAmPm,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Buttons: Cancel & Confirm (Image 3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedButton(
                        onClick = { showTimePickerSheet = false },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                    }

                    Button(
                        onClick = {
                            val formatted = String.format("%02d:%02d %s", pickerHour, pickerMinute, pickerAmPm)
                            if (isPickingPickupTime) {
                                pickupTime = formatted
                            } else {
                                dropTime = formatted
                            }
                            showTimePickerSheet = false
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                    ) {
                        Text(
                            text = "Confirm",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                selectedTab = 1 // Switch to History
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
                    text = "School Vehicle Request Sent!",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Your request for $studentName ($selectedClass) has been submitted. A verified school driver will be assigned shortly.",
                    fontSize = 14.sp,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        selectedTab = 1 // Switch to History tab
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E88E5),
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
}

/**
 * Booking History Card
 */
@Composable
fun SchoolBookingCard(item: SchoolBookingItem) {
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
            // Header Row: Student name, class and status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.studentName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = item.studentClass,
                            fontSize = 12.sp,
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (item.status == "Driver Assigned") Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                ) {
                    Text(
                        text = item.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.status == "Driver Assigned") Color(0xFF166534) else Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Route Details
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pickup: ${item.pickupLocation}",
                    fontSize = 13.sp,
                    color = Color(0xFF334155),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Drop: ${item.dropoffLocation}",
                    fontSize = 13.sp,
                    color = Color(0xFF334155),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Timings & Booking timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pickup: ${item.pickupTime} • Drop: ${item.dropTime}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0284C7)
                )
                Text(
                    text = item.bookingDate,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

/**
 * Parses "06:00 AM" into Hour (int), Minute (int), AM/PM (string)
 */
private fun parseTimeToPicker(timeStr: String, onParsed: (Int, Int, String) -> Unit) {
    try {
        val parts = timeStr.trim().split(" ")
        val timePart = parts[0]
        val amPm = if (parts.size > 1) parts[1].uppercase() else "AM"
        val hm = timePart.split(":")
        val h = hm[0].toIntOrNull() ?: 6
        val m = if (hm.size > 1) hm[1].toIntOrNull() ?: 0 else 0
        onParsed(h, m, amPm)
    } catch (e: Exception) {
        onParsed(6, 0, "AM")
    }
}
