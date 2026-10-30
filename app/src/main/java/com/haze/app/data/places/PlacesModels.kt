package com.haze.app.data.places

import java.util.UUID

/**
 * Autocomplete place suggestion model for nearby location search.
 */
data class AutocompletePlace(
    val id: String = UUID.randomUUID().toString(),
    val primaryText: String,
    val secondaryText: String,
    val fullAddress: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Float? = null,
    val distanceFormatted: String? = null
)
