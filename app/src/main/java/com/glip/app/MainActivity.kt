package com.glip.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.glip.app.data.local.DemoProfilePreferences
import com.glip.app.navigation.AppNavigation
import com.glip.app.ui.theme.GlipTheme

class MainActivity : ComponentActivity() {

    private val demoPreferences by lazy {
        DemoProfilePreferences(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GlipTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(preferences = demoPreferences)
                }
            }
        }
    }
}
