package com.glip.app.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MiscellaneousServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glip.app.R
import com.glip.app.data.local.DemoProfilePreferences
import com.glip.app.data.repository.DemoMobilityRepositoryImpl
import com.glip.app.data.repository.UserRepositoryImpl
import com.glip.app.ui.home.HomeScreen
import com.glip.app.ui.home.HomeViewModel
import com.glip.app.ui.profile.ProfileScreen
import com.glip.app.ui.profile.ProfileViewModel
import com.glip.app.ui.registration.RegistrationScreen
import com.glip.app.ui.registration.RegistrationViewModel
import com.glip.app.ui.services.ServicesScreen
import com.glip.app.ui.services.ServicesViewModel

@Composable
fun AppNavigation(
    preferences: DemoProfilePreferences,
    modifier: Modifier = Modifier
) {
    val userRepository = remember { UserRepositoryImpl(preferences) }
    val mobilityRepository = remember { DemoMobilityRepositoryImpl() }

    // Check if user is already registered locally
    var isRegistered by remember { mutableStateOf(preferences.getUserProfile() != null) }

    // Bottom Navigation Tab Selection (0 = Home, 1 = Services, 2 = Profile)
    var currentBottomTab by remember { mutableIntStateOf(0) }

    if (!isRegistered) {
        val regViewModel = remember { RegistrationViewModel(userRepository) }
        RegistrationScreen(
            viewModel = regViewModel,
            onRegistrationSuccess = {
                isRegistered = true
                currentBottomTab = 0
            },
            modifier = modifier
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                NavigationBar(
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentBottomTab == 0,
                        onClick = { currentBottomTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text(stringResource(id = R.string.nav_home)) }
                    )
                    NavigationBarItem(
                        selected = currentBottomTab == 1,
                        onClick = { currentBottomTab = 1 },
                        icon = { Icon(Icons.Default.MiscellaneousServices, contentDescription = null) },
                        label = { Text(stringResource(id = R.string.nav_services)) }
                    )
                    NavigationBarItem(
                        selected = currentBottomTab == 2,
                        onClick = { currentBottomTab = 2 },
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text(stringResource(id = R.string.nav_profile)) }
                    )
                }
            }
        ) { innerPadding ->
            val contentModifier = Modifier.padding(innerPadding)
            when (currentBottomTab) {
                0 -> {
                    val homeViewModel = remember { HomeViewModel(mobilityRepository) }
                    HomeScreen(
                        viewModel = homeViewModel,
                        modifier = contentModifier
                    )
                }
                1 -> {
                    val servicesViewModel = remember { ServicesViewModel(mobilityRepository) }
                    ServicesScreen(
                        viewModel = servicesViewModel,
                        onNavigateToHomePooling = { currentBottomTab = 0 },
                        modifier = contentModifier
                    )
                }
                2 -> {
                    val profileViewModel = remember { ProfileViewModel(userRepository) }
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onLoggedOut = {
                            isRegistered = false
                        },
                        modifier = contentModifier
                    )
                }
            }
        }
    }
}
