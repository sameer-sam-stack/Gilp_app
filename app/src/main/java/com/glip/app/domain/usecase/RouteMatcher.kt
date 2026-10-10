package com.glip.app.domain.usecase

import com.glip.app.domain.model.Vehicle

class RouteMatcher {
    /**
     * Matches vehicles whose existing route or intermediate stops match
     * the requested pickup and drop location query keywords.
     * Also checks seat availability.
     */
    fun findCompatibleVehicles(
        vehicles: List<Vehicle>,
        pickupQuery: String,
        dropQuery: String,
        requiredSeats: Int = 1
    ): List<Vehicle> {
        val cleanPickup = pickupQuery.trim().lowercase()
        val cleanDrop = dropQuery.trim().lowercase()

        return vehicles.filter { vehicle ->
            // Must have available seats
            val hasSeats = vehicle.seatAvailability.availableSeats >= requiredSeats
            if (!hasSeats) return@filter false

            val route = vehicle.route

            val matchesPickup = cleanPickup.isEmpty() ||
                route.originName.lowercase().contains(cleanPickup) ||
                route.stops.any { it.name.lowercase().contains(cleanPickup) }

            val matchesDrop = cleanDrop.isEmpty() ||
                route.destinationName.lowercase().contains(cleanDrop) ||
                route.stops.any { it.name.lowercase().contains(cleanDrop) }

            matchesPickup && matchesDrop
        }
    }
}
