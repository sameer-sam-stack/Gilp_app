package com.glip.app.ui.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.glip.app.ui.theme.GlipGreenPrimary
import com.glip.app.util.LocationTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/**
 * LocationPickerScreen:
 * Opens a full-screen interactive OSM map with a fixed center pin.
 * The user drags and zooms the map under the fixed pin.
 * Live center coordinates are reverse-geocoded to show the address preview.
 * Tapping "Confirm Location" returns exact latitude, longitude, and readable address.
 */
@Composable
fun LocationPickerScreen(
    title: String,
    initialLat: Double?,
    initialLng: Double?,
    onLocationConfirmed: (address: String, lat: Double, lng: Double) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    val startLat = initialLat ?: LocationTracker.currentCoordinates.value?.first ?: 28.6139
    val startLng = initialLng ?: LocationTracker.currentCoordinates.value?.second ?: 77.2090

    var centerLat by remember { mutableStateOf(startLat) }
    var centerLng by remember { mutableStateOf(startLng) }
    var addressPreview by remember { mutableStateOf("Locating...") }
    var isGeocoding by remember { mutableStateOf(false) }

    // Reverse-geocode whenever map center stabilizes
    LaunchedEffect(centerLat, centerLng) {
        isGeocoding = true
        val resolved = withContext(Dispatchers.IO) {
            LocationTracker.reverseGeocode(context, centerLat, centerLng)
        }
        addressPreview = resolved
        isGeocoding = false
    }

    DisposableEffect(Unit) {
        onDispose {
            mapViewRef?.onPause()
            mapViewRef?.onDetach()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // 1. Full-screen Interactive OSM Map
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
            factory = { ctx ->
                Configuration.getInstance().userAgentValue = ctx.packageName
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setBuiltInZoomControls(false)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    isHorizontalMapRepetitionEnabled = false
                    isVerticalMapRepetitionEnabled = false
                    setDestroyMode(false)

                    controller.setZoom(16.5)
                    controller.setCenter(GeoPoint(startLat, startLng))

                    // Listen to map scrolling / panning under fixed center pin
                    addMapListener(DelayedMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            val mapCenter = mapCenter
                            centerLat = mapCenter.latitude
                            centerLng = mapCenter.longitude
                            return true
                        }

                        override fun onZoom(event: ZoomEvent?): Boolean {
                            val mapCenter = mapCenter
                            centerLat = mapCenter.latitude
                            centerLng = mapCenter.longitude
                            return true
                        }
                    }, 250))

                    mapViewRef = this
                }
            }
        )

        // 2. Fixed Center Pin (Stays stationary in dead-center while user drags map underneath)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 36.dp), // Offset slightly so the needle tip points to exact dead center
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Pin Head & Needle
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (title.contains("Drop", ignoreCase = true)) Color(0xFFEF4444) else Color(0xFF2563EB)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
                // Small needle stem / shadow point
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 8.dp)
                        .background(Color.Black.copy(alpha = 0.7f))
                )
                // Small ground shadow dot
                Box(
                    modifier = Modifier
                        .size(width = 12.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                )
            }
        }

        // 3. Top Bar (Back button, Title, and GPS Recenter button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 20.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.size(42.dp)
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF18181B)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF18181B),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.size(42.dp)
            ) {
                IconButton(onClick = {
                    val myCoords = LocationTracker.currentCoordinates.value
                    if (myCoords != null) {
                        mapViewRef?.controller?.animateTo(GeoPoint(myCoords.first, myCoords.second))
                        centerLat = myCoords.first
                        centerLng = myCoords.second
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "My Location",
                        tint = Color(0xFF2563EB)
                    )
                }
            }
        }

        // 4. Bottom Floating Card with Address Preview & "Confirm Location" Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 20.dp)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = if (title.contains("Drop", ignoreCase = true)) "Selected Drop Location" else "Selected Pickup Location",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = addressPreview,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    fontSize = 14.sp,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        onLocationConfirmed(addressPreview, centerLat, centerLng)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18181B))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = GlipGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Confirm Location",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
