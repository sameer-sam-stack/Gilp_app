package com.glip.app.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
import com.glip.app.ui.map.GlipLocationOverlay
import com.glip.app.util.LocationTracker
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * Real OpenStreetMap (OSM) Map component via osmdroid.
 * 100% Royalty-Free and Open-Source under ODbL license.
 * Supports multi-vehicle light-color route polylines and smooth real-time tracking.
 */
@Composable
fun InteractiveDemoMap(
    vehicles: List<Vehicle>,
    selectedVehicle: Vehicle?,
    onVehicleSelect: (Vehicle) -> Unit,
    onRecenter: () -> Unit,
    passengerRoutePoints: List<GeoPoint> = emptyList(),
    isRideAccepted: Boolean = false,
    pickupGeoPoint: GeoPoint? = null,
    dropGeoPoint: GeoPoint? = null,
    isInteractive: Boolean = true,
    isBlurred: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var locationOverlay by remember { mutableStateOf<GlipLocationOverlay?>(null) }
    val userCoords by LocationTracker.currentCoordinates.collectAsState()

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
            .then(
                if (isBlurred) Modifier else Modifier.clip(RoundedCornerShape(16.dp))
            )
            .background(Color(0xFFE2E8F0))
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isBlurred) Modifier else Modifier.clip(RoundedCornerShape(16.dp))
                )
                .clipToBounds(),
            factory = { ctx ->
                try {
                    Configuration.getInstance().userAgentValue = ctx.packageName
                    Configuration.getInstance().cacheMapTileCount = 32
                    Configuration.getInstance().tileFileSystemCacheMaxBytes = 150L * 1024 * 1024
                    Configuration.getInstance().tileFileSystemCacheTrimBytes = 120L * 1024 * 1024
                    MapView(ctx).apply {
                        setTileSource(MapStyles.GLIP_CLEAN)
                        if (isBlurred) {
                            MapStyles.applyBlurredTone(this)
                        } else {
                            MapStyles.applyGlipTone(this)
                        }
                        zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                        setMultiTouchControls(isInteractive)
                        isTilesScaledToDpi = true
                        minZoomLevel = 4.0
                        maxZoomLevel = 20.0
                        setDestroyMode(false)

                        if (!isInteractive) {
                            setOnTouchListener { _, _ -> true }
                        }

                        val startLat = userCoords?.first ?: 28.6139
                        val startLng = userCoords?.second ?: 77.2090
                        controller.setZoom(if (isBlurred) 13.5 else 15.5)
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

                    // 1. User live location beacon
                    userCoords?.let { (uLat, uLng) ->
                        val geoPoint = GeoPoint(uLat, uLng)
                        val overlay = locationOverlay ?: GlipLocationOverlay(view, geoPoint).also {
                            locationOverlay = it
                        }
                        overlay.position = geoPoint
                        view.overlays.add(overlay)

                        if (vehicles.isEmpty() && !isRideAccepted) {
                            view.controller.setCenter(geoPoint)
                        }
                    }

                    // 2. Render Polylines
                    if (isRideAccepted && passengerRoutePoints.isNotEmpty()) {
                        // Accepted Ride: Dark casing line + Vibrant Emerald Core Line
                        val casingLine = Polyline(view).apply {
                            outlinePaint.color = android.graphics.Color.parseColor("#0F172A")
                            outlinePaint.strokeWidth = 12f
                            outlinePaint.strokeCap = Paint.Cap.ROUND
                            outlinePaint.strokeJoin = Paint.Join.ROUND
                            outlinePaint.isAntiAlias = true
                            setPoints(passengerRoutePoints)
                        }
                        view.overlays.add(casingLine)

                        val coreLine = Polyline(view).apply {
                            outlinePaint.color = android.graphics.Color.parseColor("#10B981")
                            outlinePaint.strokeWidth = 8f
                            outlinePaint.strokeCap = Paint.Cap.ROUND
                            outlinePaint.strokeJoin = Paint.Join.ROUND
                            outlinePaint.isAntiAlias = true
                            setPoints(passengerRoutePoints)
                        }
                        view.overlays.add(coreLine)

                        // Pickup Pin 'A' (Green)
                        pickupGeoPoint?.let { pt ->
                            val pMarker = Marker(view).apply {
                                position = pt
                                icon = createPinDrawable(context, "A", "#10B981")
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = "Your Pickup Point"
                            }
                            view.overlays.add(pMarker)
                        }

                        // Dropoff Pin 'B' (Red)
                        dropGeoPoint?.let { pt ->
                            val dMarker = Marker(view).apply {
                                position = pt
                                icon = createPinDrawable(context, "B", "#EF4444")
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = "Your Destination"
                            }
                            view.overlays.add(dMarker)
                        }
                    } else {
                        // Pre-booking: Render each vehicle's corridor polyline in its distinct light color
                        vehicles.forEach { vehicle ->
                            val linePoints = if (vehicle.roadPolylinePoints.isNotEmpty()) {
                                vehicle.roadPolylinePoints.map { GeoPoint(it.first, it.second) }
                            } else if (vehicle.route.destinationLat != 0.0) {
                                listOf(
                                    GeoPoint(vehicle.currentLat, vehicle.currentLng),
                                    GeoPoint(vehicle.route.destinationLat, vehicle.route.destinationLng)
                                )
                            } else {
                                emptyList()
                            }

                            if (linePoints.isNotEmpty()) {
                                val colorInt = try {
                                    android.graphics.Color.parseColor(vehicle.routeColorHex)
                                } catch (_: Exception) {
                                    android.graphics.Color.parseColor("#10B981")
                                }

                                val isSel = vehicle.vehicleId == (selectedVehicle?.vehicleId ?: vehicles.firstOrNull()?.vehicleId)

                                val polyline = Polyline(view).apply {
                                    outlinePaint.color = colorInt
                                    outlinePaint.alpha = if (isSel) 240 else 185
                                    outlinePaint.strokeWidth = if (isSel) 9f else 6.5f
                                    outlinePaint.strokeCap = Paint.Cap.ROUND
                                    outlinePaint.strokeJoin = Paint.Join.ROUND
                                    outlinePaint.isAntiAlias = true
                                    setPoints(linePoints)
                                }
                                view.overlays.add(polyline)
                            }
                        }
                    }

                    // 3. Render Vehicle Markers
                    val visibleVehicles = if (isRideAccepted) {
                        listOfNotNull(selectedVehicle ?: vehicles.firstOrNull())
                    } else {
                        vehicles
                    }

                    visibleVehicles.forEach { vehicle ->
                        val marker = Marker(view).apply {
                            position = GeoPoint(vehicle.currentLat, vehicle.currentLng)
                            rotation = vehicle.headingDegrees
                            title = "${vehicle.driver.fullName} (${vehicle.licensePlateMasked})"
                            snippet = "${vehicle.seatAvailability.availableSeats}/${vehicle.seatAvailability.totalSeats} seats free • ${vehicle.etaMinutes} min away"
                            icon = createVehicleMarkerDrawable(context, vehicle.routeColorHex)
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

/**
 * Programmatic sharp vehicle marker with color-coded pulsing radar halo and forward heading indicator.
 */
fun createVehicleMarkerDrawable(context: Context, colorHex: String): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (36 * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = sizePx / 2f
    val cy = sizePx / 2f

    val colorInt = try {
        android.graphics.Color.parseColor(colorHex)
    } catch (_: Exception) {
        android.graphics.Color.parseColor("#10B981")
    }

    // 1. Soft pulsing radar aura in vehicle color
    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorInt
        alpha = 65
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 16 * density, haloPaint)

    // 2. White outer circle
    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 12 * density, whitePaint)

    // 3. Core circle in vehicle color
    val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorInt
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 9.5f * density, corePaint)

    // 4. White forward directional arrow
    val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    val path = android.graphics.Path().apply {
        moveTo(cx, cy - (5.5f * density))
        lineTo(cx + (4f * density), cy + (4f * density))
        lineTo(cx, cy + (2.5f * density))
        lineTo(cx - (4f * density), cy + (4f * density))
        close()
    }
    canvas.drawPath(path, arrowPaint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Origin ('A' - Green) or Destination ('B' - Red) Pin.
 */
fun createPinDrawable(context: Context, label: String, pinColorHex: String): Drawable {
    val density = context.resources.displayMetrics.density
    val w = (30 * density).toInt()
    val h = (40 * density).toInt()
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = w / 2f
    val headRadius = 11 * density

    val colorInt = android.graphics.Color.parseColor(pinColorHex)
    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorInt
        style = Paint.Style.FILL
    }
    val path = android.graphics.Path().apply {
        arcTo(cx - headRadius, 2 * density, cx + headRadius, 2 * density + (headRadius * 2), 180f, 180f, true)
        lineTo(cx, h - (2 * density))
        close()
    }
    canvas.drawPath(path, pinPaint)

    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, 2 * density + headRadius, 8 * density, whitePaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorInt
        textSize = 10 * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val textY = 2 * density + headRadius - ((textPaint.descent() + textPaint.ascent()) / 2)
    canvas.drawText(label, cx, textY, textPaint)

    return BitmapDrawable(context.resources, bitmap)
}
