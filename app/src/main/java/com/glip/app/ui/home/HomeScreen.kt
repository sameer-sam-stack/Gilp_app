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
import com.glip.app.domain.model.VehicleCategory
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

        // 1. Top AD Banner Box: Minimized left/right gap per user requirement
        AdBannerBox(
            slotId = AdConfig.SLOT_HOME_TOP_BANNER,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 6.dp, bottom = 10.dp)
                .height(96.dp),
            shape = RoundedCornerShape(14.dp)
        )

        // Map Area (Directly under Top AD per user requirement)
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

        Spacer(modifier = Modifier.height(4.dp))

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

                    // 4-Digit Ride OTP Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "RIDE OTP",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFB45309)
                                )
                                Text(
                                    text = "Share OTP with driver upon boarding",
                                    fontSize = 9.5.sp,
                                    color = Color(0xFF78350F)
                                )
                            }
                            Text(
                                text = uiState.rideOtp.ifBlank { "4821" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB45309),
                                letterSpacing = 2.sp
                            )
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
            val isSearchingRoute = uiState.isSearchingRouteVehicles && uiState.filteredVehicles.isEmpty()
            val isPersonalMode = uiState.isPersonalRideMode || (!uiState.isSearchingRouteVehicles && uiState.filteredVehicles.isEmpty())

            if (isSearchingRoute) {
                // 60-Second Route Vehicle Search Radar Card (Auto-switches to Personal if no route driver joins)
                val effectiveKm = if (uiState.tripDistanceKm > 0.0) uiState.tripDistanceKm else 5.4
                RouteVehicleSearchingCard(
                    pickupLocation = pickupLocation,
                    dropLocation = dropLocation,
                    onChangeLocation = onChangeLocation,
                    secondsRemaining = uiState.routeSearchSecondsRemaining,
                    distanceKm = effectiveKm,
                    onSkipToPersonal = { viewModel.skipRouteSearchAndOpenPersonal() }
                )
            } else {
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

                PersonalRideOptionsCard(
                    pickupLocation = pickupLocation,
                    dropLocation = dropLocation,
                    onChangeLocation = onChangeLocation,
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
}

/**
 * Rapido / Uber Style Personal Vehicle Selection Card.
 * Displays vehicle categories (Bike, Auto, Cab Economy, Cab Premium, Cab XL),
 * upfront calculated fares, Cash / Offers row, and Book button.
 */
@Composable
fun PersonalRideOptionsCard(
    pickupLocation: String = "",
    dropLocation: String = "",
    onChangeLocation: () -> Unit = {},
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
            // 1. Pickup and Drop and Change Bar (Replaces old Personal Ride header per user requirement)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(GlipGreenPrimary)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = pickupLocation.ifBlank { "Pickup" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "➔", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = dropLocation.ifBlank { "Drop" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Text(
                        text = "Change",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlipGreenPrimary,
                        modifier = Modifier
                            .clickable { onChangeLocation() }
                            .padding(start = 6.dp, top = 2.dp, bottom = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. "No shared vehicle on Your Route.." Text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "No shared vehicle on Your Route..",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )

                if (canSwitchToPooling) {
                    Text(
                        text = "Switch to Pooling",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlipGreenPrimary,
                        modifier = Modifier.clickable { onSwitchToPooling() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Vehicle Category Rows (Bike ₹22/km, E-Rickshaw ₹27/km, Auto ₹30/km, Cab ₹40/km) matching Screenshot 1
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { opt ->
                    val isSelected = opt.type == (selectedOption?.type ?: PersonalVehicleType.AUTO)
                    val isAvailable = opt.isAvailable
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectOption(opt) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFF0F7FF) else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = when {
                                isSelected -> Color(0xFF2563EB)
                                !isAvailable -> Color(0xFFF1F5F9)
                                else -> Color(0xFFE2E8F0)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
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
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when {
                                                isSelected -> Color(0xFFE0EDFE)
                                                !isAvailable -> Color(0xFFF8FAFC)
                                                else -> Color(0xFFF1F5F9)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (opt.type) {
                                            PersonalVehicleType.BIKE -> Icons.Default.TwoWheeler
                                            PersonalVehicleType.E_RICKSHAW -> Icons.Default.ElectricRickshaw
                                            PersonalVehicleType.AUTO -> Icons.Default.ElectricRickshaw
                                            PersonalVehicleType.CAB -> Icons.Default.DirectionsCar
                                        },
                                        contentDescription = opt.type.displayName,
                                        tint = when {
                                            isSelected -> Color(0xFF2563EB)
                                            !isAvailable -> Color(0xFF94A3B8)
                                            else -> Color(0xFF0F172A)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = opt.type.displayName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAvailable) Color(0xFF0F172A) else Color(0xFF64748B)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isAvailable) Color(0xFF64748B) else Color(0xFF94A3B8),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "${opt.type.capacity}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isAvailable) Color(0xFF64748B) else Color(0xFF94A3B8)
                                        )
                                        if (!isAvailable) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFFEE2E2)
                                            ) {
                                                Text(
                                                    text = "No Drivers",
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFDC2626),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isAvailable) {
                                            "${opt.etaMinutes} min away • Drop ${opt.dropTimeFormatted}"
                                        } else {
                                            "No driver online in your area"
                                        },
                                        fontSize = 11.sp,
                                        color = if (isAvailable) Color(0xFF64748B) else Color(0xFFEF4444)
                                    )
                                }
                            }

                            // Right: Upfront Price (e.g. ₹87 Auto, ₹64 Bike) + Rate per KM
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹${opt.fareAmount}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isAvailable) Color(0xFF0F172A) else Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "₹${opt.type.perKmRate.toInt()}/km",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Booking Button (Rapido warm gold yellow #F5B838)
            val effectiveSel = selectedOption ?: options.firstOrNull()
            val isAvailable = effectiveSel?.isAvailable == true
            Button(
                onClick = {
                    if (effectiveSel != null && !isRequested && isAvailable) {
                        onBookClick(effectiveSel)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAvailable) Color(0xFFF5B838) else Color(0xFFE2E8F0),
                    disabledContainerColor = Color(0xFFE2E8F0)
                ),
                enabled = !isRequested && isAvailable
            ) {
                Text(
                    text = when {
                        isRequested -> "Requesting Nearest Driver..."
                        !isAvailable -> "No ${effectiveSel?.type?.displayName ?: "Vehicle"} Available Nearby"
                        else -> "Book ${effectiveSel?.type?.displayName ?: "Vehicle"} • ₹${effectiveSel?.fareAmount}"
                    },
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isAvailable && !isRequested) Color.Black else Color(0xFF94A3B8)
                )
            }
        }
    }
}

