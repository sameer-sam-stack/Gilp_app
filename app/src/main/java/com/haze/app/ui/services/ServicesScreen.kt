package com.haze.app.ui.services

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.haze.app.ui.theme.GlipGreenPrimary
import com.haze.app.ui.theme.GlipOrangeAccent
import com.haze.app.ads.AdBannerBox
import com.haze.app.ads.AdConfig

@Composable
fun ServicesScreen(
    viewModel: ServicesViewModel,
    onNavigateToHomePooling: () -> Unit,
    onNavigateToParcelBooking: () -> Unit = {},
    onNavigateToSchoolVehicle: () -> Unit = {},
    onNavigateToPersonalVehicle: () -> Unit = {},
    onNavigateToWallet: () -> Unit = {},
    onNavigateToIntercityTravel: () -> Unit = {},
    onNavigateToTravelServices: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Header: "All Services"
        Text(
            text = "All Services",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            fontSize = 24.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Row 1: Parcel, Pay, School Vehicle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Parcel
            ServiceTile(
                title = "Parcel",
                icon = Icons.Default.Inventory2,
                iconColor = GlipOrangeAccent,
                backgroundColor = Color(0xFFFFF7ED),
                onClick = onNavigateToParcelBooking,
                modifier = Modifier.weight(1f)
            )

            // 2. Pay (Navigates directly to Wallet Screen)
            ServiceTile(
                title = "Pay",
                icon = Icons.Default.AccountBalanceWallet,
                iconColor = Color(0xFF6366F1),
                backgroundColor = Color(0xFFEEF2FF),
                onClick = onNavigateToWallet,
                modifier = Modifier.weight(1f)
            )

            // 3. School Vehicle
            ServiceTile(
                title = "School Vehicle",
                icon = Icons.Default.DirectionsBus,
                iconColor = Color(0xFFEAB308),
                backgroundColor = Color(0xFFFEFCE8),
                onClick = onNavigateToSchoolVehicle,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Row 2: Personal Vehicle, Intercity travels, More services
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 4. Personal Vehicle
            ServiceTile(
                title = "Personal Vehicle",
                icon = Icons.Default.DirectionsCar,
                iconColor = GlipGreenPrimary,
                backgroundColor = Color(0xFFF0FDF4),
                onClick = onNavigateToPersonalVehicle,
                modifier = Modifier.weight(1f)
            )

            // 5. Intercity travels
            ServiceTile(
                title = "Intercity travels",
                icon = Icons.Default.DirectionsBus,
                iconColor = Color(0xFF0284C7),
                backgroundColor = Color(0xFFF0F9FF),
                onClick = onNavigateToIntercityTravel,
                modifier = Modifier.weight(1f)
            )

            // 6. More services
            ServiceTile(
                title = "More services",
                icon = Icons.Default.GridView,
                iconColor = Color(0xFF8B5CF6),
                backgroundColor = Color(0xFFF5F3FF),
                onClick = onNavigateToTravelServices,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Fixed-size AD Banner on Services Screen with reduced horizontal margins and less rounded corners
        AdBannerBox(
            slotId = AdConfig.SLOT_SERVICES_BOTTOM_BANNER,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(10.dp),
            fallbackContent = {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Advertisement Space",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun ServiceTile(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null
) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(38.dp)
                )

                if (badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(bottomStart = 8.dp),
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = Color(0xFF0F172A),
            lineHeight = 16.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
