package com.haze.app.ui.travel

import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.haze.app.domain.model.UserProfile
import com.haze.app.ui.theme.HazeBrandYellow
import com.haze.app.util.LocationTracker

data class RentalVehicle(
    val id: String,
    val name: String,
    val seatCapacity: String,
    val ratePerKm: String,
    val tag: String,
    val features: String,
    val icon: ImageVector,
    val iconColor: Color,
    val badgeColor: Color
)

val SAMPLE_RENTAL_VEHICLES = listOf(
    RentalVehicle(
        id = "1",
        name = "Toofan",
        seatCapacity = "7 Seat",
        ratePerKm = "?14/km",
        tag = "Popular for CG Tours",
        features = "AC � Diesel � Luggage Carrier",
        icon = Icons.Default.DirectionsCar,
        iconColor = Color(0xFF0284C7),
        badgeColor = Color(0xFFE0F2FE)
    ),
    RentalVehicle(
        id = "2",
        name = "Mini Bus",
        seatCapacity = "20 Seat",
        ratePerKm = "?28/km",
        tag = "Group & Family Specialist",
        features = "2x2 Pushback � AC � Sound System",
        icon = Icons.Default.DirectionsBus,
        iconColor = Color(0xFFEAB308),
        badgeColor = Color(0xFFFEFCE8)
    ),
    RentalVehicle(
        id = "3",
        name = "Tempo Traveller",
        seatCapacity = "14 Seat",
        ratePerKm = "?22/km",
        tag = "Comfortable Long Distance",
        features = "Recliner Seats � AC � USB Charging",
        icon = Icons.Default.AirportShuttle,
        iconColor = Color(0xFF8B5CF6),
        badgeColor = Color(0xFFF5F3FF)
    ),
    RentalVehicle(
        id = "4",
        name = "Innova Crysta",
        seatCapacity = "7 Seat",
        ratePerKm = "?18/km",
        tag = "VIP & Premium Travel",
        features = "Captain Seats � Dual AC � Smooth Ride",
        icon = Icons.Default.DirectionsCar,
        iconColor = Color(0xFF15803D),
        badgeColor = Color(0xFFDCFCE7)
    ),
    RentalVehicle(
        id = "5",
        name = "Cruiser Trax",
        seatCapacity = "12 Seat",
        ratePerKm = "?16/km",
        tag = "Rugged Touring",
        features = "Power Steering � High Clearance",
        icon = Icons.Default.AirportShuttle,
        iconColor = Color(0xFFEA580C),
        badgeColor = Color(0xFFFFEDD5)
    ),
    RentalVehicle(
        id = "6",
        name = "Deluxe Coach Bus",
        seatCapacity = "32 Seat",
        ratePerKm = "?45/km",
        tag = "Weddings & Large Events",
        features = "Air Suspension � Luxury Interiors",
        icon = Icons.Default.DirectionsBus,
        iconColor = Color(0xFF4F46E5),
        badgeColor = Color(0xFFEEF2FF)
    ),
    RentalVehicle(
        id = "7",
        name = "Bolero Neo",
        seatCapacity = "9 Seat",
        ratePerKm = "?13/km",
        tag = "Economical Outstation",
        features = "Spacious Cabin � Reliable Engine",
        icon = Icons.Default.DirectionsCar,
        iconColor = Color(0xFF0F766E),
        badgeColor = Color(0xFFCCFBF1)
    ),
    RentalVehicle(
        id = "8",
        name = "Ertiga",
        seatCapacity = "6 Seat",
        ratePerKm = "?12/km",
        tag = "Budget Friendly",
        features = "CNG / Petrol � Compact Group",
        icon = Icons.Default.DirectionsCar,
        iconColor = Color(0xFF64748B),
        badgeColor = Color(0xFFF1F5F9)
    )
)

val SEAT_OPTIONS = listOf("All", "6 Seat", "7 Seat", "9 Seat", "12 Seat", "14 Seat", "20 Seat", "32 Seat")

data class HubServiceItem(
    val id: String,
    val title: String,
    val category: String,
    val icon: ImageVector,
    val iconColor: Color,
    val backgroundColor: Color,
    val description: String = "",
    val startingPrice: String = ""
)