/**
 * 120-Second Route Vehicle Scanning Radar Card.
 * Sleek, compact radar with yellow progress indicator and single "Scanning Wait.." status.
 */
@Composable
fun RouteVehicleSearchingCard(
    pickupLocation: String = "",
    dropLocation: String = "",
    onChangeLocation: () -> Unit = {},
    secondsRemaining: Int,
    distanceKm: Double,
    onSkipToPersonal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(GlipGreenPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FINDING SHARED VEHICLES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GlipGreenDark,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFEFF6FF)
                ) {
                    Text(
                        text = timeFormatted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pickup and drop and change
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(GlipGreenPrimary)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = pickupLocation.ifBlank { "Pickup" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "➔", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = dropLocation.ifBlank { "Drop" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Text(
                        text = "Change",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlipGreenPrimary,
                        modifier = Modifier
                            .clickable { onChangeLocation() }
                            .padding(start = 6.dp, top = 2.dp, bottom = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Yellow Progress Bar (Pila line jo aage badte rahta hai)
            LinearProgressIndicator(
                progress = { (120 - secondsRemaining) / 120f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFFF5B838),
                trackColor = Color(0xFFFEF3C7)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scanning Wait..",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Skip & Book Personal Vehicle Button
            Button(
                onClick = onSkipToPersonal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5B838)),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Skip Wait • Book Personal Ride Now",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

/**
 * Compact Redesigned Vehicle Box:
 * Shows vehicle category, available seats, ETA, and upfront pooling fare.
 * Driver full personal name, phone and license plate are hidden until ride is accepted.
 */
@Composable
fun VehicleCard(
    vehicle: Vehicle,
    isSelected: Boolean,
    onClick: () -> Unit,
    onBookClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val categoryLabel = when (vehicle.category) {
        VehicleCategory.BIKE -> "Bike"
        VehicleCategory.AUTO_RICKSHAW -> "Auto"
        VehicleCategory.E_RICKSHAW -> "E-Rickshaw"
        else -> "Cab"
    }

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
            // 1. Vehicle Category Title + Color Dot
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
                    text = categoryLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

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

            Spacer(modifier = Modifier.height(3.dp))

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
                        fontSize = 10.sp,
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
        }
    }
}
