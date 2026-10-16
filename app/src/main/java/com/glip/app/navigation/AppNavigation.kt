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
import com.glip.app.ui.home.RideSearchScreen
import com.glip.app.ui.profile.ProfileScreen
import com.glip.app.ui.profile.ProfileViewModel
import com.glip.app.ui.registration.RegistrationScreen
import com.glip.app.ui.registration.RegistrationViewModel
import com.glip.app.ui.services.ServicesScreen
import com.glip.app.ui.services.ServicesViewModel

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
        // Display RouteLoader while GPS, tiles and assets initialize smoothly
        delay(2500)
        isAppLoading = false
    }

    // On app reopen / start, show the centered Ride Search screen first!
    var hasSearchedRide by remember { mutableStateOf(false) }
    var selectedPickup by remember { mutableStateOf("") }
    var selectedDrop by remember { mutableStateOf("") }

    // Bottom Navigation Tab: 0 = Ride, 1 = All Services, 2 = Profile
    var currentBottomTab by remember { mutableIntStateOf(0) }

    if (isAppLoading) {
        AppLoadingSplashScreen(modifier = modifier)
    } else if (!isRegistered) {
        val regViewModel = remember { RegistrationViewModel(userRepository) }
        RegistrationScreen(
            viewModel = regViewModel,
            onRegistrationSuccess = {
                isRegistered = true
                hasSearchedRide = false
                currentBottomTab = 0
            },
            modifier = modifier
        )
    } else if (!hasSearchedRide) {
        // App opens directly to Pickup & Drop page. No other tabs (Ride, All Services, Profile)
        // are accessible until Pickup and Drop locations are entered and Ride is clicked!
        RideSearchScreen(
            initialPickup = selectedPickup,
            initialDrop = selectedDrop,
            onConfirmRideSearch = { pickup, drop, pLat, pLng, dLat, dLng ->
                selectedPickup = pickup
                selectedDrop = drop
                hasSearchedRide = true
                currentBottomTab = 0

                // Store in Supabase database so Driver app receives the live ride request
                coroutineScope.launch {
                    val profile = userProfile ?: preferences.getUserProfile()
                    val uId = profile?.userId ?: "user-${System.currentTimeMillis()}"
                    val uName = profile?.fullName?.ifBlank { "Glip Passenger" } ?: "Glip Passenger"
                    val uPhone = profile?.phoneNumber?.ifBlank { "+91 9876543210" } ?: "+91 9876543210"

                    RideRequestClient.createRideRequest(
                        userId = uId,
                        userName = uName,
                        userPhone = uPhone,
                        pickupAddress = pickup,
                        pickupLat = pLat,
                        pickupLng = pLng,
                        dropAddress = drop,
                        dropLat = dLat,
                        dropLng = dLng
                    )
                }
            },
            onCancel = {
                hasSearchedRide = true
                currentBottomTab = 0
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
                    val homeViewModel = remember { HomeViewModel(mobilityRepository) }
                    val profile = userProfile ?: preferences.getUserProfile()
                    HomeScreen(
                        viewModel = homeViewModel,
                        pickupLocation = selectedPickup,
                        dropLocation = selectedDrop,
                        userName = profile?.fullName ?: "",
                        userPhone = profile?.phoneNumber ?: "",
                        onChangeLocation = { hasSearchedRide = false },
                        modifier = contentModifier
                    )
                }
                1 -> {
                    val servicesViewModel = remember { ServicesViewModel(mobilityRepository) }
                    ServicesScreen(
                        viewModel = servicesViewModel,
                        onNavigateToHomePooling = {
                            currentBottomTab = 0
                            hasSearchedRide = true
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
                            hasSearchedRide = false
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
        shadowElevation = 8.dp
    ) {
        NavigationBar(
            tonalElevation = 0.dp,
            containerColor = Color(0xFFFFFFFF),
            windowInsets = WindowInsets.navigationBars
        ) {
            NavigationBarItem(
                selected = currentTab == 0,
                onClick = { onTabSelected(0) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Ride",
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = "Ride",
                        fontSize = 12.sp,
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
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = "All Services",
                        fontSize = 12.sp,
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
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = "Profile",
                        fontSize = 12.sp,
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
