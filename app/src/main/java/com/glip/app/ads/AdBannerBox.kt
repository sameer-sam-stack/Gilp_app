package com.glip.app.ads

import android.graphics.Bitmap
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import android.widget.VideoView
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
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Universal Jetpack Compose Ad Container supporting:
 * 1. Static Image Ads (JPEG, PNG, WebP)
 * 2. Animated GIF Ads
 * 3. Looping Video Ads (MP4, WebM)
 *
 * Automatically reports impressions & handles destination URL (Website/WhatsApp) clicks.
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
        val mediaType = ad.getMediaType()

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
                when (mediaType) {
                    AdMediaType.VIDEO -> {
                        // 1. VIDEO AD: Looping, muted MP4/WebM video
                        LaunchedEffect(ad.id) {
                            AdManager.onAdDisplayed(ad.id, slotId)
                        }

                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    setVideoURI(Uri.parse(ad.bannerImageUrl))
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = true
                                        mp.setVolume(0f, 0f) // Non-intrusive muted playback
                                        start()
                                    }
                                    setOnErrorListener { _, _, _ -> true }
                                }
                            },
                            update = { videoView ->
                                if (!videoView.isPlaying) {
                                    videoView.start()
                                }
                            }
                        )
                    }

                    AdMediaType.GIF -> {
                        // 2. ANIMATED GIF AD: Native animated drawable
                        var gifDrawable by remember(ad.bannerImageUrl) { mutableStateOf<Drawable?>(null) }

                        LaunchedEffect(ad.bannerImageUrl) {
                            val loaded = AdManager.loadGifDrawable(context, ad.bannerImageUrl)
                            gifDrawable = loaded
                            if (loaded != null) {
                                AdManager.onAdDisplayed(ad.id, slotId)
                            }
                        }

                        if (gifDrawable != null) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    ImageView(ctx).apply {
                                        scaleType = ImageView.ScaleType.CENTER_CROP
                                        setImageDrawable(gifDrawable)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                            (gifDrawable as? AnimatedImageDrawable)?.start()
                                        }
                                    }
                                },
                                update = { imageView ->
                                    imageView.setImageDrawable(gifDrawable)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        (gifDrawable as? AnimatedImageDrawable)?.start()
                                    }
                                }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White.copy(alpha = 0.15f))
                            )
                        }
                    }

                    AdMediaType.IMAGE -> {
                        // 3. STATIC IMAGE AD: Cached bitmap with crossfade
                        var bitmap by remember(ad.bannerImageUrl) { mutableStateOf<Bitmap?>(null) }

                        LaunchedEffect(ad.bannerImageUrl) {
                            val loaded = AdManager.loadBitmap(ad.bannerImageUrl)
                            bitmap = loaded
                            if (loaded != null) {
                                AdManager.onAdDisplayed(ad.id, slotId)
                            }
                        }

                        Crossfade(targetState = bitmap, label = "AdBannerCrossfade") { currentBitmap ->
                            if (currentBitmap != null) {
                                Image(
                                    bitmap = currentBitmap.asImageBitmap(),
                                    contentDescription = ad.name,
                                    contentScale = contentScale,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.White.copy(alpha = 0.2f))
                                )
                            }
                        }
                    }
                }

                // Small sponsor / ad badge
                if (showAdBadge) {
                    val badgeText = when (mediaType) {
                        AdMediaType.VIDEO -> "Ad • Video"
                        AdMediaType.GIF -> "Ad • GIF"
                        AdMediaType.IMAGE -> "Ad"
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
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
