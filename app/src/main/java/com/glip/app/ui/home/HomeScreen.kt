package com.glip.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.app.R
import com.glip.app.domain.model.Vehicle
import com.glip.app.ui.components.DemoBadge
import com.glip.app.ui.components.GlipBrandHeader
import com.glip.app.ui.theme.GlipGreenDark
import com.glip.app.ui.theme.GlipGreenPrimary
import com.glip.app.ui.theme.GlipOrangeAccent

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        GlipBrandHeader()

        // Quick Search along corridor
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = uiState.pickupQuery,
                onValueChange = { viewModel.onPickupQueryChange(it) },
                placeholder = { Text("From (e.g. Connaught Place)", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = GlipGreenPrimary, modifier = Modifier.size(16.dp))
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = uiState.dropQuery,
                onValueChange = { viewModel.onDropQueryChange(it) },
                placeholder = { Text("To (e.g. Cyber Hub)", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Navigation, contentDescription = null, tint = GlipOrangeAccent, modifier = Modifier.size(16.dp))
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Demo Notice Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = GlipGreenDark,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.map_legend_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Map View (60% height in normal view)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (!uiState.isFallbackListVisible) {
                InteractiveDemoMap(
                    vehicles = uiState.filteredVehicles,
                    selectedVehicle = uiState.selectedVehicle,
                    onVehicleSelect = { viewModel.selectVehicle(it) },
                    onRecenter = { viewModel.recenterDemoArea() }
                )
            } else {
                // Fallback List View
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredVehicles) { vehicle ->
                        VehicleCard(
                            vehicle = vehicle,
                            isSelected = vehicle.vehicleId == uiState.selectedVehicle?.vehicleId,
                            onClick = { viewModel.selectVehicle(vehicle) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mode Switcher Pill (Map vs List Fallback)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${uiState.filteredVehicles.size} simulated vehicle(s) on route",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = { viewModel.toggleFallbackListView() },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (uiState.isFallbackListVisible) "Show Map View" else "Show List Fallback",
                    fontSize = 12.sp,
                    color = GlipGreenPrimary
                )
            }
        }

        // Selected Vehicle Detail Bottom Card
        uiState.selectedVehicle?.let { vehicle ->
            VehicleCard(
                vehicle = vehicle,
                isSelected = true,
                onClick = {},
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
fun VehicleCard(
    vehicle: Vehicle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(if (isSelected) 3.dp else 0.dp),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GlipGreenPrimary)) else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = vehicle.modelName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${vehicle.licensePlateMasked})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (vehicle.seatAvailability.availableSeats > 1) GlipGreenPrimary.copy(alpha = 0.15f) else GlipOrangeAccent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${vehicle.seatAvailability.availableSeats} of ${vehicle.seatAvailability.totalSeats} seats free",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (vehicle.seatAvailability.availableSeats > 1) GlipGreenDark else GlipOrangeAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Route Origin -> Destination
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = null, tint = GlipGreenPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = vehicle.route.originName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "➔", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.Navigation, contentDescription = null, tint = GlipOrangeAccent, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = vehicle.route.destinationName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Driver: ${vehicle.driver.fullName} (${vehicle.driver.rating} ★)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Sample est: ₹${vehicle.farePerKmEstimate}/km",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = GlipGreenPrimary
                )
            }
        }
    }
}
