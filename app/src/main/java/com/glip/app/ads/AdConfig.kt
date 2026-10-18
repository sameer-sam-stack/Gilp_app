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

    // 2 Active Fixed-Size (250dp) In-App Ad Containers
    const val SLOT_HOME_TOP_BANNER = "home_top_banner" // HomeScreen: Top Hero Banner (Fixed 250dp, Shape 12dp, 1080x750px)
    const val SLOT_SERVICES_BOTTOM_BANNER = "services_bottom_banner" // ServicesScreen: Bottom Ad Banner (Fixed 250dp, Shape 10dp, 1080x750px)

    // Ultra-fast polling refresh interval (3.5 seconds for instant live ad updates)
    const val REFRESH_INTERVAL_MILLIS: Long = 3_500L
}
