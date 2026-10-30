package com.haze.app.util

import android.location.Location

/**
 * Industrial-strength Route Corridor Matching Engine (UberX Share / Rapido Pooling style).
 * Mathematically determines whether a passenger's pickup and dropoff lie within a driver's travel corridor
 * AND strictly verifies forward direction alignment (vector bearing, distance progression, and destination reach).
 */
object CorridorMatchingHelper {

    /**
     * Checks if passenger pickup and dropoff lie along the driver's route corridor in the same direction.
     *
     * @param pLat Passenger pickup latitude
     * @param pLng Passenger pickup longitude
     * @param dLat Passenger dropoff latitude
     * @param dLng Passenger dropoff longitude
     * @param driverLat Driver's current latitude (origin of current trip)
     * @param driverLng Driver's current longitude
     * @param destLat Driver's destination latitude
     * @param destLng Driver's destination longitude
     * @param routePoints Driver's full road polyline points (lat, lng) from current location to destination
     * @param maxCorridorMeters Maximum allowed detour distance from passenger point to driver's route (default 400m)
     */
    fun isPassengerOnDriverCorridor(
        pLat: Double,
        pLng: Double,
        dLat: Double,
        dLng: Double,
        driverLat: Double = 0.0,
        driverLng: Double = 0.0,
        destLat: Double = 0.0,
        destLng: Double = 0.0,
        routePoints: List<Pair<Double, Double>>,
        maxCorridorMeters: Double = 1200.0
    ): Boolean {
        // If passenger hasn't set search coordinates yet, allow showing nearby vehicles
        if (pLat == 0.0 || pLng == 0.0 || dLat == 0.0 || dLng == 0.0) return true
        if (routePoints.size < 2) return false

        val dStartLat = if (driverLat != 0.0) driverLat else routePoints.first().first
        val dStartLng = if (driverLng != 0.0) driverLng else routePoints.first().second
        val dEndLat = if (destLat != 0.0) destLat else routePoints.last().first
        val dEndLng = if (destLng != 0.0) destLng else routePoints.last().second

        // =========================================================================
        // LAYER 1: STRICT BEARING / HEADING VECTOR ALIGNMENT
        // Passenger vector (Pickup -> Dropoff) vs Driver vector (Current -> Destination)
        // =========================================================================
        val passBearing = calculateBearing(pLat, pLng, dLat, dLng)
        val driverBearing = calculateBearing(dStartLat, dStartLng, dEndLat, dEndLng)
        val bearingDiff = angleDifference(passBearing, driverBearing)

        // Strict angular threshold: Must be traveling within 70 degrees of each other.
        // If passenger is going A->B (e.g. 45°) and driver is going B->A (225°),
        // bearingDiff is 180° -> immediately rejected!
        if (bearingDiff > 70.0) {
            return false
        }

        // =========================================================================
        // LAYER 2: DISTANCE PROGRESSION CHECK
        // Driver must be closer to passenger pickup than to passenger dropoff.
        // =========================================================================
        val distDriverToPickup = distanceMeters(dStartLat, dStartLng, pLat, pLng)
        val distDriverToDrop = distanceMeters(dStartLat, dStartLng, dLat, dLng)
        val distPassengerTrip = distanceMeters(pLat, pLng, dLat, dLng)

        // If driver is already closer to dropoff than to pickup, driver is moving backwards
        // or has already passed the dropoff point.
        if (distPassengerTrip > 800.0 && (distDriverToDrop + 400.0 < distDriverToPickup)) {
            return false
        }

        // =========================================================================
        // LAYER 3: DRIVER DESTINATION FORWARD ALIGNMENT
        // Driver's destination must be forward of passenger's pickup (not terminating at or before pickup).
        // =========================================================================
        val distDriverDestToPickup = distanceMeters(dEndLat, dEndLng, pLat, pLng)
        val distDriverDestToDrop = distanceMeters(dEndLat, dEndLng, dLat, dLng)

        if (distPassengerTrip > 800.0 && (distDriverDestToPickup + 300.0 < distDriverDestToDrop)) {
            return false // Driver's destination terminates significantly before passenger dropoff
        }

        // =========================================================================
        // LAYER 4: DETAILED POLYLINE SEGMENT PROJECTION & CORRIDOR BUFFER
        // =========================================================================
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

        // 1. Both pickup and dropoff MUST be within the corridor buffer (1000m)
        if (minPickupDist > maxCorridorMeters || minDropDist > maxCorridorMeters) {
            return false
        }

        // 2. Sequential Order: Driver must pass passenger's pickup BEFORE reaching passenger's dropoff
        if (pickupSegmentIndex > dropSegmentIndex) {
            return false // Driver reaches dropoff before pickup (wrong direction)
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

    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val y = Math.sin(deltaLambda) * Math.cos(phi2)
        val x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(deltaLambda)
        val theta = Math.atan2(y, x)
        return (Math.toDegrees(theta) + 360.0) % 360.0
    }

    fun angleDifference(bearing1: Double, bearing2: Double): Double {
        val diff = Math.abs(bearing1 - bearing2) % 360.0
        return if (diff > 180.0) 360.0 - diff else diff
    }

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
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
