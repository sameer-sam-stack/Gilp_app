package com.glip.app.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.ui.draw.blur
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
import com.glip.app.domain.model.VehicleCategory
import com.glip.app.ui.theme.GlipGreenPrimary
import com.glip.app.ui.map.MapStyles
import com.glip.app.ui.map.GlipLocationOverlay
import com.glip.app.util.LocationTracker
import com.glip.app.util.OsrmRoutingHelper
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
    rideBookingStatus: String = "NONE",
    pickupGeoPoint: GeoPoint? = null,
    dropGeoPoint: GeoPoint? = null,
    pickupLocation: String = "",
    dropLocation: String = "",
    onEditPickup: () -> Unit = {},
    onEditDrop: () -> Unit = {},
    isInteractive: Boolean = true,
    isBlurred: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var locationOverlay by remember { mutableStateOf<GlipLocationOverlay?>(null) }
    val userCoords by LocationTracker.currentCoordinates.collectAsState()
    var lastZoomedBookingStatus by remember { mutableStateOf("NONE") }
    var lastFramedTripKey by remember { mutableStateOf<String?>(null) }
    var liveRoadRoutePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }

    // Fetch and snap turn-by-turn road route following real streets, turns and galis via OSRM
    LaunchedEffect(pickupGeoPoint, dropGeoPoint, passengerRoutePoints) {
        if (passengerRoutePoints.size > 2) {
            liveRoadRoutePoints = passengerRoutePoints
            mapViewRef?.invalidate()
        } else if (pickupGeoPoint != null && dropGeoPoint != null) {
            val p = pickupGeoPoint
            val d = dropGeoPoint
            val roadResult = OsrmRoutingHelper.fetchRoadRoute(
                originLat = p.latitude,
                originLng = p.longitude,
                destLat = d.latitude,
                destLng = d.longitude
            )
            if (roadResult != null && roadResult.points.size > 1) {
                liveRoadRoutePoints = roadResult.points
                mapViewRef?.invalidate()
            } else if (passengerRoutePoints.isNotEmpty()) {
                liveRoadRoutePoints = passengerRoutePoints
            }
        } else {
            liveRoadRoutePoints = passengerRoutePoints
        }
    }

    // Auto-frame Pickup & Drop pins when trip route is loaded, so both points are visible to user
    LaunchedEffect(pickupGeoPoint, dropGeoPoint, mapViewRef) {
        val map = mapViewRef ?: return@LaunchedEffect
        val p = pickupGeoPoint
        val d = dropGeoPoint
        if (p != null && d != null) {
            val tripKey = "${p.latitude},${p.longitude}->${d.latitude},${d.longitude}"
            if (lastFramedTripKey != tripKey) {
                lastFramedTripKey = tripKey
                val midLat = (p.latitude + d.latitude) / 2.0
                val midLng = (p.longitude + d.longitude) / 2.0
                val results = FloatArray(1)
                android.location.Location.distanceBetween(p.latitude, p.longitude, d.latitude, d.longitude, results)
                val distMeters = results[0]
                val optimalZoom = when {
                    distMeters <= 800f -> 15.5
                    distMeters <= 2000f -> 14.5
                    distMeters <= 4000f -> 13.8
                    distMeters <= 8000f -> 13.0
                    distMeters <= 15000f -> 12.0
                    distMeters <= 30000f -> 11.0
                    else -> 10.0
                }
                map.controller.setZoom(optimalZoom)
                map.controller.animateTo(GeoPoint(midLat, midLng))
            }
        } else if (p == null && d == null) {
            lastFramedTripKey = null
        }
    }

    // Smooth camera zoom and focus when ride is booked or accepted
    LaunchedEffect(rideBookingStatus, isRideAccepted, mapViewRef, selectedVehicle) {
        val map = mapViewRef ?: return@LaunchedEffect
        val isBookedOrAccepted = rideBookingStatus == "REQUESTED" || rideBookingStatus == "ACCEPTED" || rideBookingStatus == "IN_PROGRESS"

        if (isBookedOrAccepted && lastZoomedBookingStatus != rideBookingStatus) {
            lastZoomedBookingStatus = rideBookingStatus
            val targetVehicle = selectedVehicle ?: vehicles.firstOrNull()
            val pGeo = if (rideBookingStatus == "IN_PROGRESS") dropGeoPoint else pickupGeoPoint
            val vGeo = targetVehicle?.let { GeoPoint(it.currentLat, it.currentLng) }

            if (pGeo != null && vGeo != null) {
                // Focus on mid-point between pickup/drop and incoming/trip vehicle
                val midLat = (pGeo.latitude + vGeo.latitude) / 2.0
                val midLng = (pGeo.longitude + vGeo.longitude) / 2.0
                map.controller.setZoom(16.5)
                map.controller.animateTo(GeoPoint(midLat, midLng))
            } else if (pGeo != null) {
                map.controller.setZoom(17.0)
                map.controller.animateTo(pGeo)
            } else if (vGeo != null) {
                map.controller.setZoom(17.0)
                map.controller.animateTo(vGeo)
            } else {
                map.controller.setZoom(16.5)
            }
        } else if (!isBookedOrAccepted && lastZoomedBookingStatus != "NONE") {
            lastZoomedBookingStatus = "NONE"
            map.controller.setZoom(15.0)
            userCoords?.let { (uLat, uLng) ->
                map.controller.animateTo(GeoPoint(uLat, uLng))
            }
        }
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
            .then(
                if (isBlurred) Modifier else Modifier.clip(RoundedCornerShape(16.dp))
            )
            .background(Color(0xFFE2E8F0))
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isBlurred) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur(24.dp)
                        } else {
                            Modifier
                        }
                    } else {
                        Modifier.clip(RoundedCornerShape(16.dp))
                    }
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
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                try {
                                    setRenderEffect(android.graphics.RenderEffect.createBlurEffect(32f, 32f, android.graphics.Shader.TileMode.CLAMP))
                                } catch (_: Exception) {}
                            }
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
                    }

                    // 2. Render Polylines: Only show passenger's own pickup-to-drop road polyline!
                    val pointsToDraw = if (liveRoadRoutePoints.isNotEmpty()) liveRoadRoutePoints else passengerRoutePoints
                    if (pointsToDraw.isNotEmpty()) {
                        // Passenger's Chosen Trip Polyline: Dark casing line + Vibrant Emerald Core Line
                        val casingLine = Polyline(view).apply {
                            outlinePaint.color = android.graphics.Color.parseColor("#0F172A")
                            outlinePaint.strokeWidth = 12f
                            outlinePaint.strokeCap = Paint.Cap.ROUND
                            outlinePaint.strokeJoin = Paint.Join.ROUND
                            outlinePaint.isAntiAlias = true
                            setPoints(pointsToDraw)
                        }
                        view.overlays.add(casingLine)

                        val coreLine = Polyline(view).apply {
                            outlinePaint.color = android.graphics.Color.parseColor("#10B981")
                            outlinePaint.strokeWidth = 8f
                            outlinePaint.strokeCap = Paint.Cap.ROUND
                            outlinePaint.strokeJoin = Paint.Join.ROUND
                            outlinePaint.isAntiAlias = true
                            setPoints(pointsToDraw)
                        }
                        view.overlays.add(coreLine)

                        // Pickup Pin (Green) with Address Pill and Edit Pencil Icon
                        pickupGeoPoint?.let { pt ->
                            val pMarker = Marker(view).apply {
                                position = pt
                                icon = createAddressPinDrawable(context, pickupLocation, "#10B981", "Pickup Point")
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = "Pickup: ${pickupLocation.ifBlank { "Pickup Point" }}"
                                setOnMarkerClickListener { _, _ ->
                                    onEditPickup()
                                    true
                                }
                            }
                            view.overlays.add(pMarker)
                        }

                        // Dropoff Pin (Red) with Address Pill and Edit Pencil Icon
                        dropGeoPoint?.let { pt ->
                            val dMarker = Marker(view).apply {
                                position = pt
                                icon = createAddressPinDrawable(context, dropLocation, "#EF4444", "Drop Location")
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = "Drop: ${dropLocation.ifBlank { "Drop Location" }}"
                                setOnMarkerClickListener { _, _ ->
                                    onEditDrop()
                                    true
                                }
                            }
                            view.overlays.add(dMarker)
                        }
                    }

                    // 3. Render Vehicle Markers (Clean vehicle position without exposing driver personal details before acceptance)
                    val visibleVehicles = if (isRideAccepted) {
                        listOfNotNull(selectedVehicle ?: vehicles.firstOrNull())
                    } else {
                        vehicles
                    }

                    visibleVehicles.forEach { vehicle ->
                        val marker = Marker(view).apply {
                            position = GeoPoint(vehicle.currentLat, vehicle.currentLng)
                            rotation = vehicle.headingDegrees
                            title = if (isRideAccepted) {
                                "${vehicle.driver.fullName} (${vehicle.licensePlateMasked})"
                            } else {
                                "${vehicle.category.name.replace('_', ' ')} • ${vehicle.etaMinutes} min away"
                            }
                            snippet = if (isRideAccepted) {
                                "${vehicle.seatAvailability.availableSeats}/${vehicle.seatAvailability.totalSeats} seats free • ${vehicle.etaMinutes} min away"
                            } else {
                                "Available on route • Tap to request"
                            }
                            icon = createVehicleMarkerDrawable(context, vehicle.routeColorHex, vehicle.category)
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
                    mapViewRef?.controller?.setZoom(17.0)
                    mapViewRef?.controller?.animateTo(GeoPoint(targetLat, targetLng))
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

        // OpenStreetMap Legal Attribution (ODbL Compliance - Ultra-compact subtle in bottom-left corner)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 4.dp, bottom = 4.dp),
            shape = RoundedCornerShape(3.dp),
            color = Color.White.copy(alpha = 0.65f)
        ) {
            Text(
                text = "© OpenStreetMap",
                fontSize = 6.5.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
            )
        }
    }
}

