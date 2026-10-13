package com.glip.app.ads

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Singleton State & Cache Manager for Advertisement Display
 */
object AdManager {
    private const val TAG = "AdManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _slotsState = MutableStateFlow<Map<String, PlacementSlot>>(emptyMap())
    val slotsState: StateFlow<Map<String, PlacementSlot>> = _slotsState.asStateFlow()

    // In-memory LRU Bitmap cache (max 25 entries or ~20MB)
    private val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(25) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    // Deduplication set for impressions within the same display session
    private val recordedImpressions = mutableSetOf<String>()

    private var isInitialized = false

    /**
     * Initializes background sync for in-app ad placements
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true

        Log.d(TAG, "Initializing AdManager with base URL: ${AdConfig.BASE_URL}")

        // Launch periodic sync coroutine
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                syncPlacements()
                delay(AdConfig.REFRESH_INTERVAL_MILLIS)
            }
        }
    }

    /**
     * Manually triggers an immediate placements sync
     */
    fun refresh() {
        scope.launch(Dispatchers.IO) {
            syncPlacements()
        }
    }

    private suspend fun syncPlacements() {
        val result = AdApiClient.fetchPlacements()
        if (result.isSuccess) {
            val list = result.getOrNull() ?: emptyList()
            val map = list.associateBy { it.slotId }
            _slotsState.value = map
            Log.d(TAG, "Placements updated successfully. Total slots: ${map.size}")

            // Pre-cache banners for instant zero-lag rendering across all slider items
            list.forEach { slot ->
                slot.getActiveAdsList().forEach { ad ->
                    val bannerUrl = ad.bannerImageUrl
                    if (!bannerUrl.isNullOrBlank()) {
                        loadBitmap(bannerUrl)
                    }
                }
            }
        } else {
            Log.w(TAG, "Placements sync failed: ${result.exceptionOrNull()?.message}")
        }
    }

    /**
     * Retrieves cached bitmap or downloads asynchronously
     */
    suspend fun loadBitmap(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        if (imageUrl.isBlank()) return@withContext null

        synchronized(memoryCache) {
            val cached = memoryCache.get(imageUrl)
            if (cached != null) return@withContext cached
        }

        try {
            val url = URL(imageUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                doInput = true
                setRequestProperty("User-Agent", "GlipGo-Android-App/1.0")
            }
            connection.connect()
            val stream = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(stream)
            stream.close()

            if (bitmap != null) {
                synchronized(memoryCache) {
                    memoryCache.put(imageUrl, bitmap)
                }
            }
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download ad banner bitmap: $imageUrl", e)
            null
        }
    }

    /**
     * Downloads and caches animated GIF as native Drawable
     */
    suspend fun loadGifDrawable(context: Context, gifUrl: String): android.graphics.drawable.Drawable? = withContext(Dispatchers.IO) {
        if (gifUrl.isBlank()) return@withContext null
        try {
            val fileName = "ad_gif_" + gifUrl.hashCode().toString() + ".gif"
            val file = java.io.File(context.cacheDir, fileName)

            if (!file.exists() || file.length() == 0L) {
                val url = URL(gifUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    doInput = true
                    setRequestProperty("User-Agent", "GlipGo-Android-App/1.0")
                }
                connection.inputStream.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(file)
                android.graphics.ImageDecoder.decodeDrawable(source)
            } else {
                android.graphics.drawable.BitmapDrawable(context.resources, file.absolutePath)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load GIF drawable: $gifUrl", e)
            null
        }
    }

    /**
     * Records impression telemetry once per ad display
     */
    fun onAdDisplayed(adId: String, slotId: String) {
        val key = "${adId}_$slotId"
        if (recordedImpressions.add(key)) {
            scope.launch(Dispatchers.IO) {
                AdApiClient.recordImpression(adId, slotId)
                Log.d(TAG, "Recorded impression for ad: $adId in slot: $slotId")
            }
        }
    }

    /**
     * Handles ad click: fires telemetry & launches destination URL (Website or WhatsApp)
     */
    fun onAdClicked(context: Context, ad: AppAd, slotId: String) {
        Log.d(TAG, "Ad clicked: ${ad.name} -> ${ad.destinationUrl}")

        // 1. Report click telemetry in background
        scope.launch(Dispatchers.IO) {
            AdApiClient.recordClick(ad.id, slotId)
        }

        // 2. Open destination URL
        try {
            val rawUrl = ad.destinationUrl.trim()
            val uri = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
                Uri.parse(rawUrl)
            } else {
                Uri.parse("https://$rawUrl")
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch destination url: ${ad.destinationUrl}", e)
        }
    }
}
