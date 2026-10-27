package com.glip.app.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.app.data.local.DemoProfilePreferences
import com.glip.app.data.repository.DemoMobilityRepositoryImpl
import com.glip.app.data.repository.UserRepositoryImpl
import com.glip.app.ui.home.HomeScreen
import com.glip.app.ui.home.HomeViewModel
import com.glip.app.ui.home.LocationPickerScreen
import com.glip.app.ui.profile.ProfileScreen
import com.glip.app.ui.profile.ProfileViewModel
import com.glip.app.ui.registration.RegistrationScreen
import com.glip.app.ui.registration.RegistrationViewModel
import com.glip.app.ui.services.ServicesScreen
import com.glip.app.ui.services.ServicesViewModel
import com.glip.app.ui.parcel.ParcelBookingScreen
import com.glip.app.ui.school.SchoolVehicleScreen
import com.glip.app.ui.personal.PersonalVehicleScreen
import com.glip.app.ui.wallet.WalletScreen
import com.glip.app.ui.travel.IntercityTravelScreen
import com.glip.app.ui.travel.TravelServicesScreen

import androidx.compose.ui.text.font.FontWeight
import com.glip.app.ui.theme.GlipGreenPrimary
import com.glip.app.ui.components.AppLoadingSplashScreen
import com.glip.app.data.supabase.RideRequestClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    preferences: DemoProfilePreferences,
    modifier: Modifier = Modifier
) {
    val userRepository = remember { UserRepositoryImpl(preferences) }
    val mobilityRepository = remember { DemoMobilityRepositoryImpl() }
    val coroutineScope = rememberCoroutineScope()
    val userProfile by userRepository.getUserProfile().collectAsState(initial = preferences.getUserProfile())

    var isRegistered by remember { mutableStateOf(preferences.getUserProfile() != null) }

    // Splash Loader state: shows RouteLoader on launch so screen is never blank
    var isAppLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Fast, smooth initial entrance animation
        delay(750)
        isAppLoading = false
    }

    // Route confirmed state: false = shows Pickup/Drop input card on HomeScreen, true = shows active route / radar / tracking
    var hasConfirmedRoute by remember { mutableStateOf(false) }
    var selectedPickup by remember { mutableStateOf("") }
    var selectedDrop by remember { mutableStateOf("") }
    var selectedPickupLat by remember { mutableDoubleStateOf(0.0) }
    var selectedPickupLng by remember { mutableDoubleStateOf(0.0) }
    var selectedDropLat by remember { mutableDoubleStateOf(0.0) }
    var selectedDropLng by remember { mutableDoubleStateOf(0.0) }

    val homeViewModel = remember { HomeViewModel(mobilityRepository) }

    LaunchedEffect(userProfile, isRegistered) {
        if (isRegistered) {
            val phone = userProfile?.phoneNumber ?: preferences.getUserProfile()?.phoneNumber
            if (!phone.isNullOrBlank()) {
                val activeRide = homeViewModel.checkAndRestoreActiveRide(phone)
                if (activeRide != null) {
                    activeRide.pickupName?.takeIf { it.isNotBlank() }?.let { selectedPickup = it }
                    activeRide.dropName?.takeIf { it.isNotBlank() }?.let { selectedDrop = it }
                    if (activeRide.pickupLat != 0.0) selectedPickupLat = activeRide.pickupLat
                    if (activeRide.pickupLng != 0.0) selectedPickupLng = activeRide.pickupLng
                    if (activeRide.dropLat != 0.0) selectedDropLat = activeRide.dropLat
                    if (activeRide.dropLng != 0.0) selectedDropLng = activeRide.dropLng
                    hasConfirmedRoute = true
                }
            }
        }
    }

    // Bottom Navigation Tab: 0 = Ride, 1 = All Services, 2 = Profile
    var currentBottomTab by remember { mutableIntStateOf(0) }
    var isParcelBookingOpen by remember { mutableStateOf(false) }
    var isSchoolVehicleOpen by remember { mutableStateOf(false) }
    var isPersonalVehicleOpen by remember { mutableStateOf(false) }
    var isWalletOpen by remember { mutableStateOf(false) }
    var isIntercityTravelOpen by remember { mutableStateOf(false) }
    var isTravelServicesOpen by remember { mutableStateOf(false) }
    var isPersonalSearchFromServices by remember { mutableStateOf(false) }
    var editingLocationMode by remember { mutableStateOf<String?>(null) } // "pickup" or "drop"

    if (isAppLoading) {
        AppLoadingSplashScreen(modifier = modifier)
    } else if (!isRegistered) {
        val regViewModel = remember { RegistrationViewModel(userRepository) }
        RegistrationScreen(
            viewModel = regViewModel,
            onRegistrationSuccess = {
                isRegistered = true
                hasConfirmedRoute = false
                currentBottomTab = 0
            },
            modifier = modifier
        )
    } else if (isWalletOpen) {
        WalletScreen(
            onBack = { isWalletOpen = false },
            modifier = modifier.fillMaxSize()
        )
    } else if (isParcelBookingOpen) {
        ParcelBookingScreen(
            userProfile = userProfile ?: preferences.getUserProfile(),
            onNavigateBack = { isParcelBookingOpen = false },
            modifier = modifier.fillMaxSize()
        )
    } else if (isSchoolVehicleOpen) {
        SchoolVehicleScreen(
            userProfile = userProfile ?: preferences.getUserProfile(),
            onNavigateBack = { isSchoolVehicleOpen = false },
            modifier = modifier.fillMaxSize()
        )
    } else if (isPersonalVehicleOpen) {
        PersonalVehicleScreen(
            userProfile = userProfile ?: preferences.getUserProfile(),
            onNavigateBack = { isPersonalVehicleOpen = false },
            modifier = modifier.fillMaxSize()
        )
} else if (isIntercityTravelOpen) {
        IntercityTravelScreen(
            userProfile = userProfile ?: preferences.getUserProfile(),
            onNavigateBack = { isIntercityTravelOpen = false },
            modifier = modifier.fillMaxSize()
        )
    } else if (isTravelServicesOpen) {
        TravelServicesScreen(
            userProfile = userProfile ?: preferences.getUserProfile(),
            onNavigateBack = { isTravelServicesOpen = false },
            modifier = modifier.fillMaxSize()
        )
    } else if (editingLocationMode != null) {
        // Direct Map Location Picker: "Select Pickup on Map" / "Select Drop on Map"
        val isPickup = editingLocationMode == "pickup"
        val title = if (isPickup) "Select Pickup on Map" else "Select Drop on Map"
        val currentLat = if (isPickup) {
            if (selectedPickupLat != 0.0) selectedPickupLat else null
        } else {
            if (selectedDropLat != 0.0) selectedDropLat else null
        }
        val currentLng = if (isPickup) {
            if (selectedPickupLng != 0.0) selectedPickupLng else null
        } else {
            if (selectedDropLng != 0.0) selectedDropLng else null
        }
        val otherAddress = if (isPickup) selectedDrop else selectedPickup
        val otherCoords = if (isPickup) {
            if (selectedDropLat != 0.0 && selectedDropLng != 0.0) Pair(selectedDropLat, selectedDropLng) else null
        } else {
            if (selectedPickupLat != 0.0 && selectedPickupLng != 0.0) Pair(selectedPickupLat, selectedPickupLng) else null
        }

        LocationPickerScreen(
            title = title,
            initialLat = currentLat,
            initialLng = currentLng,
            otherLocationAddress = otherAddress,
            otherLocationCoords = otherCoords,
            onLocationConfirmed = { newAddress, lat, lng ->
                if (isPickup) {
                    selectedPickup = newAddress
                    selectedPickupLat = lat
                    selectedPickupLng = lng
                } else {
                    selectedDrop = newAddress
                    selectedDropLat = lat
                    selectedDropLng = lng
                }
                if (hasConfirmedRoute) {
                    homeViewModel.setTripCoordinates(
                        pickup = selectedPickup,
                        drop = selectedDrop,
                        pLat = selectedPickupLat,
                        pLng = selectedPickupLng,
                        dLat = selectedDropLat,
                        dLng = selectedDropLng
                    )
                }
                editingLocationMode = null
            },
            onDismiss = {
                editingLocationMode = null
            },
            modifier = modifier.fillMaxSize()
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                GlipCompactBottomBar(
                    currentTab = currentBottomTab,
                    onTabSelected = { currentBottomTab = it }
                )
            }
        ) { innerPadding ->
            val contentModifier = Modifier.padding(innerPadding)
            when (currentBottomTab) {
                0 -> {
                    val profile = userProfile ?: preferences.getUserProfile()
                    HomeScreen(
                        viewModel = homeViewModel,
                        pickupLocation = selectedPickup,
                        dropLocation = selectedDrop,
                        pickupLat = selectedPickupLat,
                        pickupLng = selectedPickupLng,
                        dropLat = selectedDropLat,
                        dropLng = selectedDropLng,
                        hasConfirmedRoute = hasConfirmedRoute,
                        isPersonalSearchFromServices = isPersonalSearchFromServices,
                        userName = profile?.fullName ?: "",
                        userPhone = profile?.phoneNumber ?: "",
                        onConfirmRideSearch = { pickup, drop, pLat, pLng, dLat, dLng ->
                            selectedPickup = pickup
                            selectedDrop = drop
                            selectedPickupLat = pLat
                            selectedPickupLng = pLng
                            selectedDropLat = dLat
                            selectedDropLng = dLng
                            hasConfirmedRoute = true

                            // Pass selected route corridor coordinates to HomeViewModel so matching vehicles appear
                            homeViewModel.setTripCoordinates(pickup, drop, pLat, pLng, dLat, dLng)
                            if (isPersonalSearchFromServices) {
                                homeViewModel.skipRouteSearchAndOpenPersonal()
                                isPersonalSearchFromServices = false
                            }
                        },
                        onChangeLocation = {
                            homeViewModel.cancelCurrentRide()
                            hasConfirmedRoute = false
                            selectedDrop = ""
                            selectedDropLat = 0.0
                            selectedDropLng = 0.0
                        },
                        onEditPickup = {
                            editingLocationMode = "pickup"
                        },
                        onEditDrop = {
                            editingLocationMode = "drop"
                        },
                        onResetHome = {
                            selectedDrop = ""
                            selectedDropLat = 0.0
                            selectedDropLng = 0.0
                            hasConfirmedRoute = false
                        },
                        modifier = contentModifier
                    )
                }
                1 -> {
                    val servicesViewModel = remember { ServicesViewModel(mobilityRepository) }
                    ServicesScreen(
                        viewModel = servicesViewModel,
                        onNavigateToHomePooling = {
                            isPersonalSearchFromServices = false
                            hasConfirmedRoute = false
                            currentBottomTab = 0
                        },
                        onNavigateToParcelBooking = {
                            isParcelBookingOpen = true
                        },
                        onNavigateToSchoolVehicle = {
                            isSchoolVehicleOpen = true
                        },
                        onNavigateToPersonalVehicle = {
                            isPersonalSearchFromServices = true
                            hasConfirmedRoute = false
                            currentBottomTab = 0
                        },
                        onNavigateToWallet = {
                            isWalletOpen = true
                        },
                        onNavigateToIntercityTravel = {
                            isIntercityTravelOpen = true
                        },
                        onNavigateToTravelServices = {
                            isTravelServicesOpen = true
                        },
                        modifier = contentModifier
                    )
                }
                2 -> {
                    val profileViewModel = remember { ProfileViewModel(userRepository) }
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onLoggedOut = {
                            isRegistered = false
                            hasConfirmedRoute = false
                        },
                        modifier = contentModifier
                    )
                }
            }
        }
    }
}

/**
 * Compact, robust Bottom Navigation Bar with clear icons & labels that never clips or disappears
 */
@Composable
fun GlipCompactBottomBar(
    currentTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFFFFFF),
        shadowElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .navigationBarsPadding()
        ) {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                tonalElevation = 0.dp,
                containerColor = Color(0xFFFFFFFF),
                windowInsets = WindowInsets(0.dp)
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { onTabSelected(0) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Ride",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Ride",
                            fontSize = 10.sp,
                            fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1D4ED8),
                        selectedTextColor = Color(0xFF1D4ED8),
                        indicatorColor = Color(0xFFDBEAFE),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { onTabSelected(1) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "All Services",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "All Services",
                            fontSize = 10.sp,
                            fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1D4ED8),
                        selectedTextColor = Color(0xFF1D4ED8),
                        indicatorColor = Color(0xFFDBEAFE),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { onTabSelected(2) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Profile",
                            fontSize = 10.sp,
                            fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1D4ED8),
                        selectedTextColor = Color(0xFF1D4ED8),
                        indicatorColor = Color(0xFFDBEAFE),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )
            }
        }
    }
}
