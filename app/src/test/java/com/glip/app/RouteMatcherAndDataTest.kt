package com.glip.app

import com.glip.app.data.demo.DemoDataStore
import com.glip.app.domain.model.DriverProfile
import com.glip.app.domain.model.RouteStatus
import com.glip.app.domain.model.SeatAvailability
import com.glip.app.domain.model.Vehicle
import com.glip.app.domain.model.VehicleCategory
import com.glip.app.domain.usecase.RouteMatcher
import org.junit.Assert.*
import org.junit.Test

class RouteMatcherAndDataTest {

    private val routeMatcher = RouteMatcher()

    // Test vehicles fixtures to validate route matching independently
    private val testVehicles = listOf(
        Vehicle(
            vehicleId = "GLIP-V-101",
            licensePlateMasked = "DL 01 ** 4812",
            category = VehicleCategory.SEDAN,
            modelName = "Maruti Suzuki Dzire",
            currentLat = 28.6250,
            currentLng = 77.2120,
            headingDegrees = 45f,
            route = DemoDataStore.demoRoutes[0],
            routeStatus = RouteStatus.EN_ROUTE,
            seatAvailability = SeatAvailability(totalSeats = 4, occupiedSeats = 1),
            driver = DriverProfile(
                driverId = "DRV-001",
                fullName = "Rajesh Sharma",
                rating = 4.85,
                totalRidesCompleted = 340,
                isVerified = true
            ),
            farePerKmEstimate = 8.5
        )
    )

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
            vehicles = testVehicles,
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
            vehicles = testVehicles,
            pickupQuery = "Dhaula Kuan",
            dropQuery = "Cyber Hub"
        )

        assertTrue(matches.isNotEmpty())
        assertEquals("GLIP-V-101", matches.first().vehicleId)
    }

    @Test
    fun testRouteMatcherReturnsEmptyForIncompatibleRoute() {
        val matches = routeMatcher.findCompatibleVehicles(
            vehicles = testVehicles,
            pickupQuery = "NonExistentPlace",
            dropQuery = "UnknownDestination"
        )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun testDemoVehiclesReadyForLiveDriverConnection() {
        // demoVehicles is intentionally empty in preparation for live driver app integration
        assertTrue(DemoDataStore.demoVehicles.isEmpty())
    }

    @Test
    fun testVehicleDataIntegrity() {
        testVehicles.forEach { vehicle ->
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
