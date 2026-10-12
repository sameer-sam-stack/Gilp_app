package com.glip.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.app.ui.theme.GlipGreenPrimary
import com.glip.app.util.LocationTracker

import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.ui.zIndex

/**
 * RideSearchScreen:
 * Landing screen when app opens.
 * 1. Top: Empty Box for future Ad
 * 2. Center: Pickup and Drop address inputs (with map picker buttons) + "Ride ->" button
 * 3. Bottom: Empty Box for future Ad
 * 4. Background: Real OSM street map (Non-interactive, touch blocked)
 * 5. GPS auto-detects user location and pre-fills into Pickup box
 * 6. Map picker modal with fixed center pin for exact location selection
 */
@Composable
fun RideSearchScreen(
    initialPickup: String = "",
    initialDrop: String = "",
    onConfirmRideSearch: (pickup: String, drop: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val liveAddress by LocationTracker.currentAddress.collectAsState()
    val liveCoords by LocationTracker.currentCoordinates.collectAsState()

    var pickup by remember { mutableStateOf(initialPickup) }
    var drop by remember { mutableStateOf(initialDrop) }

    // Coordinates state
    var pickupCoordinates by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var dropCoordinates by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    // Map selection modal state: null = closed, "pickup" = selecting pickup, "drop" = selecting drop
    var selectingLocationMode by remember { mutableStateOf<String?>(null) }

    // Auto-fill pickup with live GPS location as soon as detected
    LaunchedEffect(liveAddress, liveCoords) {
        if (!liveAddress.isNullOrBlank() && pickup.isBlank()) {
            pickup = liveAddress ?: ""
            pickupCoordinates = liveCoords
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // 1. Real OpenStreetMap background (Blur effect + Touch blocked)
        InteractiveDemoMap(
            vehicles = emptyList(),
            selectedVehicle = null,
            onVehicleSelect = {},
            onRecenter = {},
            isInteractive = false,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 12.dp)
        )

        // Frosted Blur overlay Scrim (Semi-transparent so map streets & shapes are visible through frosted glass without being readable)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A).copy(alpha = 0.40f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    // Consumes touches so user cannot click or drag through to background map
                }
        )

        // Main UI Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP: Empty Box for Ad (Height 270dp and padding adjusted to 75dp to shift closer down)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 75.dp)
                    .height(270.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
            ) {
                // Empty container ready for Ad
                Box(modifier = Modifier.fillMaxSize())
            }

            // CENTER: Floating Pickup & Drop Card + Ride Button
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Labels: Pickup (Green bullet) & Drop (Red bullet)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pickup",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Drop",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row: Pickup Input Box -> Arrow -> Drop Input Box
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pickup Input Box with Map Icon Button on right
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 10.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    BasicTextField(
                                        value = pickup,
                                        onValueChange = { pickup = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF0F172A)
                                        ),
                                        decorationBox = { innerTextField ->
                                            if (pickup.isEmpty()) {
                                                Text(
                                                    text = "Pickup address",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF94A3B8)
                                                )
                                            }
                                            innerTextField()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Map Icon Button for Pickup
                                IconButton(
                                    onClick = { selectingLocationMode = "pickup" },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Map,
                                        contentDescription = "Pick Pickup on Map",
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Arrow Icon
                        Text(
                            text = "➔",
                            fontSize = 14.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold
                        )

                        // Drop Input Box with Map Icon Button on right
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 10.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    BasicTextField(
                                        value = drop,
                                        onValueChange = { drop = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF0F172A)
                                        ),
                                        decorationBox = { innerTextField ->
                                            if (drop.isEmpty()) {
                                                Text(
                                                    text = "Drop address",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF94A3B8)
                                                )
                                            }
                                            innerTextField()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Map Icon Button for Drop
                                IconButton(
                                    onClick = { selectingLocationMode = "drop" },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Map,
                                        contentDescription = "Pick Drop on Map",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val isRideEnabled = pickup.isNotBlank() && drop.isNotBlank()

                    // RIDE BUTTON (Clickable ONLY when both pickup and drop are entered)
                    Button(
                        onClick = {
                            if (isRideEnabled) {
                                onConfirmRideSearch(pickup.trim(), drop.trim())
                            }
                        },
                        enabled = isRideEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF18181B),
                            disabledContainerColor = Color(0xFF94A3B8).copy(alpha = 0.5f),
                            contentColor = Color.White,
                            disabledContentColor = Color.White.copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Ride",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRideEnabled) Color.White else Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = if (isRideEnabled) GlipGreenPrimary else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // BOTTOM: Small Empty Box for Ad (User requested: no mock ads, just empty container ready for Ad)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF8FAFC)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        // Empty container ready for Ad
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Location Picker Overlay (Opens map selection screen with fixed center pin)
        if (selectingLocationMode != null) {
            val isPickup = selectingLocationMode == "pickup"
            val title = if (isPickup) "Select Pickup on Map" else "Select Drop on Map"
            val currentLat = if (isPickup) pickupCoordinates?.first else dropCoordinates?.first
            val currentLng = if (isPickup) pickupCoordinates?.second else dropCoordinates?.second

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(100f)
            ) {
                LocationPickerScreen(
                    title = title,
                    initialLat = currentLat,
                    initialLng = currentLng,
                    onLocationConfirmed = { detailedAddress, lat, lng ->
                        if (isPickup) {
                            pickup = detailedAddress
                            pickupCoordinates = Pair(lat, lng)
                        } else {
                            drop = detailedAddress
                            dropCoordinates = Pair(lat, lng)
                        }
                        selectingLocationMode = null
                    },
                    onDismiss = {
                        selectingLocationMode = null
                    }
                )
            }
        }
    }
}
