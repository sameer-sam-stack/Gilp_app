package com.haze.app.ui.parcel

import androidx.activity.compose.BackHandler
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haze.app.domain.model.UserProfile
import com.haze.app.data.places.AutocompletePlace
import com.haze.app.data.places.PlacesSearchService
import com.haze.app.data.supabase.RideRequestClient
import com.haze.app.ui.home.PlaceAutocompleteDropdown
import com.haze.app.ui.home.LocationPickerScreen
import androidx.compose.ui.zIndex
import com.haze.app.ui.theme.HazeBrandYellow
import com.haze.app.util.LocationTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParcelBookingScreen(
    userProfile: UserProfile?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        onNavigateBack()
    }

    // Default user info
    val fallbackName = userProfile?.fullName?.ifBlank { "Sameer" } ?: "Sameer"
    val fallbackPhone = userProfile?.phoneNumber?.ifBlank { "9752304981" } ?: "9752304981"

    // Live coordinates and address
    val liveAddress by LocationTracker.currentAddress.collectAsState()
    val liveCoords by LocationTracker.currentCoordinates.collectAsState()

    // Pickup state
    var pickupAddress by remember {
        mutableStateOf(liveAddress ?: "208, Sankar Nagar, Motipur, Rajnandgaon, Chhattisgarh 491441")
    }
    var pickupLat by remember { mutableDoubleStateOf(liveCoords?.first ?: 21.0970) }
    var pickupLng by remember { mutableDoubleStateOf(liveCoords?.second ?: 81.0315) }
    var senderName by remember { mutableStateOf(fallbackName) }
    var senderPhone by remember { mutableStateOf(fallbackPhone) }

    // Drop state
    var dropAddress by remember { mutableStateOf("") }
    var dropLat by remember { mutableDoubleStateOf(0.0) }
    var dropLng by remember { mutableDoubleStateOf(0.0) }
    var receiverName by remember { mutableStateOf(fallbackName) }
    var receiverPhone by remember { mutableStateOf(fallbackPhone) }

    // Drop search state
    var dropSearchQuery by remember { mutableStateOf("") }
    var dropSuggestions by remember { mutableStateOf<List<AutocompletePlace>>(emptyList()) }
    var isSearchingDrop by remember { mutableStateOf(false) }
    var isEditingDrop by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Pickup edit state
    var isEditingPickup by remember { mutableStateOf(false) }
    var pickupSearchQuery by remember { mutableStateOf("") }
    var pickupSuggestions by remember { mutableStateOf<List<AutocompletePlace>>(emptyList()) }
    var isSearchingPickup by remember { mutableStateOf(false) }
    var pickupSearchJob by remember { mutableStateOf<Job?>(null) }

    // Map selection mode: "pickup" or "drop"
    var selectingLocationMode by remember { mutableStateOf<String?>(null) }

    // Bottom sheet & modal states
    var showContactSheet by remember { mutableStateOf(false) }
    var showProhibitedDialog by remember { mutableStateOf(false) }
    var showBookingSuccessDialog by remember { mutableStateOf(false) }
    var isSubmittingBooking by remember { mutableStateOf(false) }

    // Contact bottom sheet inputs
    var houseBuildingInput by remember { mutableStateOf("") }
    var contactNameInput by remember { mutableStateOf(receiverName) }
    var contactPhoneInput by remember { mutableStateOf(receiverPhone) }
    var useMyContactChecked by remember { mutableStateOf(true) }

    // Sync live address once available
    LaunchedEffect(liveAddress) {
        if (!liveAddress.isNullOrBlank() && pickupAddress.contains("208, Sankar Nagar")) {
            pickupAddress = liveAddress!!
            liveCoords?.let {
                pickupLat = it.first
                pickupLng = it.second
            }
        }
    }

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
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }
                    Text(
                        text = "Send Parcel",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {

            // Top decorative banner / greeting card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                border = BorderStroke(1.dp, Color(0xFFFFEDD5))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Instant Door-to-Door Delivery",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9A3412)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Send gifts, food, documents & packages safely",
                            fontSize = 12.sp,
                            color = Color(0xFFC2410C)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = "Parcel",
                        tint = Color(0xFFEA580C),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // 1. PICKUP CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Top Row: Green Location Marker + "Pickup from" + Top-right Edit Icon (when not editing)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Green Pin Icon
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Pickup location",
                                tint = Color(0xFF15803D), // Emerald green
                                modifier = Modifier.size(24.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height(2.dp)
                                    .background(Color(0xFF15803D), RoundedCornerShape(1.dp))
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "Pickup from",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(1f)
                        )

                        if (pickupAddress.isNotBlank() && !isEditingPickup) {
                            IconButton(
                                onClick = {
                                    isEditingPickup = true
                                    pickupSearchQuery = ""
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Pickup",
                                    tint = Color(0xFF334155),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isEditingPickup || pickupAddress.isBlank()) {
                        // Search input Box (Right side with Map icon to choose from map)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.5.dp, Color(0xFF15803D))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color(0xFF15803D),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                BasicTextField(
                                    value = pickupSearchQuery,
                                    onValueChange = { query ->
                                        pickupSearchQuery = query
                                        pickupSearchJob?.cancel()
                                        if (query.trim().length >= 2) {
                                            isSearchingPickup = true
                                            pickupSearchJob = coroutineScope.launch {
                                                delay(250)
                                                val places = PlacesSearchService.searchNearbyPlaces(
                                                    context = context,
                                                    query = query,
                                                    userLat = pickupLat,
                                                    userLng = pickupLng
                                                )
                                                pickupSuggestions = places
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
                                    cursorBrush = SolidColor(Color(0xFF15803D)),
                                    decorationBox = { innerTextField ->
                                        if (pickupSearchQuery.isEmpty()) {
                                            Text(
                                                text = "Search pickup address...",
                                                fontSize = 14.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                if (pickupSearchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            pickupSearchQuery = ""
                                            pickupSuggestions = emptyList()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Surface(
                                    onClick = {
                                        selectingLocationMode = "pickup"
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MyLocation,
                                            contentDescription = "Select Pickup on Map",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Map",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B)
                                        )
                                    }
                                }
                            }
                        }

                        if (pickupSuggestions.isNotEmpty() || isSearchingPickup) {
                            PlaceAutocompleteDropdown(
                                suggestions = pickupSuggestions,
                                isLoading = isSearchingPickup,
                                searchQuery = pickupSearchQuery,
                                activeField = "pickup",
                                onPlaceSelected = { place ->
                                    pickupAddress = place.fullAddress
                                    pickupLat = place.latitude
                                    pickupLng = place.longitude
                                    isEditingPickup = false
                                    pickupSuggestions = emptyList()
                                },
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    } else {
                        Text(
                            text = pickupAddress,
                            fontSize = 13.sp,
                            color = Color(0xFF475569),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(start = 36.dp)
                        )
                    }

                    // Dashed Divider
                    Spacer(modifier = Modifier.height(12.dp))
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                    ) {
                        drawLine(
                            color = Color(0xFFCBD5E1),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                            strokeWidth = 1.5f
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Sender Contact Info
                    Column {
                        Text(
                            text = "Sender Details",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "$senderName ($senderPhone)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }

            // Floating Switch Button (Matches exact placement between cards)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp, top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 3.dp,
                    modifier = Modifier.clickable {
                        if (dropAddress.isNotBlank()) {
                            val tempAddr = pickupAddress
                            val tempLat = pickupLat
                            val tempLng = pickupLng
                            val tempName = senderName
                            val tempPhone = senderPhone

                            pickupAddress = dropAddress
                            pickupLat = dropLat
                            pickupLng = dropLng
                            senderName = receiverName
                            senderPhone = receiverPhone

                            dropAddress = tempAddr
                            dropLat = tempLat
                            dropLng = tempLng
                            receiverName = tempName
                            receiverPhone = tempPhone

                            isEditingPickup = false
                            isEditingDrop = false
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Switch",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Switch",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }

            // 2. DROP CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header Row: Red Concentric Pin + "Drop to" + Top-right Edit Icon (when not editing)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Concentric Red Target Pin
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .border(3.5.dp, Color(0xFFDC2626), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDC2626))
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "Drop to",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(1f)
                        )

                        // If drop is selected and not editing, show edit button on top right
                        if (dropAddress.isNotBlank() && !isEditingDrop) {
                            IconButton(
                                onClick = {
                                    isEditingDrop = true
                                    dropSearchQuery = ""
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Drop",
                                    tint = Color(0xFF334155),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (dropAddress.isBlank() || isEditingDrop) {
                        // Search Box with Blue Border
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(26.dp),
                            color = Color.White,
                            border = BorderStroke(1.8.dp, Color(0xFF2563EB))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                BasicTextField(
                                    value = dropSearchQuery,
                                    onValueChange = { query ->
                                        dropSearchQuery = query
                                        searchJob?.cancel()
                                        if (query.trim().length >= 2) {
                                            isSearchingDrop = true
                                            searchJob = coroutineScope.launch {
                                                delay(250)
                                                val places = PlacesSearchService.searchNearbyPlaces(
                                                    context = context,
                                                    query = query,
                                                    userLat = pickupLat,
                                                    userLng = pickupLng
                                                )
                                                dropSuggestions = places
                                                isSearchingDrop = false
                                            }
                                        } else {
                                            dropSuggestions = emptyList()
                                            isSearchingDrop = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    textStyle = TextStyle(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color(0xFF0F172A)
                                    ),
                                    cursorBrush = SolidColor(Color(0xFF2563EB)),
                                    decorationBox = { innerTextField ->
                                        if (dropSearchQuery.isEmpty()) {
                                            Text(
                                                text = "Search drop address",
                                                fontSize = 15.sp,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                if (dropSearchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            dropSearchQuery = ""
                                            dropSuggestions = emptyList()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Surface(
                                    onClick = {
                                        selectingLocationMode = "drop"
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MyLocation,
                                            contentDescription = "Select Drop on Map",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Map",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B)
                                        )
                                    }
                                }
                            }
                        }

                        // Real-time Autocomplete Dropdown
                        if (dropSuggestions.isNotEmpty() || isSearchingDrop) {
                            Spacer(modifier = Modifier.height(8.dp))
                            PlaceAutocompleteDropdown(
                                suggestions = dropSuggestions,
                                isLoading = isSearchingDrop,
                                searchQuery = dropSearchQuery,
                                activeField = "drop",
                                onPlaceSelected = { place ->
                                    dropAddress = place.fullAddress
                                    dropLat = place.latitude
                                    dropLng = place.longitude
                                    isEditingDrop = false
                                    dropSearchQuery = ""
                                    dropSuggestions = emptyList()
                                }
                            )
                        }
                    } else {
                        // Drop Address selected
                        Text(
                            text = dropAddress,
                            fontSize = 13.sp,
                            color = Color(0xFF475569),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(start = 34.dp)
                        )

                        // Dashed Divider
                        Spacer(modifier = Modifier.height(12.dp))
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                        ) {
                            drawLine(
                                color = Color(0xFFCBD5E1),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                                strokeWidth = 1.5f
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // Receiver Contact Info (Clickable to edit receiver details)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    contactNameInput = receiverName
                                    contactPhoneInput = receiverPhone
                                    useMyContactChecked = (receiverName == senderName && receiverPhone == senderPhone)
                                    showContactSheet = true
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Receiver Details",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "$receiverName ($receiverPhone)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Receiver Contact",
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Edit",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Prohibited Items & T&C Section (Only visible when drop address is filled, Screenshot 2)
            if (dropAddress.isNotBlank()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Read about prohibited items",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2563EB),
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { showProhibitedDialog = true }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "By continuing, you agree to our T&Cs",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bright Yellow "Continue" Button (Screenshot 2)
                Button(
                    onClick = {
                        // Open contact bottom sheet
                        contactNameInput = receiverName
                        contactPhoneInput = receiverPhone
                        useMyContactChecked = (receiverName == senderName && receiverPhone == senderPhone)
                        showContactSheet = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HazeBrandYellow,
                        contentColor = Color(0xFF0F172A)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = "Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            }
        }
    }

    // BOTTOM SHEET: House No. & Add Contact Details (Screenshot 4)
    if (showContactSheet) {
        ModalBottomSheet(
            onDismissRequest = { showContactSheet = false },
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
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .imePadding()
            ) {
                // 1. House no./ Building (optional)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "House/Building",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicTextField(
                            value = houseBuildingInput,
                            onValueChange = { houseBuildingInput = it },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(Color(0xFF2563EB)),
                            decorationBox = { innerTextField ->
                                if (houseBuildingInput.isEmpty()) {
                                    Text(
                                        text = "House no./ Building (optional)",
                                        fontSize = 15.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Heading: "Add contact details"
                Text(
                    text = "Add contact details",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Name* input with Contact Book icon
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Name",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicTextField(
                            value = contactNameInput,
                            onValueChange = {
                                contactNameInput = it
                                useMyContactChecked = (it == senderName && contactPhoneInput == senderPhone)
                            },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(Color(0xFF2563EB)),
                            decorationBox = { innerTextField ->
                                if (contactNameInput.isEmpty()) {
                                    Text(
                                        text = "Name*",
                                        fontSize = 15.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                innerTextField()
                            }
                        )
                        Icon(
                            imageVector = Icons.Default.AccountBox,
                            contentDescription = "Contacts",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Checkbox: "Use my contact for this booking"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val nextChecked = !useMyContactChecked
                            useMyContactChecked = nextChecked
                            if (nextChecked) {
                                contactNameInput = senderName
                                contactPhoneInput = senderPhone
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = useMyContactChecked,
                        onCheckedChange = { checked ->
                            useMyContactChecked = checked
                            if (checked) {
                                contactNameInput = senderName
                                contactPhoneInput = senderPhone
                            }
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF2563EB),
                            checkmarkColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Use my contact for this booking",
                        fontSize = 14.sp,
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Phone Number* input
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Phone Number",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicTextField(
                            value = contactPhoneInput,
                            onValueChange = {
                                contactPhoneInput = it
                                useMyContactChecked = (contactNameInput == senderName && it == senderPhone)
                            },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(Color(0xFF2563EB)),
                            decorationBox = { innerTextField ->
                                if (contactPhoneInput.isEmpty()) {
                                    Text(
                                        text = "Phone Number*",
                                        fontSize = 15.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Action: Confirm Parcel Booking Button
                Button(
                    onClick = {
                        val finalReceiverName = contactNameInput.ifBlank { senderName }
                        val finalReceiverPhone = contactPhoneInput.ifBlank { senderPhone }
                        receiverName = finalReceiverName
                        receiverPhone = finalReceiverPhone

                        isSubmittingBooking = true
                        coroutineScope.launch {
                            val uId = userProfile?.userId ?: "user-${System.currentTimeMillis()}"
                            val fullDropLocation = if (houseBuildingInput.isNotBlank()) {
                                "$houseBuildingInput, $dropAddress"
                            } else {
                                dropAddress
                            }

                            RideRequestClient.createRideRequest(
                                passengerName = "$senderName -> $finalReceiverName",
                                passengerMobile = "$senderPhone / $finalReceiverPhone",
                                pickupAddress = pickupAddress,
                                pickupLat = pickupLat,
                                pickupLng = pickupLng,
                                dropAddress = "[Parcel] $fullDropLocation",
                                dropLat = dropLat,
                                dropLng = dropLng,
                                fare = 49.0
                            )

                            isSubmittingBooking = false
                            showContactSheet = false
                            showBookingSuccessDialog = true
                        }
                    },
                    enabled = !isSubmittingBooking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HazeBrandYellow,
                        contentColor = Color(0xFF0F172A)
                    )
                ) {
                    if (isSubmittingBooking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF0F172A),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Confirm & Book Parcel",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Prohibited Items Dialog
    if (showProhibitedDialog) {
        AlertDialog(
            onDismissRequest = { showProhibitedDialog = false },
            title = {
                Text(
                    text = "Prohibited Items",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "For safety and legal compliance, the following items are strictly prohibited for parcel delivery:",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                    Text(text = "• Flammable liquids, explosives or hazardous materials", fontSize = 13.sp, color = Color(0xFF334155))
                    Text(text = "• Firearms, weapons, knives or ammunition", fontSize = 13.sp, color = Color(0xFF334155))
                    Text(text = "• Illegal substances, drugs or narcotics", fontSize = 13.sp, color = Color(0xFF334155))
                    Text(text = "• Cash, currency notes, coins or bullion", fontSize = 13.sp, color = Color(0xFF334155))
                    Text(text = "• Unpackaged fragile glassware or liquids", fontSize = 13.sp, color = Color(0xFF334155))
                    Text(text = "• Animals, birds or biological specimens", fontSize = 13.sp, color = Color(0xFF334155))
                }
            },
            confirmButton = {
                TextButton(onClick = { showProhibitedDialog = false }) {
                    Text(text = "I Understand", fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Booking Success Dialog
    if (showBookingSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showBookingSuccessDialog = false
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
                    text = "Parcel Booked Successfully!",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "A delivery partner is being assigned. You will be notified as soon as they reach your pickup point.",
                    fontSize = 14.sp,
                    color = Color(0xFF475569),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBookingSuccessDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HazeBrandYellow,
                        contentColor = Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Done",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Interactive Map Picker Screen Overlay (Select on Map)
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
        val otherAddress = if (isPickup) dropAddress else pickupAddress
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
                        pickupAddress = detailedAddress
                        pickupLat = lat
                        pickupLng = lng
                        isEditingPickup = false
                        pickupSuggestions = emptyList()
                    } else {
                        dropAddress = detailedAddress
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
