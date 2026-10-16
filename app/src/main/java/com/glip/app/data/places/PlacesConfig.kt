package com.glip.app.data.places

/**
 * Configuration for Nearby Places & Autocomplete Provider.
 * Supports:
 * 1. Google Places API (if apiKey is supplied)
 * 2. High-speed Photon & OpenStreetMap Location-Biased Autocomplete (Default, zero key needed)
 */
object PlacesConfig {
    /**
     * Optional Google Places API Key.
     * Can be set at runtime or loaded from environment / BuildConfig.
     * If blank, the app uses real-time Photon & OSM Location-Biased API.
     */
    var googlePlacesApiKey: String = ""

    /**
     * Nearby search radius in meters around the user's current GPS location (default 50 km).
     */
    const val DEFAULT_SEARCH_RADIUS_METERS: Int = 50_000

    /**
     * Debounce duration in milliseconds before triggering search.
     */
    const val DEBOUNCE_MILLIS: Long = 300L

    /**
     * Max suggestions to display in the dropdown.
     */
    const val MAX_SUGGESTIONS: Int = 7
}
