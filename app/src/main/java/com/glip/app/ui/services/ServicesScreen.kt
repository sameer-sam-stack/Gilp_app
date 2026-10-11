package com.glip.app.ui.services

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.app.ui.theme.GlipGreenPrimary
import com.glip.app.ui.theme.GlipOrangeAccent

@Composable
fun ServicesScreen(
    viewModel: ServicesViewModel,
    onNavigateToHomePooling: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Big Header: "All Services"
        Text(
            text = "All Services",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Row of 3 Service Cards exactly as shown in Image 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Parcel on Bike
            ServiceTile(
                title = "Parcel on Bike",
                icon = Icons.Default.TwoWheeler,
                iconColor = GlipOrangeAccent,
                backgroundColor = Color(0xFFFFF7ED),
                onClick = { },
                modifier = Modifier.weight(1f)
            )

            // 2. School Vehicle
            ServiceTile(
                title = "School Vehicle",
                icon = Icons.Default.DirectionsBus,
                iconColor = Color(0xFFEAB308),
                backgroundColor = Color(0xFFFEFCE8),
                onClick = { },
                modifier = Modifier.weight(1f)
            )

            // 3. Personal Vehicle
            ServiceTile(
                title = "Personal Vehicle",
                icon = Icons.Default.DirectionsCar,
                iconColor = GlipGreenPrimary,
                backgroundColor = Color(0xFFF0FDF4),
                onClick = onNavigateToHomePooling,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ServiceTile(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
