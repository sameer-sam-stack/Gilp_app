package com.glip.app.domain.model

enum class VehicleCategory {
    AUTO_RICKSHAW,
    MINI_CAR,
    SEDAN,
    SUV,
    VAN,
    E_RICKSHAW
}

enum class RouteStatus {
    SCHEDULED,
    EN_ROUTE,
    COMPLETED,
    PAUSED
}

enum class ServiceType {
    RIDE_POOLING,
    PARCEL_DELIVERY,
    SCHOOL_TRANSPORT,
    LOCAL_SHOPS
}

data class RouteStop(
    val stopId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val sequenceOrder: Int,
    val estimatedArrivalMinutes: Int
)

data class Route(
    val routeId: String,
    val originName: String,
    val destinationName: String,
    val originLat: Double,
    val originLng: Double,
    val destinationLat: Double,
    val destinationLng: Double,
    val polylinePoints: List<Pair<Double, Double>>,
    val stops: List<RouteStop> = emptyList(),
    val totalDistanceKm: Double,
    val estimatedDurationMinutes: Int
)

data class SeatAvailability(
    val totalSeats: Int,
    val occupiedSeats: Int
) {
    val availableSeats: Int get() = (totalSeats - occupiedSeats).coerceAtLeast(0)
    val isFull: Boolean get() = availableSeats <= 0
}

data class DriverProfile(
    val driverId: String,
    val fullName: String,
    val rating: Double,
    val totalRidesCompleted: Int,
    val isVerified: Boolean
)

data class Vehicle(
    val vehicleId: String,
    val licensePlateMasked: String, // e.g. "DL 01 ** 3456" for privacy
    val category: VehicleCategory,
    val modelName: String,
    val currentLat: Double,
    val currentLng: Double,
    val headingDegrees: Float = 0f,
    val route: Route,
    val routeStatus: RouteStatus,
    val seatAvailability: SeatAvailability,
    val driver: DriverProfile,
    val farePerKmEstimate: Double,
    val isDemoSimulated: Boolean = true,
    val routeColorHex: String = "#10B981",
    val etaMinutes: Int = 0,
    val driverPhone: String = "",
    val roadPolylinePoints: List<Pair<Double, Double>> = emptyList()
)

data class UserProfile(
    val userId: String,
    val fullName: String,
    val phoneNumber: String, // E.164 or 10-digit Indian number
    val email: String? = null,
    val createdAtTimestamp: Long = System.currentTimeMillis()
)

data class LocalPromotion(
    val id: String,
    val title: String,
    val businessName: String,
    val category: String,
    val discountDescription: String,
    val distanceDescription: String,
    val validUntil: String,
    val terms: String,
    val isDemoPromotion: Boolean = true
)
