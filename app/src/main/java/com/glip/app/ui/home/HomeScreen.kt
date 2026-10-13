package com.glip.app.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.app.domain.model.Vehicle
import com.glip.app.ui.theme.GlipGreenDark
import com.glip.app.ui.theme.GlipGreenPrimary
import com.glip.app.ui.theme.GlipOrangeAccent
import com.glip.app.ads.AdBannerBox
import com.glip.app.ads.AdConfig

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    pickupLocation: String = "",
    dropLocation: String = "",
    onChangeLocation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Sync search query if passed from search card
    LaunchedEffect(pickupLocation, dropLocation) {
        if (pickupLocation.isNotBlank()) viewModel.onPickupQueryChange(pickupLocation)
        if (dropLocation.isNotBlank()) viewModel.onDropQueryChange(dropLocation)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // 1. Top AD Banner Box: Live AdBannerBox connected to Advertisement Management API
        AdBannerBox(
            slotId = AdConfig.SLOT_HOME_TOP_BANNER,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .height(80.dp),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Selected Route Badge showing current pickup -> drop with ONLY Change clickable
        if (pickupLocation.isNotBlank() || dropLocation.isNotBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(GlipGreenPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = pickupLocation.ifBlank { "Pickup" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "➔", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = dropLocation.ifBlank { "Drop" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Text(
                        text = "Change",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlipGreenPrimary,
                        modifier = Modifier
                            .clickable { onChangeLocation() }
                            .padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onChangeLocation() }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(GlipGreenPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Where to? Enter pickup & drop",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                    Text(
                        text = "Search",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlipGreenPrimary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Map Area - Restored to framed box with gap and rounded corners
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
        ) {
            if (!uiState.isFallbackListVisible) {
                InteractiveDemoMap(
                    vehicles = uiState.filteredVehicles,
                    selectedVehicle = uiState.selectedVehicle,
                    onVehicleSelect = { viewModel.selectVehicle(it) },
                    onRecenter = { viewModel.recenterDemoArea() }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredVehicles) { vehicle ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            VehicleCard(
                                vehicle = vehicle,
                                isSelected = vehicle.vehicleId == uiState.selectedVehicle?.vehicleId,
                                onClick = { viewModel.selectVehicle(vehicle) },
                                modifier = Modifier.width(280.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // View toggle & App Version
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${uiState.filteredVehicles.size} active vehicle",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "• Version 7",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
            }
            TextButton(
                onClick = { viewModel.toggleFallbackListView() },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) {
                Text(
                    text = if (uiState.isFallbackListVisible) "Show Map" else "Show List",
                    fontSize = 12.sp,
                    color = GlipGreenPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 3. Compact Vehicle Details Box (Width 99dp) starting from left, with trailing AD Container
        if (uiState.filteredVehicles.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                // Vehicle Details Boxes (Width 99dp each)
                items(uiState.filteredVehicles) { vehicle ->
                    val isSelected = vehicle.vehicleId == (uiState.selectedVehicle?.vehicleId ?: uiState.filteredVehicles.firstOrNull()?.vehicleId)
                    VehicleCard(
                        vehicle = vehicle,
                        isSelected = isSelected,
                        onClick = { viewModel.selectVehicle(vehicle) },
                        modifier = Modifier.width(99.dp)
                    )
                }

                // Trailing AD Container: Live AdBannerBox placed at the end of the vehicle cards
                item {
                    AdBannerBox(
                        slotId = AdConfig.SLOT_HOME_VEHICLE_TRAILING_CARD,
                        modifier = Modifier
                            .width(99.dp)
                            .height(86.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        } else {
            // If no vehicles are available, show live ad container instead
            AdBannerBox(
                slotId = AdConfig.SLOT_HOME_VEHICLE_TRAILING_CARD,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(86.dp)
                    .padding(bottom = 6.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

/**
 * Compact Redesigned Vehicle Box with ONLY 4 items:
 * 1. Rajesh Sharma (Driver Ka naam)
 * 2. 3/4 Seats free
 * 3. DL 01**4812 (Vehicle number)
 * 4. Call icon button -> opens phone dialer with 1234567890
 */
@Composable
fun VehicleCard(
    vehicle: Vehicle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) GlipGreenPrimary.copy(alpha = 0.6f) else Color(0xFFE2E8F0)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 7.dp, vertical = 6.dp)
        ) {
            // 1. Driver Name (Truncated cleanly)
            Text(
                text = vehicle.driver.fullName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            // 2. Seat Info
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (vehicle.seatAvailability.availableSeats > 0) Color(0xFFDCFCE7) else Color(0xFFFFEDD5)
            ) {
                Text(
                    text = "${vehicle.seatAvailability.availableSeats}/${vehicle.seatAvailability.totalSeats} seats",
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (vehicle.seatAvailability.availableSeats > 0) GlipGreenDark else GlipOrangeAccent,
                    fontSize = 9.sp
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // 3. Vehicle Number & Call Button in a row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = vehicle.licensePlateMasked.replace(" ", ""),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569),
                    fontSize = 9.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                // 4. Call icon button
                Surface(
                    shape = CircleShape,
                    color = GlipGreenPrimary,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable {
                            val callIntent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:1234567890")
                            }
                            context.startActivity(callIntent)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call Driver",
                            tint = Color.Black,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
    }
}
