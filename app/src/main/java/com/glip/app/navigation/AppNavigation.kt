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

@Composable
fun AppNavigation(
    preferences: DemoProfilePreferences,
    modifier: Modifier = Modifier
) {
    val userRepository = remember { UserRepositoryImpl(preferences) }
    val mobilityRepository = remember { DemoMobilityRepositoryImpl() }

    var isRegistered by remember { mutableStateOf(preferences.getUserProfile() != null) }

    // On app reopen / start, show the centered Ride Search screen first!
    var hasSearchedRide by remember { mutableStateOf(false) }
    var selectedPickup by remember { mutableStateOf("") }
    var selectedDrop by remember { mutableStateOf("") }

    // Bottom Navigation Tab: 0 = Ride, 1 = All Services, 2 = Profile
    var currentBottomTab by remember { mutableIntStateOf(0) }

    if (!isRegistered) {
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
            onConfirmRideSearch = { pickup, drop ->
                selectedPickup = pickup
                selectedDrop = drop
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
                    HomeScreen(
                        viewModel = homeViewModel,
                        pickupLocation = selectedPickup,
                        dropLocation = selectedDrop,
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
    NavigationBar(
        modifier = Modifier.height(60.dp),
        tonalElevation = 6.dp,
        containerColor = Color(0xFFFFFFFF),
        windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    ) {
        NavigationBarItem(
            selected = currentTab == 0,
            onClick = { onTabSelected(0) },
            icon = {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "Ride",
                    modifier = Modifier.size(22.dp)
                )
            },
            label = {
                Text(
                    text = "Ride",
                    fontSize = 11.sp,
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
                    modifier = Modifier.size(22.dp)
                )
            },
            label = {
                Text(
                    text = "All Services",
                    fontSize = 11.sp,
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
                    modifier = Modifier.size(22.dp)
                )
            },
            label = {
                Text(
                    text = "Profile",
                    fontSize = 11.sp,
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
