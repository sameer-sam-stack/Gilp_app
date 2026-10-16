package com.glip.app.util

import android.location.Location

/**
 * Industrial-strength Route Corridor Matching Engine (UberX Share / Rapido Pooling style).
 * Mathematically determines whether a passenger's pickup and dropoff lie within a driver's travel corridor.
 */
object CorridorMatchingHelper {

    /**
     * Checks if passenger pickup and dropoff lie along the driver's route corridor in the same direction.
     *
     * @param pLat Passenger pickup latitude
     * @param pLng Passenger pickup longitude
     * @param dLat Passenger dropoff latitude
     * @param dLng Passenger dropoff longitude
     * @param routePoints Driver's full road polyline points (lat, lng) from current location to destination
     * @param maxCorridorMeters Maximum allowed detour distance from passenger point to driver's route (default 2200m)
     */
    fun isPassengerOnDriverCorridor(
        pLat: Double,
        pLng: Double,
        dLat: Double,
        dLng: Double,
        routePoints: List<Pair<Double, Double>>,
        maxCorridorMeters: Double = 2200.0
    ): Boolean {
        // If passenger hasn't set search coordinates yet, allow showing nearby vehicles
        if (pLat == 0.0 || pLng == 0.0 || dLat == 0.0 || dLng == 0.0) return true
        if (routePoints.size < 2) return false

        var minPickupDist = Double.MAX_VALUE
        var pickupSegmentIndex = -1

        var minDropDist = Double.MAX_VALUE
        var dropSegmentIndex = -1

        for (i in 0 until routePoints.size - 1) {
            val a = routePoints[i]
            val b = routePoints[i + 1]

            val distToPickup = distanceToSegmentMeters(pLat, pLng, a.first, a.second, b.first, b.second)
            if (distToPickup < minPickupDist) {
                minPickupDist = distToPickup
                pickupSegmentIndex = i
            }

            val distToDrop = distanceToSegmentMeters(dLat, dLng, a.first, a.second, b.first, b.second)
            if (distToDrop < minDropDist) {
                minDropDist = distToDrop
                dropSegmentIndex = i
            }
        }

        // 1. Both pickup and dropoff MUST be within the corridor buffer
        if (minPickupDist > maxCorridorMeters || minDropDist > maxCorridorMeters) {
            return false
        }

        // 2. Sequential Order: Driver must pass passenger's pickup BEFORE reaching passenger's dropoff
        if (pickupSegmentIndex > dropSegmentIndex) {
            return false // Driver is heading in the opposite direction
        }

        // 3. If within the same road segment, ensure linear projection order is forward
        if (pickupSegmentIndex == dropSegmentIndex) {
            val a = routePoints[pickupSegmentIndex]
            val b = routePoints[pickupSegmentIndex + 1]
            val projP = projectOnSegment(pLat, pLng, a.first, a.second, b.first, b.second)
            val projD = projectOnSegment(dLat, dLng, a.first, a.second, b.first, b.second)
            if (projP > projD) return false
        }

        return true
    }

    private fun distanceToSegmentMeters(
        pLat: Double, pLng: Double,
        aLat: Double, aLng: Double,
        bLat: Double, bLng: Double
    ): Double {
        val t = projectOnSegment(pLat, pLng, aLat, aLng, bLat, bLng)
        val projLat = aLat + t * (bLat - aLat)
        val projLng = aLng + t * (bLng - aLng)

        val res = FloatArray(1)
        Location.distanceBetween(pLat, pLng, projLat, projLng, res)
        return res[0].toDouble()
    }

    private fun projectOnSegment(
        pLat: Double, pLng: Double,
        aLat: Double, aLng: Double,
        bLat: Double, bLng: Double
    ): Double {
        val dx = bLng - aLng
        val dy = bLat - aLat
        val lenSq = dx * dx + dy * dy
        if (lenSq == 0.0) return 0.0
        val t = ((pLng - aLng) * dx + (pLat - aLat) * dy) / lenSq
        return t.coerceIn(0.0, 1.0)
    }
}
