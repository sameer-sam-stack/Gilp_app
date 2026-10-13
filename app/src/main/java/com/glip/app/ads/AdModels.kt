package com.glip.app.ads

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
)

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
