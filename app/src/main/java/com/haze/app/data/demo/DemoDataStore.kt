package com.haze.app.data.demo

import com.haze.app.domain.model.DriverProfile
import com.haze.app.domain.model.LocalPromotion
import com.haze.app.domain.model.Route
import com.haze.app.domain.model.RouteStatus
import com.haze.app.domain.model.RouteStop
import com.haze.app.domain.model.SeatAvailability
import com.haze.app.domain.model.Vehicle
import com.haze.app.domain.model.VehicleCategory

object DemoDataStore {

    // Demo Center: Connaught Place, New Delhi (28.6315, 77.2167)
    const val DEMO_CENTER_LAT = 28.6315
    const val DEMO_CENTER_LNG = 77.2167

    val demoRoutes = listOf(
        Route(
            routeId = "route-001",
            originName = "Connaught Place",
            destinationName = "Cyber Hub Gurugram",
            originLat = 28.6315,
            originLng = 77.2167,
            destinationLat = 28.4950,
            destinationLng = 77.0895,
            totalDistanceKm = 24.5,
            estimatedDurationMinutes = 45,
            polylinePoints = listOf(
                Pair(28.6315, 77.2167),
                Pair(28.6139, 77.2090),
                Pair(28.5800, 77.1650),
                Pair(28.5280, 77.1200),
                Pair(28.4950, 77.0895)
            ),
            stops = listOf(
                RouteStop("s1", "Dhaula Kuan", 28.5921, 77.1610, 1, 12),
                RouteStop("s2", "Mahipalpur Bypass", 28.5440, 77.1280, 2, 22),
                RouteStop("s3", "Ambience Mall", 28.5042, 77.0970, 3, 35)
            )
        ),
        Route(
            routeId = "route-002",
            originName = "Karol Bagh Metro",
            destinationName = "Noida Sector 62",
            originLat = 28.6515,
            originLng = 77.1905,
            destinationLat = 28.6280,
            destinationLng = 77.3648,
            totalDistanceKm = 21.0,
            estimatedDurationMinutes = 50,
            polylinePoints = listOf(
                Pair(28.6515, 77.1905),
                Pair(28.6350, 77.2250),
                Pair(28.6180, 77.2780),
                Pair(28.6280, 77.3648)
            ),
            stops = listOf(
                RouteStop("s4", "ITO Junction", 28.6289, 77.2405, 1, 15),
                RouteStop("s5", "Akshardham", 28.6127, 77.2773, 2, 25),
                RouteStop("s6", "Indirapuram Crossing", 28.6350, 77.3450, 3, 40)
            )
        ),
        Route(
            routeId = "route-003",
            originName = "Saket District Centre",
            destinationName = "Nehru Place",
            originLat = 28.5244,
            originLng = 77.2177,
            destinationLat = 28.5494,
            destinationLng = 77.2520,
            totalDistanceKm = 6.8,
            estimatedDurationMinutes = 20,
            polylinePoints = listOf(
                Pair(28.5244, 77.2177),
                Pair(28.5360, 77.2340),
                Pair(28.5494, 77.2520)
            ),
            stops = listOf(
                RouteStop("s7", "Chirag Dilli", 28.5390, 77.2290, 1, 8),
                RouteStop("s8", "Greater Kailash 1", 28.5460, 77.2410, 2, 14)
            )
        ),
        Route(
            routeId = "route-004",
            originName = "Rohini West",
            destinationName = "Rajouri Garden",
            originLat = 28.7144,
            originLng = 77.1147,
            destinationLat = 28.6492,
            destinationLng = 77.1220,
            totalDistanceKm = 11.2,
            estimatedDurationMinutes = 30,
            polylinePoints = listOf(
                Pair(28.7144, 77.1147),
                Pair(28.6820, 77.1180),
                Pair(28.6492, 77.1220)
            ),
            stops = listOf(
                RouteStop("s9", "Pitampura", 28.6990, 77.1320, 1, 10),
                RouteStop("s10", "Punjabi Bagh", 28.6690, 77.1240, 2, 20)
            )
        )
    )

    // Mock demo vehicles removed; real-time vehicles will be populated via Driver App connection
    val demoVehicles: List<Vehicle> = emptyList()

    val demoPromotions = listOf(
        LocalPromotion(
            id = "promo-001",
            title = "15% Off Fresh Organic Groceries",
            businessName = "Apna Kirana & Fresh Mart",
            category = "Daily Groceries",
            discountDescription = "Flat 15% discount on orders above ₹499",
            distanceDescription = "350 m away along Dhaula Kuan route",
            validUntil = "Valid till 30th of this month",
            terms = "Show Glip demo token at the billing counter. Demo partner listing."
        ),
        LocalPromotion(
            id = "promo-002",
            title = "Free Kadak Chai with Samosa Plate",
            businessName = "Sharma Sweets & Chaat Corner",
            category = "Food & Snacks",
            discountDescription = "Complimentary hot tea with morning snack platter",
            distanceDescription = "500 m away near Karol Bagh stop",
            validUntil = "Daily 7:00 AM – 11:00 AM",
            terms = "Valid for in-store dining. Not redeemable for cash."
        ),
        LocalPromotion(
            id = "promo-003",
            title = "10% Discount on School Stationery & Books",
            businessName = "Vidyarthi Book Depot & Supplies",
            category = "Stationery & Education",
            discountDescription = "10% off on all NCERT guides, notebook bundles and pens",
            distanceDescription = "1.2 km away near Saket sector",
            validUntil = "Ongoing academic season",
            terms = "Applicable on cash and UPI payments."
        ),
        LocalPromotion(
            id = "promo-004",
            title = "Fast Phone Screen Guard & Back Cover ₹99",
            businessName = "Speed Mobile Care & Accessories",
            category = "Electronics Care",
            discountDescription = "Tempered glass installation and shockproof case bundle",
            distanceDescription = "800 m away near Nehru Place transit hub",
            validUntil = "Limited demo stock",
            terms = "Valid across standard smartphone models."
        )
    )
}