private val markerDrawableCache = java.util.concurrent.ConcurrentHashMap<String, Drawable>()
private val pinDrawableCache = java.util.concurrent.ConcurrentHashMap<String, Drawable>()

/**
 * Programmatic realistic top-down vehicle silhouette rendered directly on Canvas.
 * Supports Bike, Auto Rickshaw, E-Rickshaw, and Car/Cab with realistic proportions,
 * chassis drop shadow, wheels, windshield, and head/tail lamps (no cartoon radar halos).
 */
fun createVehicleMarkerDrawable(
    context: Context,
    colorHex: String,
    category: VehicleCategory = VehicleCategory.AUTO_RICKSHAW
): Drawable {
    val cacheKey = "${colorHex}_${category.name}"
    markerDrawableCache[cacheKey]?.let { return it }

    val density = context.resources.displayMetrics.density
    val sizePx = (46 * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = sizePx / 2f
    val cy = sizePx / 2f

    val colorInt = try {
        android.graphics.Color.parseColor(colorHex)
    } catch (_: Exception) {
        android.graphics.Color.parseColor("#10B981")
    }

    when (category) {
        VehicleCategory.BIKE -> {
            // Realistic top-down Motorcycle/Scooter with Rider
            // 1. Soft subtle chassis shadow (no cartoon radar halos)
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x40000000
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(
                RectF(cx - 3.5f * density, cy - 13.0f * density, cx + 3.5f * density, cy + 14.0f * density),
                3.5f * density, 3.5f * density, shadowPaint
            )

            // Front Tire
            val tirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#1E293B")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(
                RectF(cx - 1.5f * density, cy - 14.0f * density, cx + 1.5f * density, cy - 8.0f * density),
                1.2f * density, 1.2f * density, tirePaint
            )

            // Rear Tire
            canvas.drawRoundRect(
                RectF(cx - 1.8f * density, cy + 7.0f * density, cx + 1.8f * density, cy + 13.0f * density),
                1.2f * density, 1.2f * density, tirePaint
            )

            // Handlebar & Mirrors
            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#334155")
                style = Paint.Style.STROKE
                strokeWidth = 2.0f * density
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawLine(cx - 7.0f * density, cy - 7.5f * density, cx + 7.0f * density, cy - 7.5f * density, barPaint)

            // Bike Body & Fuel Tank
            val bikeBody = android.graphics.Path().apply {
                moveTo(cx - 2.5f * density, cy - 8.0f * density)
                lineTo(cx + 2.5f * density, cy - 8.0f * density)
                lineTo(cx + 3.5f * density, cy + 7.0f * density)
                lineTo(cx - 3.5f * density, cy + 7.0f * density)
                close()
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorInt
                style = Paint.Style.FILL
            }
            canvas.drawPath(bikeBody, bodyPaint)

            // Rider Torso / Jacket
            val riderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#0F172A")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(
                RectF(cx - 4.5f * density, cy - 2.5f * density, cx + 4.5f * density, cy + 4.5f * density),
                2.5f * density, 2.5f * density, riderPaint
            )

            // Rider Helmet (top-down circle with visor)
            val helmetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx, cy - 1.0f * density, 3.2f * density, helmetPaint)

            val visorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#0F172A")
                style = Paint.Style.FILL
            }
            canvas.drawArc(
                RectF(cx - 3.2f * density, cy - 4.2f * density, cx + 3.2f * density, cy + 2.2f * density),
                200f, 140f, true, visorPaint
            )

            // Headlight
            val headlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#FEF08A")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx, cy - 12.5f * density, 1.4f * density, headlightPaint)

            // Taillight
            val taillightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#EF4444")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx, cy + 12.0f * density, 1.2f * density, taillightPaint)
        }

        VehicleCategory.AUTO_RICKSHAW, VehicleCategory.E_RICKSHAW -> {
            // Realistic 3-Wheeler Auto Rickshaw / E-Rickshaw
            // 1. Soft Drop Shadow
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x45000000
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(
                RectF(cx - 7.5f * density, cy - 9.5f * density, cx + 7.5f * density, cy + 12.5f * density),
                4.5f * density, 4.5f * density, shadowPaint
            )

            // 2. Wheels
            val wheelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#0F172A")
                style = Paint.Style.FILL
            }
            // Front wheel
            canvas.drawRoundRect(
                RectF(cx - 1.8f * density, cy - 13.5f * density, cx + 1.8f * density, cy - 8.5f * density),
                1.5f * density, 1.5f * density, wheelPaint
            )
            // Rear wheels
            canvas.drawRoundRect(
                RectF(cx - 8.8f * density, cy + 4.5f * density, cx - 6.0f * density, cy + 11.5f * density),
                1.5f * density, 1.5f * density, wheelPaint
            )
            canvas.drawRoundRect(
                RectF(cx + 6.0f * density, cy + 4.5f * density, cx + 8.8f * density, cy + 11.5f * density),
                1.5f * density, 1.5f * density, wheelPaint
            )

            // 3. Body Shell (Tapered 3-wheeler contour)
            val bodyPath = android.graphics.Path().apply {
                moveTo(cx - 3.5f * density, cy - 9.5f * density)
                quadTo(cx, cy - 12.5f * density, cx + 3.5f * density, cy - 9.5f * density)
                lineTo(cx + 7.0f * density, cy - 3.5f * density)
                lineTo(cx + 7.0f * density, cy + 9.5f * density)
                quadTo(cx + 7.0f * density, cy + 11.5f * density, cx + 5.0f * density, cy + 11.5f * density)
                lineTo(cx - 5.0f * density, cy + 11.5f * density)
                quadTo(cx - 7.0f * density, cy + 11.5f * density, cx - 7.0f * density, cy + 9.5f * density)
                lineTo(cx - 7.0f * density, cy - 3.5f * density)
                close()
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorInt
                style = Paint.Style.FILL
            }
            canvas.drawPath(bodyPath, bodyPaint)

            // White high-contrast outline
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 1.4f * density
            }
            canvas.drawPath(bodyPath, borderPaint)

            // Windshield
            val windshieldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#1E293B")
                style = Paint.Style.FILL
            }
            val windshieldPath = android.graphics.Path().apply {
                moveTo(cx - 4.2f * density, cy - 5.5f * density)
                quadTo(cx, cy - 8.2f * density, cx + 4.2f * density, cy - 5.5f * density)
                lineTo(cx + 3.8f * density, cy - 3.2f * density)
                quadTo(cx, cy - 5.2f * density, cx - 3.8f * density, cy - 3.2f * density)
                close()
            }
            canvas.drawPath(windshieldPath, windshieldPaint)

            // Cockpit & Dashboard
            val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#334155")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx, cy - 1.8f * density, 1.6f * density, dashPaint)

            // Rear passenger bench
            val seatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#0F172A")
                alpha = 180
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(
                RectF(cx - 5.2f * density, cy + 4.5f * density, cx + 5.2f * density, cy + 9.5f * density),
                2.0f * density, 2.0f * density, seatPaint
            )

            // Canopy rib line
            val ribPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                alpha = 80
                style = Paint.Style.STROKE
                strokeWidth = 0.9f * density
            }
            canvas.drawLine(cx - 4.5f * density, cy + 1.5f * density, cx + 4.5f * density, cy + 1.5f * density, ribPaint)

            // Tail lights
            val tailLightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#EF4444")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx - 5.0f * density, cy + 11.2f * density, 1.2f * density, tailLightPaint)
            canvas.drawCircle(cx + 5.0f * density, cy + 11.2f * density, 1.2f * density, tailLightPaint)

            // Headlight
            val headlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#FEF08A")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx, cy - 11.0f * density, 1.3f * density, headlightPaint)
        }

        else -> {
            // Realistic top-down Car (Cab / Sedan / SUV / Van)
            // 1. Soft Drop Shadow
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x45000000
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(
                RectF(cx - 7.5f * density, cy - 12.0f * density, cx + 7.5f * density, cy + 13.0f * density),
                4.5f * density, 4.5f * density, shadowPaint
            )

            // 2. Wheels
            val wheelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#0F172A")
                style = Paint.Style.FILL
            }
            // Front left & right wheels
            canvas.drawRoundRect(
                RectF(cx - 8.2f * density, cy - 8.0f * density, cx - 6.0f * density, cy - 2.5f * density),
                1.5f * density, 1.5f * density, wheelPaint
            )
            canvas.drawRoundRect(
                RectF(cx + 6.0f * density, cy - 8.0f * density, cx + 8.2f * density, cy - 2.5f * density),
                1.5f * density, 1.5f * density, wheelPaint
            )
            // Rear left & right wheels
            canvas.drawRoundRect(
                RectF(cx - 8.2f * density, cy + 4.5f * density, cx - 6.0f * density, cy + 10.0f * density),
                1.5f * density, 1.5f * density, wheelPaint
            )
            canvas.drawRoundRect(
                RectF(cx + 6.0f * density, cy + 4.5f * density, cx + 8.2f * density, cy + 10.0f * density),
                1.5f * density, 1.5f * density, wheelPaint
            )

            // 3. Car Body Chassis
            val carBody = android.graphics.Path().apply {
                moveTo(cx - 5.5f * density, cy - 12.0f * density)
                quadTo(cx, cy - 13.5f * density, cx + 5.5f * density, cy - 12.0f * density)
                quadTo(cx + 7.2f * density, cy - 10.0f * density, cx + 7.2f * density, cy + 9.5f * density)
                quadTo(cx + 7.0f * density, cy + 12.5f * density, cx + 4.5f * density, cy + 12.5f * density)
                lineTo(cx - 4.5f * density, cy + 12.5f * density)
                quadTo(cx - 7.0f * density, cy + 12.5f * density, cx - 7.2f * density, cy + 9.5f * density)
                quadTo(cx - 7.2f * density, cy - 10.0f * density, cx - 5.5f * density, cy - 12.0f * density)
                close()
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorInt
                style = Paint.Style.FILL
            }
            canvas.drawPath(carBody, bodyPaint)

            // Crisp white outline
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 1.4f * density
            }
            canvas.drawPath(carBody, borderPaint)

            // Side Mirrors
            val mirrorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorInt
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx - 7.8f * density, cy - 5.0f * density, 1.2f * density, mirrorPaint)
            canvas.drawCircle(cx + 7.8f * density, cy - 5.0f * density, 1.2f * density, mirrorPaint)

            // Front Windshield (Dark Glass)
            val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#1E293B")
                style = Paint.Style.FILL
            }
            val frontWindshield = android.graphics.Path().apply {
                moveTo(cx - 5.0f * density, cy - 6.5f * density)
                quadTo(cx, cy - 8.5f * density, cx + 5.0f * density, cy - 6.5f * density)
                lineTo(cx + 4.5f * density, cy - 2.5f * density)
                quadTo(cx, cy - 4.0f * density, cx - 4.5f * density, cy - 2.5f * density)
                close()
            }
            canvas.drawPath(frontWindshield, glassPaint)

            // Car Roof (Slightly darker/lighter tone)
            val roofPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#334155")
                alpha = 80
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(
                RectF(cx - 4.5f * density, cy - 2.5f * density, cx + 4.5f * density, cy + 5.5f * density),
                1.5f * density, 1.5f * density, roofPaint
            )

            // Rear Windshield
            val rearWindshield = android.graphics.Path().apply {
                moveTo(cx - 4.5f * density, cy + 5.5f * density)
                lineTo(cx + 4.5f * density, cy + 5.5f * density)
                lineTo(cx + 4.8f * density, cy + 8.5f * density)
                lineTo(cx - 4.8f * density, cy + 8.5f * density)
                close()
            }
            canvas.drawPath(rearWindshield, glassPaint)

            // Headlights (Warm yellow)
            val headlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#FEF08A")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx - 4.5f * density, cy - 11.5f * density, 1.2f * density, headlightPaint)
            canvas.drawCircle(cx + 4.5f * density, cy - 11.5f * density, 1.2f * density, headlightPaint)

            // Tail lights (Vibrant red)
            val tailLightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#EF4444")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx - 4.8f * density, cy + 12.0f * density, 1.2f * density, tailLightPaint)
            canvas.drawCircle(cx + 4.8f * density, cy + 12.0f * density, 1.2f * density, tailLightPaint)
        }
    }

    val drawable = BitmapDrawable(context.resources, bitmap)
    markerDrawableCache[cacheKey] = drawable
    return drawable
}

