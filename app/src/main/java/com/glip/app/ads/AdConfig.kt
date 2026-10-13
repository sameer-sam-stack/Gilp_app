package com.glip.app.ads

/**
 * Advertisement System Configuration for Glip_go Android App
 */
object AdConfig {
    /**
     * Base URL for the live Advertisement Management System API.
     * Can point to production Vercel deployment or custom domain.
     */
    var BASE_URL: String = "https://advertisement-management-system.vercel.app"

    // Slot IDs matching Glip_go in-app containers
    const val SLOT_FIRST_SCREEN_TOP_BANNER = "first_screen_top_banner"
    const val SLOT_FIRST_SCREEN_BOTTOM_STRIP = "first_screen_bottom_strip"
    const val SLOT_HOME_TOP_BANNER = "home_top_banner"
    const val SLOT_HOME_VEHICLE_TRAILING_CARD = "home_vehicle_trailing_card"
    const val SLOT_SERVICES_PROMO_BANNER = "services_promo_banner"

    // Default polling refresh interval (60 seconds)
    const val REFRESH_INTERVAL_MILLIS: Long = 60_000L
}
