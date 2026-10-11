package com.glip.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.app.ui.theme.GlipGreenPrimary

/**
 * RideSearchScreen:
 * Opens when the app launches (after registration).
 * Shows a full-screen map background with the centered Ride Search Card (matching User's Image).
 * Clicking the Map icon opens the interactive map modal to pick location.
 * Clicking "Ride ->" navigates to the active Ride / Vehicles view.
 */
@Composable
fun RideSearchScreen(
    initialPickup: String = "",
    initialDrop: String = "",
    onConfirmRideSearch: (pickup: String, drop: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var pickup by remember { mutableStateOf(initialPickup) }
    var drop by remember { mutableStateOf(initialDrop) }
    var showMapPickerFor by remember { mutableStateOf<String?>(null) } // "pickup" or "drop" or null

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        // Background Subtle Map Simulation
        InteractiveDemoMap(
            vehicles = emptyList(),
            selectedVehicle = null,
            onVehicleSelect = {},
            onRecenter = {},
            modifier = Modifier.fillMaxSize()
        )

        // Semi-transparent overlay to emphasize the search card
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.15f))
        )

        // Centered Floating Search Card (Exact design from user's image)
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.92f),
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
                // Row: Pickup label & Drop label with green/red bullet indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pickup indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E)) // Green bullet
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pickup",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }

                    // Drop indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFEF4444)) // Red square/bullet
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

                Spacer(modifier = Modifier.height(10.dp))

                // Row: Pickup Input Box -> Arrow -> Drop Input Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Pickup Input Box with Map Icon
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
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
                                            text = "Pickup point",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    innerTextField()
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Open Map for Pickup",
                                tint = GlipGreenPrimary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { showMapPickerFor = "pickup" }
                            )
                        }
                    }

                    // Arrow Icon
                    Text(
                        text = "➔",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold
                    )

                    // Drop Input Box
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
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
                                            text = "Drop point",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    innerTextField()
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Open Map for Drop",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { showMapPickerFor = "drop" }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Ride -> Button (Black pill button as shown in user's image)
                Button(
                    onClick = {
                        val finalPickup = pickup.ifBlank { "Connaught Place" }
                        val finalDrop = drop.ifBlank { "Cyber Hub Gurugram" }
                        onConfirmRideSearch(finalPickup, finalDrop)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF111827) // Solid Dark Black
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Ride",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Map Location Picker Dialog
        showMapPickerFor?.let { target ->
            AlertDialog(
                onDismissRequest = { showMapPickerFor = null },
                title = {
                    Text(
                        text = if (target == "pickup") "Select Pickup on Map" else "Select Drop on Map",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Tap on map corridor or select demo location:",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            InteractiveDemoMap(
                                vehicles = emptyList(),
                                selectedVehicle = null,
                                onVehicleSelect = {},
                                onRecenter = {},
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val options = if (target == "pickup") {
                                listOf("Connaught Place", "Karol Bagh", "Saket")
                            } else {
                                listOf("Cyber Hub", "Noida Sec 62", "Nehru Place")
                            }
                            options.forEach { loc ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF1F5F9),
                                    modifier = Modifier.clickable {
                                        if (target == "pickup") pickup = loc else drop = loc
                                        showMapPickerFor = null
                                    }
                                ) {
                                    Text(
                                        text = loc,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMapPickerFor = null }) {
                        Text("Done", color = GlipGreenPrimary)
                    }
                }
            )
        }
    }
}