val ALL_HUB_SERVICES = listOf(
    HubServiceItem(
        id = "travel_outstation",
        title = "Travel Services",
        category = "Travel",
        icon = Icons.Default.AirportShuttle,
        iconColor = Color(0xFF8B5CF6),
        backgroundColor = Color(0xFFF5F3FF),
        description = "Outstation vehicle rental (Toofan, Mini Bus, Innova, Cruiser) across Chhattisgarh.",
        startingPrice = "From ?12/km"
    ),
    HubServiceItem(
        id = "intercity_travels",
        title = "Intercity Bus",
        category = "Travel",
        icon = Icons.Default.DirectionsBus,
        iconColor = Color(0xFF0284C7),
        backgroundColor = Color(0xFFF0F9FF),
        description = "Express intercity bus routes connecting all major cities across CG.",
        startingPrice = "From ?150"
    ),
    HubServiceItem(
        id = "ride_pooling",
        title = "Daily Route Ride",
        category = "Travel",
        icon = Icons.Default.TwoWheeler,
        iconColor = Color(0xFF15803D),
        backgroundColor = Color(0xFFF0FDF4),
        description = "Affordable daily commuter shared rides and pooling along common routes.",
        startingPrice = "From ?30"
    ),
    HubServiceItem(
        id = "parcel_delivery",
        title = "Parcel Delivery",
        category = "Travel",
        icon = Icons.Default.Inventory2,
        iconColor = Color(0xFFEA580C),
        backgroundColor = Color(0xFFFFF7ED),
        description = "Same-day fast doorstep courier & document delivery across the city.",
        startingPrice = "From ?40"
    ),
    HubServiceItem(
        id = "school_vehicle",
        title = "School Vehicle",
        category = "Travel",
        icon = Icons.Default.DirectionsBus,
        iconColor = Color(0xFFEAB308),
        backgroundColor = Color(0xFFFEFCE8),
        description = "Safe, verified daily school cab & van service with real-time GPS tracking.",
        startingPrice = "Monthly passes"
    ),
    HubServiceItem(
        id = "home_cleaning",
        title = "Home Cleaning",
        category = "Cleaning",
        icon = Icons.Default.Home,
        iconColor = Color(0xFF0284C7),
        backgroundColor = Color(0xFFF0F9FF),
        description = "Complete deep home cleaning, floor scrubbing & kitchen sanitization.",
        startingPrice = "From ?499"
    ),
    HubServiceItem(
        id = "sofa_cleaning",
        title = "Sofa & Carpet",
        category = "Cleaning",
        icon = Icons.Default.Home,
        iconColor = Color(0xFFD97706),
        backgroundColor = Color(0xFFFFFBEB),
        description = "Shampooing, fabric stain removal & deep vacuuming for sofas and mattresses.",
        startingPrice = "From ?299"
    ),
    HubServiceItem(
        id = "ac_repair",
        title = "AC Repair & Service",
        category = "Repairs",
        icon = Icons.Default.FlashOn,
        iconColor = Color(0xFF0891B2),
        backgroundColor = Color(0xFFECFEFF),
        description = "Gas refill, cooling check, jet pump foam wash & compressor diagnostics.",
        startingPrice = "From ?399"
    ),
    HubServiceItem(
        id = "electrician",
        title = "Electrician",
        category = "Repairs",
        icon = Icons.Default.FlashOn,
        iconColor = Color(0xFFF59E0B),
        backgroundColor = Color(0xFFFEF3C7),
        description = "Wiring, switchboard, ceiling fan, MCB & home inverter fitting & repair.",
        startingPrice = "From ?149"
    ),
    HubServiceItem(
        id = "plumber",
        title = "Plumber",
        category = "Repairs",
        icon = Icons.Default.Build,
        iconColor = Color(0xFF4F46E5),
        backgroundColor = Color(0xFFEEF2FF),
        description = "Tap leakages, water motor repair, bathroom fittings & pipeline work.",
        startingPrice = "From ?149"
    ),
    HubServiceItem(
        id = "carpenter",
        title = "Carpenter",
        category = "Repairs",
        icon = Icons.Default.Build,
        iconColor = Color(0xFFB45309),
        backgroundColor = Color(0xFFFEF3C7),
        description = "Door locks, cupboard handles, bed assembly, woodwork & hinges.",
        startingPrice = "From ?199"
    ),
    HubServiceItem(
        id = "appliance_repair",
        title = "Appliance Repair",
        category = "Repairs",
        icon = Icons.Default.Build,
        iconColor = Color(0xFF0D9488),
        backgroundColor = Color(0xFFCCFBF1),
        description = "Washing machine, refrigerator, microwave oven & geyser service.",
        startingPrice = "From ?249"
    ),
    HubServiceItem(
        id = "painting_polish",
        title = "Painting & Polish",
        category = "Home",
        icon = Icons.Default.Edit,
        iconColor = Color(0xFFEA580C),
        backgroundColor = Color(0xFFFFEDD5),
        description = "Interior & exterior wall painting, waterproof coating & wood polish.",
        startingPrice = "Free Estimate"
    ),
    HubServiceItem(
        id = "pest_control",
        title = "Pest Control",
        category = "Cleaning",
        icon = Icons.Default.Security,
        iconColor = Color(0xFFDC2626),
        backgroundColor = Color(0xFFFEE2E2),
        description = "Herbal & chemical control for termites, cockroaches, bedbugs & mosquitoes.",
        startingPrice = "From ?599"
    ),
    HubServiceItem(
        id = "salon_women",
        title = "Salon for Women",
        category = "Personal",
        icon = Icons.Default.Person,
        iconColor = Color(0xFFEC4899),
        backgroundColor = Color(0xFFFCE7F3),
        description = "At-home facials, waxing, threading, manicure, pedicure & hair treatments.",
        startingPrice = "From ?299"
    ),
    HubServiceItem(
        id = "salon_men",
        title = "Salon for Men",
        category = "Personal",
        icon = Icons.Default.Person,
        iconColor = Color(0xFF475569),
        backgroundColor = Color(0xFFF1F5F9),
        description = "Grooming, haircut, beard styling, head massage & facial at your doorstep.",
        startingPrice = "From ?199"
    ),
    HubServiceItem(
        id = "car_wash",
        title = "Car Wash",
        category = "Vehicle",
        icon = Icons.Default.DirectionsCar,
        iconColor = Color(0xFF2563EB),
        backgroundColor = Color(0xFFEFF6FF),
        description = "High pressure water wash, interior vacuuming & dashboard polish.",
        startingPrice = "From ?249"
    ),
    HubServiceItem(
        id = "bike_service",
        title = "Bike Service",
        category = "Vehicle",
        icon = Icons.Default.TwoWheeler,
        iconColor = Color(0xFF16A34A),
        backgroundColor = Color(0xFFDCFCE7),
        description = "Doorstep engine oil change, spark plug tuning, brake pad & chain lube.",
        startingPrice = "From ?199"
    ),
    HubServiceItem(
        id = "ro_water",
        title = "RO Purifier",
        category = "Repairs",
        icon = Icons.Default.Build,
        iconColor = Color(0xFF0284C7),
        backgroundColor = Color(0xFFE0F2FE),
        description = "Water purifier filter change, TDS testing, pump repair & membrane flush.",
        startingPrice = "From ?299"
    ),
    HubServiceItem(
        id = "packers_movers",
        title = "Packers & Movers",
        category = "Home",
        icon = Icons.Default.Inventory2,
        iconColor = Color(0xFF7C3AED),
        backgroundColor = Color(0xFFEDE9FE),
        description = "Reliable household shifting, bubble wrapping & safe vehicle transport.",
        startingPrice = "Instant Quote"
    ),
    HubServiceItem(
        id = "cctv_security",
        title = "CCTV Security",
        category = "Home",
        icon = Icons.Default.Security,
        iconColor = Color(0xFF334155),
        backgroundColor = Color(0xFFF8FAFC),
        description = "Security camera setup, DVR configuration & mobile remote view setup.",
        startingPrice = "From ?499"
    ),
    HubServiceItem(
        id = "laundry_service",
        title = "Laundry & Dry Clean",
        category = "Cleaning",
        icon = Icons.Default.Inventory2,
        iconColor = Color(0xFF059669),
        backgroundColor = Color(0xFFD1FAE5),
        description = "Doorstep pickup, steam press, blanket dry cleaning & spotless wash.",
        startingPrice = "From ?79"
    ),
    HubServiceItem(
        id = "gardening",
        title = "Gardening",
        category = "Home",
        icon = Icons.Default.Home,
        iconColor = Color(0xFF15803D),
        backgroundColor = Color(0xFFDCFCE7),
        description = "Lawn mowing, tree trimming, seasonal pest spray & garden beautification.",
        startingPrice = "From ?349"
    ),
    HubServiceItem(
        id = "masonry_work",
        title = "Masonry & Tiles",
        category = "Home",
        icon = Icons.Default.Build,
        iconColor = Color(0xFF9A3412),
        backgroundColor = Color(0xFFFFEDD5),
        description = "Tile fitting, brick repair, wall plastering & renovation civil work.",
        startingPrice = "Free Visit"
    )
)

