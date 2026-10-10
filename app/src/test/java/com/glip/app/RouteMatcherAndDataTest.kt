package com.glip.app

import com.glip.app.data.demo.DemoDataStore
import com.glip.app.domain.model.SeatAvailability
import com.glip.app.domain.usecase.RouteMatcher
import org.junit.Assert.*
import org.junit.Test

class RouteMatcherAndDataTest {

    private val routeMatcher = RouteMatcher()

    @Test
    fun testSeatAvailabilityCalculations() {
        val seats1 = SeatAvailability(totalSeats = 4, occupiedSeats = 1)
        assertEquals(3, seats1.availableSeats)
        assertFalse(seats1.isFull)

        val seatsFull = SeatAvailability(totalSeats = 4, occupiedSeats = 4)
        assertEquals(0, seatsFull.availableSeats)
        assertTrue(seatsFull.isFull)

        // Edge case: overflow
        val seatsOver = SeatAvailability(totalSeats = 3, occupiedSeats = 5)
        assertEquals(0, seatsOver.availableSeats)
        assertTrue(seatsOver.isFull)
    }

    @Test
    fun testRouteMatcherFindsConnaughtPlaceToCyberHub() {
        val matches = routeMatcher.findCompatibleVehicles(
            vehicles = DemoDataStore.demoVehicles,
            pickupQuery = "Connaught",
            dropQuery = "Cyber Hub"
        )

        assertTrue(matches.isNotEmpty())
        assertEquals("GLIP-V-101", matches.first().vehicleId)
        assertEquals("Connaught Place", matches.first().route.originName)
    }

    @Test
    fun testRouteMatcherFindsIntermediateStops() {
        // Stop "Dhaula Kuan" is on Route 001
        val matches = routeMatcher.findCompatibleVehicles(
            vehicles = DemoDataStore.demoVehicles,
            pickupQuery = "Dhaula Kuan",
            dropQuery = "Cyber Hub"
        )

        assertTrue(matches.isNotEmpty())
        assertEquals("GLIP-V-101", matches.first().vehicleId)
    }

    @Test
    fun testRouteMatcherReturnsEmptyForIncompatibleRoute() {
        val matches = routeMatcher.findCompatibleVehicles(
            vehicles = DemoDataStore.demoVehicles,
            pickupQuery = "NonExistentPlace",
            dropQuery = "UnknownDestination"
        )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun testDemoVehiclesDataIntegrity() {
        assertTrue(DemoDataStore.demoVehicles.isNotEmpty())
        DemoDataStore.demoVehicles.forEach { vehicle ->
            assertNotNull(vehicle.vehicleId)
            assertTrue(vehicle.vehicleId.startsWith("GLIP-V-"))
            assertTrue(vehicle.driver.rating in 1.0..5.0)
            assertTrue(vehicle.seatAvailability.totalSeats > 0)
            assertTrue(vehicle.farePerKmEstimate > 0.0)
        }
    }

    @Test
    fun testDemoPromotionsDataIntegrity() {
        assertTrue(DemoDataStore.demoPromotions.isNotEmpty())
        DemoDataStore.demoPromotions.forEach { promo ->
            assertNotNull(promo.id)
            assertTrue(promo.title.isNotBlank())
            assertTrue(promo.businessName.isNotBlank())
            assertTrue(promo.isDemoPromotion)
        }
    }
}
