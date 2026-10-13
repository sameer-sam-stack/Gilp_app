package com.glip.app.ads

/**
 * Supported Advertisement Creative Types
 */
enum class AdMediaType {
    IMAGE,
    GIF,
    VIDEO
}

/**
 * Advertisement data entity received from API
 */
data class AppAd(
    val id: String,
    val name: String,
    val advertiserName: String,
    val bannerImageUrl: String,
    val destinationUrl: String,
    val targetCategory: String = "All Services"
) {
    /**
     * Determines whether the ad creative is a Video, Animated GIF, or Static Image
     */
    fun getMediaType(): AdMediaType {
        val lower = bannerImageUrl.lowercase()
        return when {
            lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mov") || lower.contains("/video") -> AdMediaType.VIDEO
            lower.endsWith(".gif") || lower.contains("image/gif") -> AdMediaType.GIF
            else -> AdMediaType.IMAGE
        }
    }
}

/**
 * In-app placement container configuration
 */
data class PlacementSlot(
    val slotId: String,
    val name: String,
    val screenName: String,
    val heightDp: Int,
    val widthDp: Int? = null,
    val aspectRatio: String = "",
    val rotationIntervalSeconds: Int = 10,
    val isActive: Boolean = true,
    val ad: AppAd? = null
)