val CATEGORY_CHIPS = listOf("All", "Travel", "Repairs", "Cleaning", "Home", "Personal", "Vehicle")

val ALL_CITIES_CATALOG = listOf(
    // Chhattisgarh Cities
    "Rajnandgaon",
    "Raipur",
    "Bhilai",
    "Durg",
    "Bilaspur",
    "Korba",
    "Jagdalpur",
    "Ambikapur",
    "Dhamtari",
    "Mahasamund",
    "Kanker",
    "Kawardha",
    "Bhatapara",
    "Champa",
    "Balod",
    "Bemetara",
    "Dongargarh",
    "Khairagarh",
    "Kondagaon",
    "Manendragarh",
    "Sakti",
    "Sarangarh",
    "Surajpur",
    "Jashpur",
    "Gariaband",
    "Mungeli",
    "Baloda Bazar",
    "Janjgir",
    "Baikunthpur",
    "Narayanpur",
    "Dantewada",
    "Sukma",
    "Bijapur",
    // Major Indian Cities
    "Delhi",
    "Mumbai",
    "Bengaluru",
    "Kolkata",
    "Hyderabad",
    "Pune",
    "Indore",
    "Nagpur",
    "Jaipur",
    "Lucknow",
    "Ahmedabad",
    "Bhopal",
    "Patna",
    "Chandigarh",
    "Surat",
    "Kanpur",
    "Varanasi"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelServicesScreen(
    userProfile: UserProfile?,
    onNavigateBack: () -> Unit,
    onNavigateToParcelBooking: () -> Unit = {},
    onNavigateToSchoolVehicle: () -> Unit = {},
    onNavigateToDailyRide: () -> Unit = {},
    onNavigateToIntercityTravel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val defaultName = userProfile?.fullName?.ifBlank { "Sameer" } ?: "Sameer"
    val defaultPhone = userProfile?.phoneNumber?.ifBlank { "9752304981" } ?: "9752304981"

    // Navigation State: "DIRECTORY" (all 20+ services) or "VEHICLE_RENTAL" (Outstation travel)
    var currentViewMode by remember { mutableStateOf("DIRECTORY") }

    // Trigger real-time location refresh when entering screen
    LaunchedEffect(Unit) {
        LocationTracker.refreshLocation(context)
    }

    // Location state from LocationTracker: detected city and sublocality area
    val currentCityDetected by LocationTracker.currentCity.collectAsState()
    val currentAreaDetected by LocationTracker.currentArea.collectAsState()
    val activeCity = currentCityDetected?.ifBlank { "Rajnandgaon" } ?: "Rajnandgaon"
    val activeArea = currentAreaDetected?.ifBlank { null }

    // Directory Search & Filter state
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedServiceForBooking by remember { mutableStateOf<HubServiceItem?>(null) }
    var showBookingDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var isSubmittingBooking by remember { mutableStateOf(false) }
    var activeLeadQuotation by remember { mutableStateOf<com.haze.app.data.supabase.ServiceLeadItem?>(null) }
    var showQuotationStatusDialog by remember { mutableStateOf(false) }
    var isRefreshingQuotes by remember { mutableStateOf(false) }
    var showUnavailableDialog by remember { mutableStateOf(false) }
    var unavailableServiceItem by remember { mutableStateOf<HubServiceItem?>(null) }
    var showCityPickerModal by remember { mutableStateOf(false) }
    var citySearchQuery by remember { mutableStateOf("") }

    // Outstation Vehicle Rental State
    var pickupCity by remember { mutableStateOf("Rajnandgaon") }
    var travelCity by remember { mutableStateOf("Jagdalpur") }
    var isSelectingPickupCity by remember { mutableStateOf(false) }
    var isSelectingTravelCity by remember { mutableStateOf(false) }
    var selectedSeatFilter by remember { mutableStateOf("All") }
    var fuelAndMaintenanceIncluded by remember { mutableStateOf(true) }
    var hasSearched by remember { mutableStateOf(false) }
    var confirmedVehicle by remember { mutableStateOf<RentalVehicle?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (showQuotationStatusDialog) {
            showQuotationStatusDialog = false
        } else if (showUnavailableDialog) {
            showUnavailableDialog = false
            unavailableServiceItem = null
        } else if (showBookingDialog) {
            showBookingDialog = false
            selectedServiceForBooking = null
        } else if (showCityPickerModal) {
            showCityPickerModal = false
        } else if (currentViewMode == "VEHICLE_RENTAL") {
            currentViewMode = "DIRECTORY"
        } else {
            onNavigateBack()
        }
    }

    val filteredServices = remember(searchQuery, selectedCategory) {
        ALL_HUB_SERVICES.filter { service ->
            val matchesCategory = (selectedCategory == "All" || service.category.equals(selectedCategory, ignoreCase = true))
            val matchesQuery = searchQuery.isBlank() ||
                    service.title.contains(searchQuery, ignoreCase = true) ||
                    service.description.contains(searchQuery, ignoreCase = true) ||
                    service.category.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    val displayedVehicles = remember(selectedSeatFilter, hasSearched) {
        if (selectedSeatFilter == "All") {
            SAMPLE_RENTAL_VEHICLES
        } else {
            SAMPLE_RENTAL_VEHICLES.filter { it.seatCapacity.equals(selectedSeatFilter, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (currentViewMode == "VEHICLE_RENTAL") {
                        Column {
                            Text(
                                text = "Travel Services",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Outstation vehicle rental across CG",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    } else {
                        Text(
                            text = "All Services Directory",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentViewMode == "VEHICLE_RENTAL") {
                            currentViewMode = "DIRECTORY"
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (currentViewMode == "DIRECTORY") {
            // ==========================================
            // ALL 20+ SERVICES DIRECTORY SCREEN
            // ==========================================
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFF8FAFC)),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. LOCATION HEADER: Matches Screenshot 2 (Pin icon, Bold City with dropdown arrow, and Area subtext)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showCityPickerModal = true }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeCity,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select City",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            val cleanArea = if (!activeArea.isNullOrBlank()) {
                                activeArea.replace(Regex(",?\\s*" + Regex.escape(activeCity) + "\\b", RegexOption.IGNORE_CASE), "").trim().trimEnd(',')
                            } else ""
                            val subtext = cleanArea.ifBlank { "Current Area" }
                            Text(
                                text = subtext,
                                fontSize = 12.5.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }

                // 2. SEARCH BAR: Live search input for services
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search services",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF0284C7),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )
                }

                // 3. CATEGORY FILTER CHIPS
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(CATEGORY_CHIPS) { category ->
                            val isSelected = category == selectedCategory
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF0F172A) else Color.White,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0)
                                ),
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier.clickable { selectedCategory = category }
                            ) {
                                Text(
                                    text = category,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }


                // 5. COMPACT SERVICES GRID (3 boxes per row, compact size as requested: 'In Sab ke box ka size chota rakh na')
                val chunkedServices = filteredServices.chunked(3)
                items(chunkedServices) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { service ->
                            Box(modifier = Modifier.weight(1f)) {
                                CompactServiceTile(
                                    item = service,
                                    onClick = {
                                        when (service.id) {
                                            "parcel_delivery" -> onNavigateToParcelBooking()
                                            "school_vehicle" -> onNavigateToSchoolVehicle()
                                            "ride_pooling" -> onNavigateToDailyRide()
                                            "intercity_travels" -> onNavigateToIntercityTravel()
                                            "travel_outstation" -> {
                                                currentViewMode = "VEHICLE_RENTAL"
                                            }
                                            else -> {
                                                selectedServiceForBooking = service
                                                showBookingDialog = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        // Fill empty slots in the last row to maintain grid alignment
                        val emptySlots = 3 - rowItems.size
                        repeat(emptySlots) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        } else {
            // ==========================================
            // VEHICLE RENTAL FLOW (Outstation Travel)
            // ==========================================
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFF8FAFC)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Back to Directory banner
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { currentViewMode = "DIRECTORY" },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEEF2FF),
                        border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Back to All Services Directory",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4F46E5)
                            )
                        }
                    }
                }

                // Vehicle Search Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Outstation Vehicle Search & Route",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Horizontal Box: Pickup City | Switch | Travel City
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Pickup City
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { isSelectingPickupCity = true }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF15803D))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Pickup City",
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = pickupCity,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // 2. Switch Button in between
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                        shadowElevation = 2.dp,
                                        modifier = Modifier.clickable {
                                            val temp = pickupCity
                                            pickupCity = travelCity
                                            travelCity = temp
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapVert,
                                            contentDescription = "Switch Cities",
                                            tint = Color(0xFF0F172A),
                                            modifier = Modifier
                                                .padding(6.dp)
                                                .size(20.dp)
                                        )
                                    }

                                    // 3. Travel City
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { isSelectingTravelCity = true }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFDC2626))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Travel City",
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = travelCity,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Seat capacity selector
                            Text(
                                text = "Required Seats",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(SEAT_OPTIONS) { seatOpt ->
                                    val isSelected = seatOpt == selectedSeatFilter
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) HazeBrandYellow else Color(0xFFF1F5F9),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) HazeBrandYellow else Color(0xFFE2E8F0)
                                        ),
                                        modifier = Modifier.clickable { selectedSeatFilter = seatOpt }
                                    ) {
                                        Text(
                                            text = seatOpt,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFF0F172A) else Color(0xFF334155),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Fuel & maintenance checkbox
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        fuelAndMaintenanceIncluded = !fuelAndMaintenanceIncluded
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = fuelAndMaintenanceIncluded,
                                    onCheckedChange = { fuelAndMaintenanceIncluded = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0F172A))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Fuel & Maintenance cost included",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "All toll, driver allowance & fuel calculated",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Search Button
                            Button(
                                onClick = {
                                    hasSearched = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(25.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HazeBrandYellow,
                                    contentColor = Color(0xFF0F172A)
                                ),
                                elevation = ButtonDefaults.buttonElevation(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Search Vehicles",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }

                // Results
                if (hasSearched) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Available Vehicles (${displayedVehicles.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "$pickupCity ? $travelCity",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    items(displayedVehicles) { vehicle ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    confirmedVehicle = vehicle
                                    showConfirmDialog = true
                                },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(vehicle.badgeColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = vehicle.icon,
                                            contentDescription = vehicle.name,
                                            tint = vehicle.iconColor,
                                            modifier = Modifier.size(34.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "PHOTO",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = vehicle.iconColor
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${vehicle.name}, ${vehicle.seatCapacity}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = vehicle.tag,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF0284C7)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = vehicle.features,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${vehicle.ratePerKm} � ${if (fuelAndMaintenanceIncluded) "Fuel Included" else "Standard Rate"}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Select",
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Service Unavailable Popup Dialog ("yaha services abhi nhi hai" in English with Cancel button & BackHandler)
    if (showUnavailableDialog) {
        AlertDialog(
            onDismissRequest = {
                showUnavailableDialog = false
                unavailableServiceItem = null
            },
            title = {
                Text(
                    text = "Service Unavailable",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "This service is currently not available in your area.",
                    fontSize = 14.5.sp,
                    color = Color(0xFF475569),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnavailableDialog = false
                        unavailableServiceItem = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F172A),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Cancel",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }

    // Service Details / Booking Dialog
    if (showBookingDialog && selectedServiceForBooking != null) {
        val srv = selectedServiceForBooking!!
        AlertDialog(
            onDismissRequest = {
                showBookingDialog = false
                selectedServiceForBooking = null
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(srv.backgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = srv.icon,
                        contentDescription = srv.title,
                        tint = srv.iconColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = srv.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = srv.description,
                        fontSize = 13.5.sp,
                        color = Color(0xFF475569),
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Service Area:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(
                                    text = "$activeCity & nearby",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Starting Price:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(
                                    text = srv.startingPrice,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Warranty:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(
                                    text = "30 Days Protection",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Verified professional technician will arrive at your location.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isSubmittingBooking) {
                            coroutineScope.launch {
                                isSubmittingBooking = true
                                val result = com.haze.app.data.supabase.ServicesSupabaseClient.createServiceLead(
                                    customerId = userProfile?.userId ?: "cust-${System.currentTimeMillis()}",
                                    customerName = defaultName,
                                    customerPhone = defaultPhone,
                                    serviceCategory = srv.category.lowercase().replace(" ", "_"),
                                    serviceTitle = srv.title,
                                    description = srv.description,
                                    city = activeCity,
                                    address = activeArea
                                )
                                isSubmittingBooking = false
                                showBookingDialog = false
                                result.fold(
                                    onSuccess = { lead ->
                                        activeLeadQuotation = lead
                                        showQuotationStatusDialog = true
                                        Toast.makeText(context, "Request sent to $activeCity providers! Tracking quote...", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { err ->
                                        Toast.makeText(context, "Request received! Team in $activeCity will call you shortly.", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        }
                    },
                    enabled = !isSubmittingBooking,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HazeBrandYellow,
                        contentColor = Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSubmittingBooking) {
                        CircularProgressIndicator(
                            color = Color(0xFF0F172A),
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Book Service Now",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBookingDialog = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancel",
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }

    // Live Quotation & Provider Response Dialog for Customer
    if (showQuotationStatusDialog && activeLeadQuotation != null) {
        val lead = activeLeadQuotation!!
        val isQuoted = lead.status.equals("quoted", ignoreCase = true)
        val isAccepted = lead.status.equals("accepted", ignoreCase = true)

        AlertDialog(
            onDismissRequest = {
                showQuotationStatusDialog = false
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isQuoted || isAccepted) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAccepted) Icons.Default.CheckCircle else if (isQuoted) Icons.Default.Call else Icons.Default.Schedule,
                        contentDescription = "Status",
                        tint = if (isQuoted || isAccepted) Color(0xFF15803D) else Color(0xFFB45309),
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = if (isAccepted) "Booking Confirmed! ðŸŽ‰" else if (isQuoted) "Quotation Received! ðŸ’°" else "Request Sent to $activeCity ðŸ“¨",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Service: ${lead.description.ifBlank { lead.serviceCategory }}",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("City:", fontSize = 12.5.sp, color = Color(0xFF64748B))
                                Text(lead.city, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Status:", fontSize = 12.5.sp, color = Color(0xFF64748B))
                                Text(
                                    text = when (lead.status.lowercase()) {
                                        "new" -> "Waiting for Provider Quote"
                                        "quoted" -> "Quotation Ready"
                                        "accepted" -> "Accepted & Scheduled"
                                        else -> lead.status.replaceFirstChar { it.uppercase() }
                                    },
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isQuoted || isAccepted) Color(0xFF15803D) else Color(0xFFD97706)
                                )
                            }

                            if (lead.visitingCharges > 0 || isQuoted) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFE2E8F0))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Visiting / Service Charge:", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                    Text("â‚¹${lead.visitingCharges.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF15803D))
                                }
                            }

                            if (!lead.estimatedArrival.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Estimated Arrival:", fontSize = 12.5.sp, color = Color(0xFF64748B))
                                    Text(lead.estimatedArrival, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                }
                            }

                            if (!lead.providerNotes.isNullOrBlank()) {
                                Text(
                                    text = "Provider Note: ${lead.providerNotes}",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    if (!isQuoted && !isAccepted) {
                        Text(
                            text = "Providers in $activeCity are reviewing your request. Hit Refresh to check for latest quotations.",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                if (isQuoted && !isAccepted) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val acceptRes = com.haze.app.data.supabase.ServicesSupabaseClient.acceptQuotation(lead.id)
                                acceptRes.onSuccess {
                                    activeLeadQuotation = lead.copy(status = "accepted")
                                    Toast.makeText(context, "Quotation accepted! Provider notified.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF15803D),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Accept Quote & Confirm", fontWeight = FontWeight.Bold)
                    }
                } else if (!isAccepted) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isRefreshingQuotes = true
                                val refreshRes = com.haze.app.data.supabase.ServicesSupabaseClient.fetchCustomerLeads(defaultPhone)
                                isRefreshingQuotes = false
                                refreshRes.onSuccess { list ->
                                    val updated = list.firstOrNull { it.id == lead.id }
                                    if (updated != null) {
                                        activeLeadQuotation = updated
                                        if (updated.status.equals("quoted", ignoreCase = true)) {
                                            Toast.makeText(context, "Quotation received from provider!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Checked: Still awaiting provider quote.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isRefreshingQuotes,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HazeBrandYellow,
                            contentColor = Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isRefreshingQuotes) "Checking..." else "Refresh Quotes", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showQuotationStatusDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // City Selection Bottom Sheets for Outstation Rental
    if (isSelectingPickupCity) {
        CitySelectionBottomSheet(
            title = "Select Pickup City",
            selectedCity = pickupCity,
            onCitySelected = { pickupCity = it },
            onDismiss = { isSelectingPickupCity = false }
        )
    }

    if (isSelectingTravelCity) {
        CitySelectionBottomSheet(
            title = "Select Travel City",
            selectedCity = travelCity,
            onCitySelected = { travelCity = it },
            onDismiss = { isSelectingTravelCity = false }
        )
    }

    // Vehicle Booking Confirmation Dialog
    if (showConfirmDialog && confirmedVehicle != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDCFCE7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Confirmed",
                        tint = Color(0xFF15803D),
                        modifier = Modifier.size(34.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Booking Confirmed! ??",
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Your ${confirmedVehicle!!.name} (${confirmedVehicle!!.seatCapacity}) booking has been confirmed successfully.",
                        fontSize = 14.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Vehicle:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(
                                    text = "${confirmedVehicle!!.name} (${confirmedVehicle!!.seatCapacity})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Route:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(
                                    text = "$pickupCity ? $travelCity",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Fuel & Maint.:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(
                                    text = if (fuelAndMaintenanceIncluded) "Included" else "Excluded",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (fuelAndMaintenanceIncluded) Color(0xFF15803D) else Color(0xFF64748B)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Rate:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(
                                    text = confirmedVehicle!!.ratePerKm,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Booked by:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(
                                    text = "$defaultName ($defaultPhone)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Our verified driver will contact you for departure time coordination.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
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
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            }
        )
    }
    // CITY SELECTION MODAL MATCHING SCREENSHOT 3
    if (showCityPickerModal) {
        Dialog(
            onDismissRequest = {
                showCityPickerModal = false
                citySearchQuery = ""
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Header: Back Arrow + "Location"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                showCityPickerModal = false
                                citySearchQuery = ""
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF0F172A)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Location",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    // Search City Box matching Screenshot 3
                    OutlinedTextField(
                        value = citySearchQuery,
                        onValueChange = { citySearchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search city, area or locality",
                                fontSize = 14.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (citySearchQuery.isNotEmpty()) {
                                IconButton(onClick = { citySearchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0F172A),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    // "All cities" Section Header
                    Text(
                        text = "All cities",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val filteredCities = remember(citySearchQuery) {
                        ALL_CITIES_CATALOG.filter {
                            citySearchQuery.isBlank() || it.contains(citySearchQuery, ignoreCase = true)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        items(filteredCities) { city ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        LocationTracker.setManualCity(city)
                                        showCityPickerModal = false
                                        citySearchQuery = ""
                                        Toast.makeText(context, "Location set to $city", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = city,
                                        fontSize = 15.sp,
                                        fontWeight = if (city.equals(activeCity, ignoreCase = true)) FontWeight.Bold else FontWeight.Medium,
                                        color = if (city.equals(activeCity, ignoreCase = true)) Color(0xFF0F172A) else Color(0xFF1E293B)
                                    )
                                    if (city.equals(activeCity, ignoreCase = true)) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color(0xFF15803D),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(
                                    color = Color(0xFFF1F5F9),
                                    thickness = 1.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
/**
 * Compact service card matching the outer services style ('same Box jaise bahr hai'),
 * with a smaller, compact footprint ('In Sab ke box ka size chota rakh na').
 */
@Composable
fun CompactServiceTile(
    item: HubServiceItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(82.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(0.8.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(item.backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.iconColor,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.title,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 12.sp
            )
        }
    }
}
