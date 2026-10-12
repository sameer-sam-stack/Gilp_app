package com.glip.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Real OpenStreetMap (OSM) Map component via osmdroid.
 * 100% Royalty-Free and Open-Source under ODbL license.
 * Loads real geographic street tiles and markers without any proprietary billing accounts.
 */
@Composable
fun InteractiveDemoMap(
    vehicles: List<Vehicle>,
    selectedVehicle: Vehicle?,
    onVehicleSelect: (Vehicle) -> Unit,
    onRecenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

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
                        setMultiTouchControls(true)
                        isTilesScaledToDpi = true
                        controller.setZoom(13.0)
                        controller.setCenter(GeoPoint(28.6139, 77.2090)) // Connaught Place / Central Delhi
                        mapViewRef = this
                    }
                } catch (e: Exception) {
                    android.view.View(ctx)
                }
            },
            update = { view ->
                if (view is MapView) {
                    view.overlays.clear()
                    vehicles.forEach { vehicle ->
                        val marker = Marker(view).apply {
                            position = GeoPoint(vehicle.currentLat, vehicle.currentLng)
                            title = "${vehicle.driver.fullName} (${vehicle.licensePlateMasked})"
                            subDescription = "${vehicle.seatAvailability.availableSeats} seats free"
                            setOnMarkerClickListener { _, _ ->
                                onVehicleSelect(vehicle)
                                true
                            }
                        }
                        view.overlays.add(marker)
                    }
                    view.invalidate()
                }
            }
        )

        // Rapido-style Status Badge
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.95f),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(GlipGreenPrimary) // Rapido signature yellow
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "OpenStreetMap • Delhi NCR",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // OpenStreetMap Legal Attribution (ODbL Compliance - 100% Free & Legal)
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

        // Bottom-Right Recenter Button
        SmallFloatingActionButton(
            onClick = {
                mapViewRef?.controller?.setZoom(13.0)
                mapViewRef?.controller?.setCenter(GeoPoint(28.6139, 77.2090))
                onRecenter()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            containerColor = Color.White,
            contentColor = Color(0xFF18181B),
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Recenter Map",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