/**
 * Origin (Green) or Destination (Red) Pin with floating Address Bubble and Edit Pencil Icon.
 * Matches Screenshot 1:
 * - Top floating white pill with address text and pencil edit icon
 * - Below pill: Green/Red circular bullseye ring (with white center)
 * - Below ring: Vertical stem anchoring to the exact map point
 */
fun createAddressPinDrawable(
    context: Context,
    address: String,
    pinColorHex: String,
    defaultLabel: String
): Drawable {
    val cleanAddress = address.trim().ifEmpty { defaultLabel }
    val cacheKey = "${cleanAddress}_$pinColorHex"
    pinDrawableCache[cacheKey]?.let { return it }

    val density = context.resources.displayMetrics.density

    // Smartly format address like in Screenshot 1 ("208, Sankar ..." or "MGM School...")
    val firstPart = cleanAddress.split(",").firstOrNull()?.trim() ?: cleanAddress
    val displayAddress = if (cleanAddress.length <= 15) {
        cleanAddress
    } else if (firstPart.length in 4..16) {
        "$firstPart..."
    } else {
        cleanAddress.take(14).trimEnd() + "..."
    }

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0F172A") // Deep Slate text
        textSize = 12.5f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val textWidth = textPaint.measureText(displayAddress)

    // Pill geometry
    val padHorizontal = 10f * density
    val pencilBtnRadius = 10.5f * density
    val gapTextIcon = 7f * density
    val bubbleWidth = (padHorizontal * 2f + textWidth + gapTextIcon + pencilBtnRadius * 2f).coerceAtLeast(80f * density)
    val bubbleHeight = 32f * density
    val bubbleCornerRadius = 12f * density

    // Pin geometry below bubble
    val gapBubbleRing = 4f * density
    val ringOuterRadius = 9f * density
    val ringHoleRadius = 4f * density
    val stemHeight = 6.5f * density
    val stemWidth = 2.4f * density

    val totalWidth = (bubbleWidth + 16f * density).toInt()
    val totalHeight = (2f * density + bubbleHeight + gapBubbleRing + (ringOuterRadius * 2f) + stemHeight + 4f * density).toInt()

    val bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = totalWidth / 2f

    // 1. Drop shadow for floating address bubble
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#22000000") // 13% black
        style = Paint.Style.FILL
    }
    val bubbleRect = RectF(
        cx - (bubbleWidth / 2f),
        2f * density,
        cx + (bubbleWidth / 2f),
        2f * density + bubbleHeight
    )
    val shadowRect = RectF(
        bubbleRect.left,
        bubbleRect.top + (1.5f * density),
        bubbleRect.right,
        bubbleRect.bottom + (2.5f * density)
    )
    canvas.drawRoundRect(shadowRect, bubbleCornerRadius, bubbleCornerRadius, shadowPaint)

    // 2. Pure White Bubble Background
    val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(bubbleRect, bubbleCornerRadius, bubbleCornerRadius, bubblePaint)

    // 3. Subtle Border for Bubble
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#E2E8F0")
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }
    canvas.drawRoundRect(bubbleRect, bubbleCornerRadius, bubbleCornerRadius, borderPaint)

    // 4. Address Text
    val textX = bubbleRect.left + padHorizontal
    val textY = bubbleRect.centerY() - ((textPaint.descent() + textPaint.ascent()) / 2f)
    canvas.drawText(displayAddress, textX, textY, textPaint)

    // 5. Circular Pencil Button Background
    val pencilBtnCx = bubbleRect.right - padHorizontal / 1.5f - pencilBtnRadius
    val pencilBtnCy = bubbleRect.centerY()
    val pencilBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#F1F5F9") // Soft slate background
        style = Paint.Style.FILL
    }
    canvas.drawCircle(pencilBtnCx, pencilBtnCy, pencilBtnRadius, pencilBgPaint)

    // 6. Tilted Pencil Icon inside button
    val pencilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#475569") // Slate-600
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
    }
    canvas.save()
    canvas.translate(pencilBtnCx, pencilBtnCy)
    canvas.rotate(-45f)
    val pw = 1.8f * density
    val ph = 4.2f * density
    val pencilBody = RectF(-pw, -ph + (1.8f * density), pw, ph)
    canvas.drawRoundRect(pencilBody, 0.7f * density, 0.7f * density, pencilPaint)

    // Pencil tip
    val tipPath = android.graphics.Path().apply {
        moveTo(-pw, -ph + (1.8f * density))
        lineTo(pw, -ph + (1.8f * density))
        lineTo(0f, -ph - (1.5f * density))
        close()
    }
    canvas.drawPath(tipPath, pencilPaint)

    // Pencil top cap
    val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#94A3B8")
        style = Paint.Style.FILL
    }
    val capRect = RectF(-pw, ph - (1.2f * density), pw, ph + (0.8f * density))
    canvas.drawRoundRect(capRect, 0.8f * density, 0.8f * density, capPaint)
    canvas.restore()

    // 7. Ground shadow at the base of the stem
    val ringCy = bubbleRect.bottom + gapBubbleRing + ringOuterRadius
    val stemTopY = ringCy + ringOuterRadius
    val stemBottomY = stemTopY + stemHeight

    val groundShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#33000000")
        style = Paint.Style.FILL
    }
    canvas.drawOval(
        RectF(cx - (4f * density), stemBottomY - (1.5f * density), cx + (4f * density), stemBottomY + (1.5f * density)),
        groundShadowPaint
    )

    // 8. Dark Vertical Stem
    val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0F172A")
        style = Paint.Style.STROKE
        strokeWidth = stemWidth
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(cx, stemTopY - (1f * density), cx, stemBottomY, stemPaint)

    // 9. Outer Color Ring (Green or Red)
    val pinColor = try {
        android.graphics.Color.parseColor(pinColorHex)
    } catch (_: Exception) {
        android.graphics.Color.parseColor("#10B981")
    }
    val ringOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pinColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, ringCy, ringOuterRadius, ringOuterPaint)

    // 10. Inner White Hole
    val ringHolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, ringCy, ringHoleRadius, ringHolePaint)

    val drawable = BitmapDrawable(context.resources, bitmap)
    pinDrawableCache[cacheKey] = drawable
    return drawable
}
