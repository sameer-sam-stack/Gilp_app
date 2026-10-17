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
import com.glip.app.ui.components.RouteLoader
import com.glip.app.util.FareCalculationEngine
import com.glip.app.util.PersonalFareEstimate
import com.glip.app.util.PersonalVehicleType
import java.util.Locale
import org.osmdroid.util.GeoPoint

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    pickupLocation: String = "",
    dropLocation: String = "",
    userName: String = "",
    userPhone: String = "",
    onChangeLocation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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

        // 1. Top AD Banner Box: Kept 100% Intact as specified by user
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
            Spacer(modifier = Modifier.height(4.dp))

            // Dynamic Ride Status Indicator Banner
            val isAccepted = uiState.rideBookingStatus == "ACCEPTED"
            val isRequested = uiState.rideBookingStatus == "REQUESTED"

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 6.dp),
                shape = RoundedCornerShape(10.dp),
                color = when {
                    isAccepted -> Color(0xFFF0FDF4)
                    isRequested -> Color(0xFFFFFBEB)
                    else -> Color(0xFFF0FDF4)
                },
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    when {
                        isAccepted -> Color(0xFF86EFAC)
                        isRequested -> Color(0xFFFDE68A)
                        else -> Color(0xFF86EFAC)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isAccepted -> Color(0xFF16A34A)
                                        isRequested -> Color(0xFFD97706)
                                        else -> GlipGreenPrimary
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isAccepted -> "Driver Accepted! Arriving in ${uiState.acceptedVehicle?.etaMinutes ?: 2} mins"
                                isRequested -> "Request Sent to ${uiState.selectedVehicle?.driver?.fullName ?: "Driver"}..."
                                else -> "Drivers Heading Along Your Route"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isAccepted -> Color(0xFF15803D)
                                isRequested -> Color(0xFFB45309)
                                else -> Color(0xFF15803D)
                            }
                        )
                    }
                    if (isRequested) {
                        Text(
                            text = "Cancel",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.clickable { viewModel.cancelCurrentRide() }
                        )
                    } else {
                        Text(
                            text = if (userPhone.isNotBlank()) userPhone else "Live Corridor",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF166534)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Map Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                ) {
                    RouteLoader(
                        modifier = Modifier.size(width = 240.dp, height = 136.dp)
                    )
                }
            } else if (!uiState.isFallbackListVisible) {
                InteractiveDemoMap(
                    vehicles = if (uiState.rideBookingStatus == "ACCEPTED") listOfNotNull(uiState.acceptedVehicle) else uiState.filteredVehicles,
                    selectedVehicle = uiState.selectedVehicle,
                    onVehicleSelect = { viewModel.selectVehicle(it) },
                    onRecenter = { viewModel.recenterDemoArea() },
                    passengerRoutePoints = uiState.passengerRoutePoints,
                    isRideAccepted = uiState.rideBookingStatus == "ACCEPTED",
                    rideBookingStatus = uiState.rideBookingStatus,
                    pickupGeoPoint = if (uiState.pickupLat != 0.0) GeoPoint(uiState.pickupLat, uiState.pickupLng) else null,
                    dropGeoPoint = if (uiState.dropLat != 0.0) GeoPoint(uiState.dropLat, uiState.dropLng) else null
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
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // View toggle & Active vehicles counter
        if (uiState.rideBookingStatus != "ACCEPTED") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${uiState.filteredVehicles.size} active vehicle(s) on your route",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
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
        }

        // 3. Bottom Cards Area
        if (uiState.rideBookingStatus == "ACCEPTED") {
            // ACCEPTED RIDE: Show dedicated single driver card with live ETA and Call
            val driverVeh = uiState.acceptedVehicle ?: uiState.selectedVehicle
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DRIVER ARRIVING",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF15803D)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFDCFCE7)
                        ) {
                            Text(
                                text = "${driverVeh?.etaMinutes ?: 2} mins away",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = driverVeh?.driver?.fullName ?: "Driver Partner",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "${driverVeh?.licensePlateMasked ?: "DL 01"} • ${driverVeh?.modelName ?: "Auto"}",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        // Call Driver Button
                        Surface(
                            shape = CircleShape,
                            color = GlipGreenPrimary,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(38.dp)
                                .clickable {
                                    val phoneToCall = driverVeh?.driverPhone?.ifBlank { "9874561239" } ?: "9874561239"
                                    val callIntent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:$phoneToCall")
                                    }
                                    context.startActivity(callIntent)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call Driver",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { viewModel.cancelCurrentRide() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Cancel Ride",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            val isPersonalMode = uiState.isPersonalRideMode || uiState.filteredVehicles.isEmpty()

            // Mode Selector Tabs (only shown when vehicles exist along the corridor)
            if (uiState.filteredVehicles.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tab 1: Shared Pooling
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setPersonalRideMode(false) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (!isPersonalMode) GlipGreenPrimary.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (!isPersonalMode) GlipGreenPrimary else Color(0xFFE2E8F0)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Shared Pooling",
                                fontSize = 11.sp,
                                fontWeight = if (!isPersonalMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isPersonalMode) GlipGreenDark else Color(0xFF475569)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = "Save 48%",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534),
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    // Tab 2: Personal Vehicle
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setPersonalRideMode(true) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isPersonalMode) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isPersonalMode) Color(0xFFF59E0B) else Color(0xFFE2E8F0)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Personal Ride",
                                fontSize = 11.sp,
                                fontWeight = if (isPersonalMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (isPersonalMode) Color(0xFF92400E) else Color(0xFF475569)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFEF08A)
                            ) {
                                Text(
                                    text = "Solo",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF854D0E),
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (isPersonalMode) {
                // RAPIDO / UBER STYLE PERSONAL VEHICLE SELECTOR
                val effectiveKm = if (uiState.tripDistanceKm > 0.0) uiState.tripDistanceKm else 5.4
                val personalOptions = if (uiState.personalOptions.isNotEmpty()) {
                    uiState.personalOptions
                } else {
                    FareCalculationEngine.getPersonalOptions(effectiveKm)
                }
                val selectedPersonal = uiState.selectedPersonalOption ?: personalOptions.firstOrNull { it.type == PersonalVehicleType.AUTO } ?: personalOptions.firstOrNull()

                Column(modifier = Modifier.fillMaxWidth()) {
                    PersonalRideOptionsCard(
                        options = personalOptions,
                        selectedOption = selectedPersonal,
                        onSelectOption = { viewModel.selectPersonalOption(it) },
                        onBookClick = { opt ->
                            viewModel.bookPersonalRide(opt, userName, userPhone)
                        },
                        canSwitchToPooling = uiState.filteredVehicles.isNotEmpty(),
                        onSwitchToPooling = { viewModel.setPersonalRideMode(false) },
                        distanceKm = effectiveKm,
                        isRequested = uiState.rideBookingStatus == "REQUESTED",
                        isCorridorEmpty = uiState.filteredVehicles.isEmpty()
                    )

                    // Trailing AD Container: Kept 100% Intact as specified by user
                    AdBannerBox(
                        slotId = AdConfig.SLOT_HOME_VEHICLE_TRAILING_CARD,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            } else {
                // SHARED POOLING VEHICLES LIST
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    // Vehicle Details Boxes
                    items(uiState.filteredVehicles) { vehicle ->
                        val isSelected = vehicle.vehicleId == (uiState.selectedVehicle?.vehicleId ?: uiState.filteredVehicles.firstOrNull()?.vehicleId)
                        VehicleCard(
                            vehicle = vehicle,
                            isSelected = isSelected,
                            onClick = { viewModel.selectVehicle(vehicle) },
                            onBookClick = {
                                viewModel.bookSelectedVehicle(vehicle, userName, userPhone)
                            },
                            modifier = Modifier.width(120.dp)
                        )
                    }

                    // Trailing AD Container: Kept 100% Intact as specified by user
                    item {
                        AdBannerBox(
                            slotId = AdConfig.SLOT_HOME_VEHICLE_TRAILING_CARD,
                            modifier = Modifier
                                .width(120.dp)
                                .height(120.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Quick Request Button for Currently Selected Vehicle
                val sel = uiState.selectedVehicle ?: uiState.filteredVehicles.firstOrNull()
                if (sel != null && uiState.rideBookingStatus == "NONE") {
                    Button(
                        onClick = {
                            viewModel.bookSelectedVehicle(sel, userName, userPhone)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 2.dp)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GlipGreenPrimary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Request Seat with ${sel.driver.fullName} (₹${sel.farePerKmEstimate.toInt()})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }

                    // Fallback shortcut link to Personal Ride
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Want a private auto/cab? Book Personal Vehicle ➔",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD97706),
                            modifier = Modifier
                                .clickable { viewModel.setPersonalRideMode(true) }
                                .padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Rapido / Uber Style Personal Vehicle Selection Card.
 * Displays vehicle categories (Bike, Auto, Cab Economy, Cab Premium, Cab XL),
 * upfront calculated fares, Cash / Offers row, and Book button.
 */
@Composable
fun PersonalRideOptionsCard(
    options: List<PersonalFareEstimate>,
    selectedOption: PersonalFareEstimate?,
    onSelectOption: (PersonalFareEstimate) -> Unit,
    onBookClick: (PersonalFareEstimate) -> Unit,
    canSwitchToPooling: Boolean,
    onSwitchToPooling: () -> Unit,
    distanceKm: Double,
    isRequested: Boolean,
    isCorridorEmpty: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PERSONAL RIDE (${String.format(Locale.getDefault(), "%.1f", distanceKm)} KM)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFB45309),
                        letterSpacing = 0.5.sp
                    )
                }

                if (canSwitchToPooling) {
                    Text(
                        text = "Switch to Pooling",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlipGreenPrimary,
                        modifier = Modifier.clickable { onSwitchToPooling() }
                    )
                }
            }

            if (isCorridorEmpty) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "No shared vehicles on your route corridor. Nearest driver will be dispatched for your personal ride.",
                    fontSize = 10.5.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Vehicle Category Rows (Bike, Auto, Cab Economy, Cab Premium, Cab XL)
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                options.forEach { opt ->
                    val isSelected = opt.type == (selectedOption?.type ?: PersonalVehicleType.AUTO)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectOption(opt) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFFF8FAFC) else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 1.8.dp else 1.dp,
                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: Vehicle Icon + Title + Capacity + Subtitle
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (opt.type) {
                                            PersonalVehicleType.BIKE -> Icons.Default.TwoWheeler
                                            PersonalVehicleType.AUTO -> Icons.Default.ElectricRickshaw
                                            PersonalVehicleType.CAB_MINI -> Icons.Default.DirectionsCar
                                            PersonalVehicleType.CAB_PREMIUM -> Icons.Default.LocalTaxi
                                            PersonalVehicleType.CAB_XL -> Icons.Default.AirportShuttle
                                        },
                                        contentDescription = opt.type.displayName,
                                        tint = if (isSelected) Color(0xFF2563EB) else Color(0xFF334155),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = opt.type.displayName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "${opt.type.capacity}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    Text(
                                        text = "${opt.etaMinutes} min away • Drop ${opt.dropTimeFormatted}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            // Right: Upfront Price (e.g. ₹118 Auto, ₹84 Bike)
                            Text(
                                text = "₹${opt.fareAmount}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cash / Offers Row (matches Rapido reference design)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "₹", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cash",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(Color(0xFFE2E8F0))
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Offers",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Booking Button (Rapido warm gold yellow #F5B838)
            val effectiveSel = selectedOption ?: options.firstOrNull()
            Button(
                onClick = {
                    if (effectiveSel != null && !isRequested) {
                        onBookClick(effectiveSel)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF5B838),
                    disabledContainerColor = Color(0xFFE2E8F0)
                ),
                enabled = !isRequested
            ) {
                Text(
                    text = if (isRequested) "Requesting Nearest Driver..." else "Book ${effectiveSel?.type?.displayName ?: "Vehicle"}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
            }
        }
    }
}

/**
 * Compact Redesigned Vehicle Box with:
 * 1. Color Indicator dot matching its map polyline
 * 2. Driver Name
 * 3. Seat info (e.g. 3/5 seats free)
 * 4. Upfront Pooling Fare Badge (e.g. ₹61 • Save ~48%)
 * 5. ETA & Plate number
 * 6. Call button
 */
@Composable
fun VehicleCard(
    vehicle: Vehicle,
    isSelected: Boolean,
    onClick: () -> Unit,
    onBookClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorInt = try {
        Color(android.graphics.Color.parseColor(vehicle.routeColorHex))
    } catch (_: Exception) {
        GlipGreenPrimary
    }

    Card(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.8.dp else 1.dp,
            color = if (isSelected) colorInt else Color(0xFFE2E8F0)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 7.dp, vertical = 6.dp)
        ) {
            // 1. Driver Name + Route Color Dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(colorInt)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = vehicle.driver.fullName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 2. Seat Info & ETA in Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (vehicle.seatAvailability.availableSeats > 0) Color(0xFFDCFCE7) else Color(0xFFFFEDD5)
                ) {
                    Text(
                        text = "${vehicle.seatAvailability.availableSeats}/${vehicle.seatAvailability.totalSeats} seats",
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (vehicle.seatAvailability.availableSeats > 0) GlipGreenDark else GlipOrangeAccent,
                        fontSize = 8.5.sp
                    )
                }

                Text(
                    text = "${vehicle.etaMinutes}m away",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 3. Upfront Pooling Fare Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFEFF6FF)
                ) {
                    Text(
                        text = "₹${vehicle.farePerKmEstimate.toInt()}",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1D4ED8),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Text(
                    text = "Save 48%",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF16A34A)
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // 4. Vehicle Number & Call Button in a row
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

                // Call icon button
                Surface(
                    shape = CircleShape,
                    color = GlipGreenPrimary,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable {
                            val phoneToCall = vehicle.driverPhone.ifBlank { "9874561239" }
                            val callIntent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$phoneToCall")
                            }
                            context.startActivity(callIntent)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call Driver",
                            tint = Color.Black,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }
}
