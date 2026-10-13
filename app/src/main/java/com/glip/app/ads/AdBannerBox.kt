package com.glip.app.ads

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable Jetpack Compose Ad Container.
 * Automatically loads, scales and renders ads from the live Ads API.
 * Handles auto-fit scaling, click redirects, and impression telemetry.
 */
@Composable
fun AdBannerBox(
    slotId: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    contentScale: ContentScale = ContentScale.Crop,
    showAdBadge: Boolean = true,
    fallbackContent: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val slotsMap by AdManager.slotsState.collectAsState()
    val slot = slotsMap[slotId]

    // If slot is explicitly disabled by admin, hide or show fallback
    if (slot != null && !slot.isActive) {
        if (fallbackContent != null) {
            fallbackContent()
        }
        return
    }

    val ad = slot?.ad

    if (ad != null && ad.bannerImageUrl.isNotBlank()) {
        var bitmap by remember(ad.bannerImageUrl) { mutableStateOf<Bitmap?>(null) }

        LaunchedEffect(ad.bannerImageUrl) {
            val loaded = AdManager.loadBitmap(ad.bannerImageUrl)
            bitmap = loaded
            if (loaded != null) {
                AdManager.onAdDisplayed(ad.id, slotId)
            }
        }

        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .clickable {
                        AdManager.onAdClicked(context, ad, slotId)
                    }
            ) {
                Crossfade(targetState = bitmap, label = "AdBannerCrossfade") { currentBitmap ->
                    if (currentBitmap != null) {
                        Image(
                            bitmap = currentBitmap.asImageBitmap(),
                            contentDescription = ad.name,
                            contentScale = contentScale,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Subtle placeholder while image is downloading
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.2f))
                        )
                    }
                }

                // Small sponsor / ad badge
                if (showAdBadge && bitmap != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Ad",
                            color = Color.White,
                            fontSize = 8.sp,
                            lineHeight = 10.sp
                        )
                    }
                }
            }
        }
    } else {
        // No ad currently assigned to this slot
        if (fallbackContent != null) {
            fallbackContent()
        } else {
            // Render clean empty box
            Card(
                modifier = modifier,
                shape = shape,
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
