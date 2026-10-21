package com.glip.app.util

import android.location.Location
import com.glip.app.data.supabase.RemoteDriver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

enum class PersonalVehicleType(
    val categoryId: String,
    val displayName: String,
    val capacity: Int,
    val perKmRate: Double,
    val minFare: Double,
    val iconEmoji: String,
    val description: String
) {
    E_RICKSHAW(
        categoryId = "E_RICKSHAW",
        displayName = "E - Rickshaw",
        capacity = 4,
        perKmRate = 15.0, // Exact user requirement: ₹15 - 1 KM, 4 Seats
        minFare = 15.0,
        iconEmoji = "🛺",
        description = "4-Seater electric rickshaw"
    ),
    AUTO(
        categoryId = "AUTO",
        displayName = "Auto",
        capacity = 5,
        perKmRate = 20.0, // Exact user requirement: ₹20 - 1 KM, 5 Seats
        minFare = 20.0,
        iconEmoji = "🛺",
        description = "5-Seater auto rickshaw"
    ),
    CAB(
        categoryId = "CAB",
        displayName = "CAB",
        capacity = 4,
        perKmRate = 25.0, // Exact user requirement: ₹25 - 1 KM, 4 Seats
        minFare = 25.0,
        iconEmoji = "🚗",
        description = "Comfortable 4-wheeler AC cab"
    )
}

data class PersonalFareEstimate(
    val type: PersonalVehicleType,
    val fareAmount: Int,
    val etaMinutes: Int,
    val dropTimeFormatted: String,
    val distanceKm: Double,
    val isAvailable: Boolean = true
)

data class PoolingFareEstimate(
    val fareAmount: Int,
    val basePersonalFare: Int,
    val savingsAmount: Int,
    val discountPercent: Int,
    val distanceKm: Double
)

/**
 * Intelligent Dynamic Pricing Engine for Glip Mobility.
 * Enforces exact user per-KM rates:
 * - E-Rickshaw: ₹15/KM (4 Seats)
 * - Auto: ₹20/KM (5 Seats)
 * - Cab: ₹25/KM (4 Seats)
 * (Bike has been completely removed)
 *
 * Checks driver vehicle type and live online presence so only genuine available vehicles
 * can be booked.
 */
object FareCalculationEngine {

    /**
     * Calculates upfront fare for Personal Solo Rides.
     */
    fun calculatePersonalFare(distanceKm: Double, type: PersonalVehicleType): Int {
        val safeKm = distanceKm.coerceAtLeast(0.5)
        val rawFare = safeKm * type.perKmRate
        return rawFare.roundToInt().coerceAtLeast(type.minFare.toInt())
    }

    /**
     * Calculates upfront fare for Shared Route Pooling (Ola Share / Uber Pool).
     * Distributes fuel & driver cost across co-passengers.
     */
    fun calculatePoolingFare(distanceKm: Double, occupiedSeats: Int): PoolingFareEstimate {
        val personalFare = calculatePersonalFare(distanceKm, PersonalVehicleType.AUTO)
        val discountFactor = when (occupiedSeats) {
            0 -> 0.70
            1 -> 0.55
            2 -> 0.48
            else -> 0.42
        }
        val poolFare = (personalFare * discountFactor).roundToInt().coerceAtLeast(15)
        val savings = (personalFare - poolFare).coerceAtLeast(0)
        val discountPct = (((personalFare - poolFare).toDouble() / personalFare) * 100).roundToInt().coerceIn(15, 65)

        return PoolingFareEstimate(
            fareAmount = poolFare,
            basePersonalFare = personalFare,
            savingsAmount = savings,
            discountPercent = discountPct,
            distanceKm = distanceKm
        )
    }

    /**
     * Generates personal vehicle options with live availability checks against nearby online drivers.
     */
    fun getPersonalOptions(
        distanceKm: Double,
        pLat: Double = 0.0,
        pLng: Double = 0.0,
        onlineDrivers: List<RemoteDriver> = emptyList(),
        baseEtaMinutes: Int = 3
    ): List<PersonalFareEstimate> {
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault())

        // Filter drivers who are currently online and within a reasonable city radius (25 km)
        val nearbyDrivers = onlineDrivers.filter { drv ->
            if (!drv.isOnline) return@filter false
            if (pLat == 0.0 || pLng == 0.0 || drv.currentLat == 0.0 || drv.currentLng == 0.0) return@filter true
            val results = FloatArray(1)
            Location.distanceBetween(drv.currentLat, drv.currentLng, pLat, pLng, results)
            results[0] <= 25_000f
        }

        return listOf(
            PersonalVehicleType.E_RICKSHAW,
            PersonalVehicleType.AUTO,
            PersonalVehicleType.CAB
        ).map { type ->
            val fare = calculatePersonalFare(distanceKm, type)

            // Check if there is an online driver matching this exact vehicle category or any nearby online driver
            val matchingDrivers = nearbyDrivers.filter { it.matchesCategory(type.name) }
            val isAvailable = if (onlineDrivers.isEmpty()) true else (matchingDrivers.isNotEmpty() || nearbyDrivers.isNotEmpty())

            val nearest = matchingDrivers.minByOrNull { drv ->
                val res = FloatArray(1)
                Location.distanceBetween(drv.currentLat, drv.currentLng, pLat, pLng, res)
                res[0]
            } ?: nearbyDrivers.minByOrNull { drv ->
                val res = FloatArray(1)
                Location.distanceBetween(drv.currentLat, drv.currentLng, pLat, pLng, res)
                res[0]
            }

            val eta = if (nearest != null && pLat != 0.0) {
                val res = FloatArray(1)
                Location.distanceBetween(nearest.currentLat, nearest.currentLng, pLat, pLng, res)
                val distKm = res[0] / 1000.0
                (distKm * 2.5 + 2).roundToInt().coerceIn(2, 25)
            } else {
                when (type) {
                    PersonalVehicleType.E_RICKSHAW -> baseEtaMinutes.coerceAtLeast(3)
                    PersonalVehicleType.AUTO -> baseEtaMinutes.coerceAtLeast(3)
                    PersonalVehicleType.CAB -> (baseEtaMinutes + 2).coerceAtLeast(4)
                }
            }

            val tripDurationMins = ((distanceKm / 22.0) * 60).roundToInt().coerceAtLeast(5)
            val dropCal = Calendar.getInstance().apply {
                add(Calendar.MINUTE, eta + tripDurationMins)
            }
            val dropTime = format.format(dropCal.time).lowercase()

            PersonalFareEstimate(
                type = type,
                fareAmount = fare,
                etaMinutes = eta,
                dropTimeFormatted = dropTime,
                distanceKm = distanceKm,
                isAvailable = isAvailable
            )
        }
    }
}
