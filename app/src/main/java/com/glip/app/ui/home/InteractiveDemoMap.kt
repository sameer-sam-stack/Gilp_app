package com.glip.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.app.domain.model.Vehicle
import com.glip.app.ui.theme.GlipGreenDark
import com.glip.app.ui.theme.GlipGreenPrimary
import com.glip.app.ui.theme.GlipOrangeAccent

/**
 * Interactive Vector Map Canvas for Glip Demo MVP.
 * Renders simulated geographic transit routes, stops, and moving vehicles
 * with pan, zoom, tap detection, and smooth responsive layout.
 * Runs completely offline, requires zero proprietary API keys,
 * and will never crash due to map tile network failures.
 */
@Composable
fun InteractiveDemoMap(
    vehicles: List<Vehicle>,
    selectedVehicle: Vehicle?,
    onVehicleSelect: (Vehicle) -> Unit,
    onRecenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Zoom and pan offsets
    var scale by remember { mutableFloatStateOf(1.0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Map bounds around Delhi NCR
    val minLat = 28.45
    val maxLat = 28.75
    val minLng = 77.05
    val maxLng = 77.40

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFEBF2EA)) // Natural cartographic tint
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.7f, 3.0f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
    ) {
        // Map Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            fun latLngToScreen(lat: Double, lng: Double): Offset {
                val normX = ((lng - minLng) / (maxLng - minLng)).toFloat()
                // Invert Y because latitude grows northwards
                val normY = (1f - ((lat - minLat) / (maxLat - minLat))).toFloat()

                val cx = width / 2f
                val cy = height / 2f

                val sx = cx + (normX * width - cx) * scale + offsetX
                val sy = cy + (normY * height - cy) * scale + offsetY
                return Offset(sx, sy)
            }

            // Draw subtle city grid lines
            val gridStep = 60f * scale
            val gridColor = Color(0xFFDCE6DA)
            var gx = (offsetX % gridStep)
            while (gx < width) {
                drawLine(gridColor, Offset(gx, 0f), Offset(gx, height), strokeWidth = 1f)
                gx += gridStep
            }
            var gy = (offsetY % gridStep)
            while (gy < height) {
                drawLine(gridColor, Offset(0f, gy), Offset(width, gy), strokeWidth = 1f)
                gy += gridStep
            }

            // Draw routes polyline
            for (vehicle in vehicles) {
                val isSelected = vehicle.vehicleId == selectedVehicle?.vehicleId
                val route = vehicle.route
                val polyPoints = route.polylinePoints

                if (polyPoints.size > 1) {
                    for (i in 0 until polyPoints.size - 1) {
                        val p1 = latLngToScreen(polyPoints[i].first, polyPoints[i].second)
                        val p2 = latLngToScreen(polyPoints[i + 1].first, polyPoints[i + 1].second)

                        // Route line
                        drawLine(
                            color = if (isSelected) GlipOrangeAccent else GlipGreenDark.copy(alpha = 0.6f),
                            start = p1,
                            end = p2,
                            strokeWidth = if (isSelected) 6f * scale else 3.5f * scale,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 8f), 0f)
                        )
                    }
                }

                // Intermediate stops
                for (stop in route.stops) {
                    val stopPos = latLngToScreen(stop.latitude, stop.longitude)
                    drawCircle(
                        color = Color.White,
                        radius = 4.5f * scale,
                        center = stopPos
                    )
                    drawCircle(
                        color = GlipGreenDark,
                        radius = 3f * scale,
                        center = stopPos
                    )
                }

                // Origin and destination points
                val originPos = latLngToScreen(route.originLat, route.originLng)
                val destPos = latLngToScreen(route.destinationLat, route.destinationLng)

                drawCircle(color = GlipGreenPrimary, radius = 5.5f * scale, center = originPos)
                drawCircle(color = GlipOrangeAccent, radius = 5.5f * scale, center = destPos)
            }

            // Draw simulated vehicle markers
            for (vehicle in vehicles) {
                val isSelected = vehicle.vehicleId == selectedVehicle?.vehicleId
                val vPos = latLngToScreen(vehicle.currentLat, vehicle.currentLng)

                // Marker outer halo
                drawCircle(
                    color = if (isSelected) GlipOrangeAccent.copy(alpha = 0.35f) else GlipGreenPrimary.copy(alpha = 0.25f),
                    radius = if (isSelected) 18f * scale else 13f * scale,
                    center = vPos
                )

                // Marker background
                drawCircle(
                    color = if (isSelected) GlipOrangeAccent else GlipGreenPrimary,
                    radius = if (isSelected) 10f * scale else 8f * scale,
                    center = vPos
                )

                // White inner core
                drawCircle(
                    color = Color.White,
                    radius = if (isSelected) 4.5f * scale else 3.5f * scale,
                    center = vPos
                )
            }
        }

        // Top-left Map status pill
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.92f),
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
                        .background(GlipGreenPrimary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Delhi NCR • ${vehicles.size} Live Corridors",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Bottom-Right Controls: Recenter and Vehicle Selector Pills
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            SmallFloatingActionButton(
                onClick = {
                    scale = 1.0f
                    offsetX = 0f
                    offsetY = 0f
                    onRecenter()
                },
                containerColor = Color.White,
                contentColor = GlipGreenPrimary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Recenter Map",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Top-left Map status pill
    }
}
