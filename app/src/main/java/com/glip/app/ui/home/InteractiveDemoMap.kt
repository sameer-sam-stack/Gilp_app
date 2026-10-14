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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.glip.app.domain.model.Vehicle
import com.glip.app.ui.theme.GlipGreenPrimary
import com.glip.app.ui.map.MapStyles
import com.glip.app.ui.map.RapidoLocationOverlay
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
    isBlurred: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var locationOverlay by remember { mutableStateOf<RapidoLocationOverlay?>(null) }
    val userCoords by LocationTracker.currentCoordinates.collectAsState()
    val liveAddress by LocationTracker.currentAddress.collectAsState()

    // Helper to generate a small light-blue map style location pin
    val lightBlueLocationIcon = remember(context) {
        val density = context.resources.displayMetrics.density
        val widthPx = (20 * density).toInt()
        val heightPx = (28 * density).toInt()
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val centerX = widthPx / 2f
        val headRadius = widthPx / 2f - (1.5f * density)

        // Draw pin head & needle path
        val path = android.graphics.Path().apply {
            moveTo(centerX, heightPx.toFloat())
            // Left curve to head
            quadTo(centerX - headRadius, heightPx * 0.55f, centerX - headRadius, headRadius + 1.5f * density)
            // Head circle top
            arcTo(
                android.graphics.RectF(
                    centerX - headRadius,
                    1.5f * density,
                    centerX + headRadius,
                    headRadius * 2 + 1.5f * density
                ),
                180f,
                180f,
                false
            )
            // Right curve to needle tip
            quadTo(centerX + headRadius, heightPx * 0.55f, centerX, heightPx.toFloat())
            close()
        }

        // Drop shadow / stroke
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * density
        paint.color = android.graphics.Color.WHITE
        canvas.drawPath(path, paint)

        // Light blue fill (#38BDF8 / sky blue)
        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.parseColor("#38BDF8")
        canvas.drawPath(path, paint)

        // Small white dot inside the head
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(centerX, headRadius + 1.5f * density, headRadius * 0.38f, paint)

        BitmapDrawable(context.resources, bitmap)
    }

    // Custom vehicle icon (small green circular pin with dark border)
    val vehicleIcon = remember(context) {
        val density = context.resources.displayMetrics.density
        val sizePx = (16 * density).toInt()
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val center = sizePx / 2f
        // White border
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(center, center, center, paint)
        // Green inner circle
        paint.color = android.graphics.Color.parseColor("#10B981")
        canvas.drawCircle(center, center, center - 1.5f * density, paint)
        // Dark center dot
        paint.color = android.graphics.Color.parseColor("#065F46")
        canvas.drawCircle(center, center, center * 0.4f, paint)

        BitmapDrawable(context.resources, bitmap)
    }

    DisposableEffect(Unit) {
        onDispose {
            locationOverlay?.stop()
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
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .clipToBounds(),
            factory = { ctx ->
                try {
                    Configuration.getInstance().userAgentValue = ctx.packageName
                    Configuration.getInstance().cacheMapTileCount = 16
                    Configuration.getInstance().tileFileSystemCacheMaxBytes = 150L * 1024 * 1024
                    Configuration.getInstance().tileFileSystemCacheTrimBytes = 120L * 1024 * 1024
                    MapView(ctx).apply {
                        setTileSource(MapStyles.RAPIDO_CLEAN)
                        MapStyles.applyRapidoTone(this)
                        zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER) // Remove visible + and - zoom buttons
                        setMultiTouchControls(isInteractive) // Keep pinch-to-zoom enabled
                        isTilesScaledToDpi = true // Set to true to prevent tiny unreadable street text on high-density screens
                        minZoomLevel = 4.0
                        maxZoomLevel = 20.0
                        setDestroyMode(false)

                        if (!isInteractive) {
                            setOnTouchListener { _, _ -> true } // Block map touch when on search screen
                        }

                        if (isBlurred) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                val blurRadiusPx = 32f * ctx.resources.displayMetrics.density
                                setRenderEffect(
                                    android.graphics.RenderEffect.createBlurEffect(
                                        blurRadiusPx,
                                        blurRadiusPx,
                                        android.graphics.Shader.TileMode.CLAMP
                                    )
                                )
                            }
                        }

                        val startLat = userCoords?.first ?: 28.6139
                        val startLng = userCoords?.second ?: 77.2090
                        controller.setZoom(if (isBlurred) 14.5 else 15.5)
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

                    // User live location beacon (Rapido / Uber style 60 FPS animated pulsing puck)
                    userCoords?.let { (uLat, uLng) ->
                        val geoPoint = GeoPoint(uLat, uLng)
                        val overlay = locationOverlay ?: RapidoLocationOverlay(view, geoPoint).also {
                            locationOverlay = it
                        }
                        overlay.position = geoPoint
                        view.overlays.add(overlay)

                        if (vehicles.isEmpty()) {
                            view.controller.setCenter(geoPoint)
                        }
                    }

                    // Vehicles markers with smooth heading rotation
                    vehicles.forEach { vehicle ->
                        val marker = Marker(view).apply {
                            position = GeoPoint(vehicle.currentLat, vehicle.currentLng)
                            rotation = vehicle.headingDegrees
                            title = "${vehicle.driver.fullName} (${vehicle.licensePlateMasked})"
                            subDescription = "${vehicle.seatAvailability.availableSeats} seats free"
                            icon = vehicleIcon
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
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
