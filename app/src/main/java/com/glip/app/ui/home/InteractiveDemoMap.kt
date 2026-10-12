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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.glip.app.domain.model.Vehicle
import com.glip.app.ui.theme.GlipGreenPrimary
import com.glip.app.util.LocationTracker
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Real OpenStreetMap (OSM) Map component via osmdroid.
 * 100% Royalty-Free and Open-Source under ODbL license.
 * Loads real geographic street tiles (street-level zoom 15.0 like Ola/Uber/Rapido) without proprietary billing.
 */
@Composable
fun InteractiveDemoMap(
    vehicles: List<Vehicle>,
    selectedVehicle: Vehicle?,
    onVehicleSelect: (Vehicle) -> Unit,
    onRecenter: () -> Unit,
    isInteractive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    val userCoords by LocationTracker.currentCoordinates.collectAsState()
    val liveAddress by LocationTracker.currentAddress.collectAsState()

    // Helper to generate a clean blue circular marker with a white dot in the center
    val blueLocationIcon = remember(context) {
        val sizePx = (24 * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val center = sizePx / 2f
        val radius = sizePx / 2f

        // Outer white border ring for contrast
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(center, center, radius, paint)

        // Blue circle
        paint.color = android.graphics.Color.parseColor("#2563EB")
        canvas.drawCircle(center, center, radius - 2f * context.resources.displayMetrics.density, paint)

        // Center white dot
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(center, center, radius * 0.35f, paint)

        BitmapDrawable(context.resources, bitmap)
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
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE2E8F0))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                try {
                    Configuration.getInstance().userAgentValue = ctx.packageName
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setBuiltInZoomControls(false) // Remove visible + and - zoom buttons
                        setMultiTouchControls(isInteractive) // Keep pinch-to-zoom enabled
                        isTilesScaledToDpi = true

                        if (!isInteractive) {
                            setOnTouchListener { _, _ -> true } // Block map touch when on search screen
                        }

                        val startLat = userCoords?.first ?: 28.6139
                        val startLng = userCoords?.second ?: 77.2090
                        controller.setZoom(15.0) // Detailed street level zoom
                        controller.setCenter(GeoPoint(startLat, startLng))
                        mapViewRef = this
                    }
                } catch (e: Exception) {
                    android.view.View(ctx)
                }
            },
            update = { view ->
                if (view is MapView) {
                    view.overlays.clear()

                    // User live location marker & map center (blue circular marker with white center dot)
                    userCoords?.let { (uLat, uLng) ->
                        val userMarker = Marker(view).apply {
                            position = GeoPoint(uLat, uLng)
                            title = liveAddress ?: "My Current Location"
                            subDescription = "You are here"
                            icon = blueLocationIcon
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        }
                        view.overlays.add(userMarker)
                        view.controller.setCenter(GeoPoint(uLat, uLng))
                    }

                    // Vehicles markers
                    vehicles.forEach { vehicle ->
                        val marker = Marker(view).apply {
                            position = GeoPoint(vehicle.currentLat, vehicle.currentLng)
                            title = "${vehicle.driver.fullName} (${vehicle.licensePlateMasked})"
                            subDescription = "${vehicle.seatAvailability.availableSeats} seats free"
                            if (isInteractive) {
                                setOnMarkerClickListener { _, _ ->
                                    onVehicleSelect(vehicle)
                                    true
                                }
                            }
                        }
                        view.overlays.add(marker)
                    }
                    view.invalidate()
                }
            }
        )

        if (isInteractive) {
            // Small circular current location button on top-right to recenter without reloading
            SmallFloatingActionButton(
                onClick = {
                    val targetLat = userCoords?.first ?: 28.6139
                    val targetLng = userCoords?.second ?: 77.2090
                    mapViewRef?.controller?.setZoom(15.0)
                    mapViewRef?.controller?.setCenter(GeoPoint(targetLat, targetLng))
                    onRecenter()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(40.dp),
                containerColor = Color.White,
                contentColor = Color(0xFF2563EB),
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Current Location",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // OpenStreetMap Legal Attribution (ODbL Compliance)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 12.dp),
            shape = RoundedCornerShape(6.dp),
            color = Color.White.copy(alpha = 0.88f)
        ) {
            Text(
                text = "© OpenStreetMap contributors",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
