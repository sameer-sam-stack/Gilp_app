package com.haze.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.haze.app.ads.AdManager
import com.haze.app.data.local.DemoProfilePreferences
import com.haze.app.navigation.AppNavigation
import com.haze.app.ui.theme.HazeTheme
import com.haze.app.util.GpsPromptHelper
import com.haze.app.util.LocationTracker

class MainActivity : ComponentActivity() {

    private val demoPreferences by lazy {
        DemoProfilePreferences(applicationContext)
    }

    private val gpsResolutionLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            LocationTracker.refreshLocation(this)
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            if (!LocationTracker.isGpsEnabled(this)) {
                GpsPromptHelper.requestEnableGps(this, gpsResolutionLauncher)
            }
            LocationTracker.refreshLocation(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize advertisement manager at cold start for instant 0ms ad rendering
        AdManager.initialize(applicationContext)

        // Prompt user for Location & GPS immediately on app open
        checkAndPromptLocation()

        setContent {
            HazeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(preferences = demoPreferences)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Fast sync ads when returning to the app
        AdManager.refresh()

        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            LocationTracker.refreshLocation(this)
        }
    }

    private fun checkAndPromptLocation() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            val perms = buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.toTypedArray()
            locationPermissionLauncher.launch(perms)
        } else {
            if (!LocationTracker.isGpsEnabled(this)) {
                GpsPromptHelper.requestEnableGps(this, gpsResolutionLauncher)
            }
            LocationTracker.refreshLocation(this)
        }
    }
}
