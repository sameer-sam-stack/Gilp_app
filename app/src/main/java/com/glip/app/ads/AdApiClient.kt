package com.glip.app.ads

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Lightweight HTTP client for Ads API using standard Android networking
 */
object AdApiClient {
    private const val TAG = "AdApiClient"

    /**
     * Fetches current placement configurations and their active assigned ads
     */
    suspend fun fetchPlacements(): Result<List<PlacementSlot>> = withContext(Dispatchers.IO) {
        val endpoint = "${AdConfig.BASE_URL.trimEnd('/')}/api/v1/placements"
        try {
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "GlipGo-Android-App/1.0")
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                val rootJson = JSONObject(responseBody)
                val slotsList = mutableListOf<PlacementSlot>()

                val dataArray = rootJson.optJSONArray("data") ?: JSONArray()
                for (i in 0 until dataArray.length()) {
                    val obj = dataArray.getJSONObject(i)
                    val slotId = obj.optString("slotId")
                    val name = obj.optString("name")
                    val screenName = obj.optString("screenName")
                    val heightDp = obj.optInt("heightDp", 80)
                    val widthDp = if (obj.has("widthDp") && !obj.isNull("widthDp")) obj.optInt("widthDp") else null
                    val aspectRatio = obj.optString("aspectRatio", "")
                    val rotationIntervalSeconds = obj.optInt("rotationIntervalSeconds", 10)
                    val isActive = obj.optBoolean("isActive", true)

                    var ad: AppAd? = null
                    if (obj.has("ad") && !obj.isNull("ad")) {
                        val adObj = obj.getJSONObject("ad")
                        val bannerUrl = when {
                            adObj.has("bannerImageUrl") && !adObj.isNull("bannerImageUrl") -> adObj.optString("bannerImageUrl")
                            adObj.has("banner_image_url") && !adObj.isNull("banner_image_url") -> adObj.optString("banner_image_url")
                            else -> ""
                        }
                        val destUrl = when {
                            adObj.has("destinationUrl") && !adObj.isNull("destinationUrl") -> adObj.optString("destinationUrl")
                            adObj.has("destination_url") && !adObj.isNull("destination_url") -> adObj.optString("destination_url")
                            else -> ""
                        }
                        val advName = when {
                            adObj.has("advertiserName") && !adObj.isNull("advertiserName") -> adObj.optString("advertiserName")
                            adObj.has("advertiser_name") && !adObj.isNull("advertiser_name") -> adObj.optString("advertiser_name")
                            else -> ""
                        }
                        val cat = when {
                            adObj.has("category") && !adObj.isNull("category") -> adObj.optString("category")
                            adObj.has("target_category") && !adObj.isNull("target_category") -> adObj.optString("target_category")
                            else -> "All Services"
                        }
                        ad = AppAd(
                            id = adObj.optString("id"),
                            name = adObj.optString("name"),
                            advertiserName = advName,
                            bannerImageUrl = bannerUrl,
                            destinationUrl = destUrl,
                            targetCategory = cat
                        )
                    }

                    slotsList.add(
                        PlacementSlot(
                            slotId = slotId,
                            name = name,
                            screenName = screenName,
                            heightDp = heightDp,
                            widthDp = widthDp,
                            aspectRatio = aspectRatio,
                            rotationIntervalSeconds = rotationIntervalSeconds,
                            isActive = isActive,
                            ad = ad
                        )
                    )
                }

                Result.success(slotsList)
            } else {
                val errText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.w(TAG, "Failed to fetch placements: HTTP $responseCode - $errText")
                Result.failure(Exception("HTTP $responseCode: $errText"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching placements from $endpoint", e)
            Result.failure(e)
        }
    }

    /**
     * Reports an advertisement impression telemetry event
     */
    suspend fun recordImpression(adId: String, slotId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (adId.isBlank()) return@withContext Result.success(false)
        val endpoint = "${AdConfig.BASE_URL.trimEnd('/')}/api/v1/ads/$adId/impression"
        sendEvent(endpoint, slotId)
    }

    /**
     * Reports an advertisement click telemetry event
     */
    suspend fun recordClick(adId: String, slotId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (adId.isBlank()) return@withContext Result.success(false)
        val endpoint = "${AdConfig.BASE_URL.trimEnd('/')}/api/v1/ads/$adId/click"
        sendEvent(endpoint, slotId)
    }

    private fun sendEvent(endpoint: String, slotId: String): Result<Boolean> {
        return try {
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "GlipGo-Android-App/1.0")
            }

            val payload = JSONObject().apply {
                put("placement", slotId)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val code = connection.responseCode
            Result.success(code in 200..299)
        } catch (e: Exception) {
            Log.w(TAG, "Telemetry event failure for $endpoint", e)
            Result.failure(e)
        }
    }
}
