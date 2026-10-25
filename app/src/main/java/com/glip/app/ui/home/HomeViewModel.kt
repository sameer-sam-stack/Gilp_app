package com.glip.app.ui.home

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glip.app.data.supabase.RemoteDriver
import com.glip.app.data.supabase.RemoteDriverRoute
import com.glip.app.data.supabase.RideRequestClient
import com.glip.app.data.supabase.RideStatusResult
import com.glip.app.domain.model.*
import com.glip.app.domain.repository.MobilityRepository
import com.glip.app.util.CorridorMatchingHelper
import com.glip.app.util.FareCalculationEngine
import com.glip.app.util.OsrmRoutingHelper
import com.glip.app.util.PersonalFareEstimate
import com.glip.app.util.PersonalVehicleType
import com.glip.app.util.PoolingFareEstimate
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

data class HomeUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val filteredVehicles: List<Vehicle> = emptyList(),
    val selectedVehicle: Vehicle? = null,
    val pickupQuery: String = "",
    val dropQuery: String = "",
    val pickupLat: Double = 0.0,
    val pickupLng: Double = 0.0,
    val dropLat: Double = 0.0,
    val dropLng: Double = 0.0,
    val isLoading: Boolean = false,
    val isMapInitialized: Boolean = true,
    val isFallbackListVisible: Boolean = false,
    val activeRequestId: String? = null,
    val rideBookingStatus: String = "NONE", // NONE, REQUESTED, ACCEPTED, IN_PROGRESS, CANCELLED
    val acceptedVehicle: Vehicle? = null,
    val completedDriver: Vehicle? = null,
    val passengerRoutePoints: List<GeoPoint> = emptyList(),
    val bookingError: String? = null,
    val tripDistanceKm: Double = 0.0,
    val isPersonalRideMode: Boolean = false,
    val personalOptions: List<PersonalFareEstimate> = emptyList(),
    val selectedPersonalOption: PersonalFareEstimate? = null,
    val allOnlineDrivers: List<RemoteDriver> = emptyList(),
    val poolingFareEstimate: PoolingFareEstimate? = null,
    val isSearchingRouteVehicles: Boolean = false,
    val routeSearchSecondsRemaining: Int = 120,
    val hasRouteSearchTimedOut: Boolean = false,
    val rideOtp: String = "",
    val liveTripDistanceKm: Double = 0.0,
    val liveEtaMinutes: Int = 0,
    val isRidePooled: Boolean = false,
    val liveFareAmount: Double = 0.0
)

class HomeViewModel(
    private val mobilityRepository: MobilityRepository
) : ViewModel() {

    companion object {
        const val MAX_DISPATCH_RADIUS_METERS = 8_000f // 8.0 KM strict city proximity limit
    }

    private val TAG = "HomeViewModel"

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = false))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Distinct soft light colors for individual vehicle routes matching user requirement
    private val LIGHT_COLORS = listOf(
        "#10B981", // Light Emerald Green
        "#0EA5E9", // Sky Blue
        "#F59E0B", // Light Amber / Orange
        "#8B5CF6", // Light Violet
        "#EC4899", // Light Pink
        "#14B8A6"  // Light Teal
    )

    private var pollJob: Job? = null
    private var driverTrackingJob: Job? = null
    private var driverRefreshJob: Job? = null
    private var routeSearchTimerJob: Job? = null

    init {
        startPeriodicDriverSearch()
    }

    private fun startPeriodicDriverSearch() {
        driverRefreshJob?.cancel()
        driverRefreshJob = viewModelScope.launch {
            while (true) {
                if (_uiState.value.rideBookingStatus == "NONE") {
                    applyRealDriverSearch()
                }
                // Adaptive Polling: 3.5s when pickup & drop coordinates exist, 8s when idle/browsing
                val hasCoordinates = _uiState.value.pickupLat != 0.0 && _uiState.value.dropLat != 0.0
                val refreshDelay = if (hasCoordinates) 3500L else 8000L
                delay(refreshDelay)
            }
        }
    }

    fun setTripCoordinates(
        pickup: String,
        drop: String,
        pLat: Double,
        pLng: Double,
        dLat: Double,
        dLng: Double
    ) {
        routeSearchTimerJob?.cancel()
        _uiState.update {
            it.copy(
                vehicles = emptyList(),
                filteredVehicles = emptyList(),
                pickupQuery = pickup,
                dropQuery = drop,
                pickupLat = pLat,
                pickupLng = pLng,
                dropLat = dLat,
                dropLng = dLng,
                tripDistanceKm = 0.0,
                personalOptions = emptyList(),
                selectedPersonalOption = null,
                passengerRoutePoints = emptyList(),
                selectedVehicle = null,
                acceptedVehicle = null,
                rideBookingStatus = "NONE",
                bookingError = null,
                isPersonalRideMode = false,
                isSearchingRouteVehicles = true,
                routeSearchSecondsRemaining = 120,
                hasRouteSearchTimedOut = false
            )
        }
        computeRoadDistanceAndPricing(pLat, pLng, dLat, dLng)
        applyRealDriverSearch()

        // 120-second (2 minutes) search countdown: Look for live drivers heading along the passenger's route
        routeSearchTimerJob = viewModelScope.launch {
            var elapsedSeconds = 0
            for (sec in 120 downTo 1) {
                _uiState.update { it.copy(routeSearchSecondsRemaining = sec) }
                delay(1000L)
                elapsedSeconds++
                // Wait at least 8 seconds before auto-transitioning (so user always sees the scanning card)
                if (elapsedSeconds >= 8 && _uiState.value.filteredVehicles.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            isSearchingRouteVehicles = false,
                            routeSearchSecondsRemaining = 0,
                            hasRouteSearchTimedOut = false
                        )
                    }
                    return@launch
                }
            }
            // 120s expired with no matching route vehicle: auto-switch to Personal Ride option (Rapido style)!
            _uiState.update {
                it.copy(
                    isSearchingRouteVehicles = false,
                    routeSearchSecondsRemaining = 0,
                    hasRouteSearchTimedOut = true,
                    isPersonalRideMode = true
                )
            }
        }
    }

    fun skipRouteSearchAndOpenPersonal() {
        routeSearchTimerJob?.cancel()
        _uiState.update {
            it.copy(
                isSearchingRouteVehicles = false,
                routeSearchSecondsRemaining = 0,
                hasRouteSearchTimedOut = true,
                isPersonalRideMode = true
            )
        }
    }

    private fun computeRoadDistanceAndPricing(pLat: Double, pLng: Double, dLat: Double, dLng: Double) {
        if (pLat == 0.0 || pLng == 0.0 || dLat == 0.0 || dLng == 0.0) return
        viewModelScope.launch {
            val roadResult = OsrmRoutingHelper.fetchRoadRoute(
                originLat = pLat,
                originLng = pLng,
                destLat = dLat,
                destLng = dLng
            )
            val rawKm = (roadResult?.distanceMeters ?: 4000.0) / 1000.0
            val distanceKm = Math.round(rawKm * 10.0) / 10.0
            val personalOpts = FareCalculationEngine.getPersonalOptions(
                distanceKm = distanceKm,
                pLat = pLat,
                pLng = pLng,
                onlineDrivers = _uiState.value.allOnlineDrivers
            )
            val defaultSel = personalOpts.firstOrNull { it.isAvailable && it.type == PersonalVehicleType.AUTO }
                ?: personalOpts.firstOrNull { it.isAvailable }
                ?: personalOpts.firstOrNull()
            val poolEst = FareCalculationEngine.calculatePoolingFare(distanceKm, occupiedSeats = 1)
            val routePoints = roadResult?.points ?: emptyList()

            _uiState.update { current ->
                val freshSelected = personalOpts.firstOrNull { it.type == current.selectedPersonalOption?.type } ?: defaultSel
                current.copy(
                    tripDistanceKm = distanceKm,
                    personalOptions = personalOpts,
                    selectedPersonalOption = freshSelected,
                    poolingFareEstimate = poolEst,
                    passengerRoutePoints = routePoints
                )
            }
        }
    }

    fun setPersonalRideMode(enabled: Boolean) {
        _uiState.update { it.copy(isPersonalRideMode = enabled) }
    }

    fun selectPersonalOption(option: PersonalFareEstimate) {
        _uiState.update { it.copy(selectedPersonalOption = option) }
    }

    fun onPickupQueryChange(query: String) {
        _uiState.update { it.copy(pickupQuery = query) }
        applyRealDriverSearch()
    }

    fun onDropQueryChange(query: String) {
        _uiState.update { it.copy(dropQuery = query) }
        applyRealDriverSearch()
    }

    fun selectVehicle(vehicle: Vehicle?) {
        _uiState.update { it.copy(selectedVehicle = vehicle) }
    }

    fun toggleFallbackListView() {
        _uiState.update { it.copy(isFallbackListVisible = !it.isFallbackListVisible) }
    }

    fun recenterDemoArea() {
        _uiState.update {
            it.copy(
                filteredVehicles = it.vehicles,
                selectedVehicle = it.vehicles.firstOrNull()
            )
        }
    }

    /**
     * Connects with Supabase backend to find real active online drivers
     * and constructs their road polylines using free OpenStreetMap OSRM routing.
     */
    fun applyRealDriverSearch() {
        viewModelScope.launch {
            val pLat = _uiState.value.pickupLat
            val pLng = _uiState.value.pickupLng
            val dLat = _uiState.value.dropLat
            val dLng = _uiState.value.dropLng

            val matchedVehicles = mutableListOf<Vehicle>()
            var remoteDrivers: List<RemoteDriver> = emptyList()

            try {
                // 1. Fetch real online drivers & their active destination routes from Supabase (spatially bounded)
                remoteDrivers = RideRequestClient.fetchActiveDrivers(pLat, pLng)
                val remoteRoutes = RideRequestClient.fetchActiveDriverRoutes()

                var colorIdx = 0
                for (drv in remoteDrivers) {
                    if (drv.currentLat == 0.0 || drv.currentLng == 0.0) continue
                    if (drv.availableSeats <= 0) continue

                    // Proximity check: Only consider drivers within 8 KM of pickup
                    if (pLat != 0.0 && pLng != 0.0) {
                        val dArr = FloatArray(1)
                        Location.distanceBetween(drv.currentLat, drv.currentLng, pLat, pLng, dArr)
                        if (dArr[0] > MAX_DISPATCH_RADIUS_METERS) {
                            continue
                        }
                    }

                    val routeInfo = remoteRoutes[drv.id] ?: remoteRoutes[drv.mobileNumber]
                    val hasPassengerTrip = (pLat != 0.0 && pLng != 0.0 && dLat != 0.0 && dLng != 0.0)

                    // A driver can ONLY be a shared pooling vehicle if they have set an active destination route in Supabase
                    if (routeInfo == null || routeInfo.destLat == 0.0 || routeInfo.destLng == 0.0) {
                        continue
                    }

                    // If passenger has not set search coordinates yet, do not add shared corridor vehicles
                    if (!hasPassengerTrip) {
                        continue
                    }

                    // Preserve DRIVER'S OWN independent destination - NEVER overwrite with passenger dropoff!
                    val destName = routeInfo.destinationName.ifBlank { "City Route Corridor" }
                    val destLat = routeInfo.destLat
                    val destLng = routeInfo.destLng

                    // Fetch turn-by-turn road polyline from driver location to driver destination
                    val roadResult = OsrmRoutingHelper.fetchRoadRoute(
                        originLat = drv.currentLat,
                        originLng = drv.currentLng,
                        destLat = destLat,
                        destLng = destLng
                    )
                    val polylinePoints = roadResult?.points?.map { Pair(it.latitude, it.longitude) } ?: listOf(
                        Pair(drv.currentLat, drv.currentLng),
                        Pair(destLat, destLng)
                    )

                    // Corridor matching: verify passenger's pickup and dropoff lie within driver's travel corridor
                    // and strictly heading in the exact same forward direction
                    if (hasPassengerTrip) {
                        val matchesCorridor = CorridorMatchingHelper.isPassengerOnDriverCorridor(
                            pLat = pLat,
                            pLng = pLng,
                            dLat = dLat,
                            dLng = dLng,
                            driverLat = drv.currentLat,
                            driverLng = drv.currentLng,
                            destLat = destLat,
                            destLng = destLng,
                            routePoints = polylinePoints,
                            maxCorridorMeters = 1000.0
                        )
                        if (!matchesCorridor) {
                            Log.d(TAG, "Driver ${drv.fullName} destination $destName does not match passenger corridor/direction - excluded.")
                            continue
                        }
                    }

                    // Calculate realistic distance to passenger pickup
                    val distToPickupMeters = if (pLat != 0.0 && pLng != 0.0) {
                        val results = FloatArray(1)
                        Location.distanceBetween(drv.currentLat, drv.currentLng, pLat, pLng, results)
                        results[0].toDouble()
                    } else {
                        800.0
                    }

                    // ETA = distance in meters / ~380 meters per minute (approx 23 km/h urban speed)
                    val etaMins = (distToPickupMeters / 380.0).toInt().coerceAtLeast(1)

                    val colorHex = LIGHT_COLORS[colorIdx % LIGHT_COLORS.size]
                    colorIdx++

                    val category = when (drv.vehicleType.uppercase()) {
                        "E_RICKSHAW" -> VehicleCategory.E_RICKSHAW
                        "AUTO" -> VehicleCategory.AUTO_RICKSHAW
                        "MINI_CAR" -> VehicleCategory.MINI_CAR
                        "SEDAN" -> VehicleCategory.SEDAN
                        else -> VehicleCategory.AUTO_RICKSHAW
                    }

                    val tripKm = if (_uiState.value.tripDistanceKm > 0.0) _uiState.value.tripDistanceKm else ((roadResult?.distanceMeters ?: 4000.0) / 1000.0)
                    val poolEst = FareCalculationEngine.calculatePoolingFare(tripKm, (drv.totalSeats - drv.availableSeats).coerceAtLeast(0))

                    val v = Vehicle(
                        vehicleId = drv.id.ifBlank { "drv_${drv.mobileNumber}" },
                        licensePlateMasked = drv.vehicleNumber.ifBlank { "DL 01 ** 4812" },
                        category = category,
                        modelName = drv.vehicleModel.ifBlank { "Auto Rickshaw (5 Seater)" },
                        currentLat = drv.currentLat,
                        currentLng = drv.currentLng,
                        headingDegrees = drv.bearing,
                        route = Route(
                            routeId = "rte_${drv.id}",
                            originName = "Current Location",
                            destinationName = destName,
                            originLat = drv.currentLat,
                            originLng = drv.currentLng,
                            destinationLat = destLat,
                            destinationLng = destLng,
                            polylinePoints = polylinePoints,
                            totalDistanceKm = (roadResult?.distanceMeters ?: 4000.0) / 1000.0,
                            estimatedDurationMinutes = etaMins
                        ),
                        routeStatus = RouteStatus.EN_ROUTE,
                        seatAvailability = SeatAvailability(
                            totalSeats = drv.totalSeats.coerceAtLeast(1),
                            occupiedSeats = (drv.totalSeats - drv.availableSeats).coerceAtLeast(0)
                        ),
                        driver = DriverProfile(
                            driverId = drv.id,
                            fullName = drv.fullName.ifBlank { "Driver Partner" },
                            rating = 4.8,
                            totalRidesCompleted = 120,
                            isVerified = true
                        ),
                        farePerKmEstimate = poolEst.fareAmount.toDouble(),
                        isDemoSimulated = false,
                        routeColorHex = colorHex,
                        etaMinutes = etaMins,
                        driverPhone = drv.mobileNumber,
                        roadPolylinePoints = polylinePoints
                    )
                    matchedVehicles.add(v)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error querying real drivers from Supabase: ${e.message}")
            }

            // High-Occupancy Optimization (Fill most-occupied vehicle first):
            // Prioritize the vehicle whose seats are closest to being full (fewest available seats > 0).
            // Once a vehicle is full (availableSeats <= 0), it is excluded.
            // If available seats are equal, prioritize the vehicle closer to pickup (lower etaMinutes).
            matchedVehicles.sortWith(
                compareBy<Vehicle> { it.seatAvailability.availableSeats }
                    .thenBy { it.etaMinutes }
            )

            val distKm = _uiState.value.tripDistanceKm
            val freshPersonalOpts = if (distKm > 0.0) {
                FareCalculationEngine.getPersonalOptions(
                    distanceKm = distKm,
                    pLat = _uiState.value.pickupLat,
                    pLng = _uiState.value.pickupLng,
                    onlineDrivers = remoteDrivers
                )
            } else {
                _uiState.value.personalOptions
            }
            val updatedSelectedPersonal = freshPersonalOpts.firstOrNull { it.type == _uiState.value.selectedPersonalOption?.type }
                ?: freshPersonalOpts.firstOrNull { it.isAvailable }
                ?: freshPersonalOpts.firstOrNull()

            // Real live vehicles only - no fake demo fallbacks
            _uiState.update {
                it.copy(
                    vehicles = matchedVehicles,
                    filteredVehicles = matchedVehicles,
                    allOnlineDrivers = remoteDrivers,
                    personalOptions = freshPersonalOpts,
                    selectedPersonalOption = updatedSelectedPersonal,
                    isLoading = false,
                    selectedVehicle = it.selectedVehicle?.takeIf { sel -> matchedVehicles.any { v -> v.vehicleId == sel.vehicleId && v.seatAvailability.availableSeats > 0 } } ?: matchedVehicles.firstOrNull()
                )
            }
        }
    }

    /**
     * Confirms / requests a specific vehicle.
     * Writes to Supabase so target driver app receives the popup alert.
     */
    fun bookSelectedVehicle(
        targetVehicle: Vehicle,
        userName: String,
        userPhone: String
    ) {
        viewModelScope.launch {
            val pLat = _uiState.value.pickupLat.takeIf { it != 0.0 } ?: targetVehicle.currentLat
            val pLng = _uiState.value.pickupLng.takeIf { it != 0.0 } ?: targetVehicle.currentLng

            // Proximity check: Must be within MAX_DISPATCH_RADIUS_METERS (8 KM)
            if (targetVehicle.currentLat != 0.0 && targetVehicle.currentLng != 0.0 && pLat != 0.0 && pLng != 0.0) {
                val results = FloatArray(1)
                Location.distanceBetween(targetVehicle.currentLat, targetVehicle.currentLng, pLat, pLng, results)
                if (results[0] > MAX_DISPATCH_RADIUS_METERS) {
                    _uiState.update {
                        it.copy(
                            rideBookingStatus = "NONE",
                            bookingError = "No vehicle available"
                        )
                    }
                    return@launch
                }
            }

            val generatedOtp = String.format(java.util.Locale.US, "%04d", (1000..9999).random())
            _uiState.update {
                it.copy(
                    selectedVehicle = targetVehicle,
                    rideBookingStatus = "REQUESTED",
                    bookingError = null,
                    rideOtp = generatedOtp
                )
            }

            val dLat = _uiState.value.dropLat.takeIf { it != 0.0 } ?: targetVehicle.route.destinationLat
            val dLng = _uiState.value.dropLng.takeIf { it != 0.0 } ?: targetVehicle.route.destinationLng

            val tripKm = if (_uiState.value.tripDistanceKm > 0.0) _uiState.value.tripDistanceKm else targetVehicle.route.totalDistanceKm
            val occupied = targetVehicle.seatAvailability.occupiedSeats
            val calculatedPoolFare = FareCalculationEngine.calculatePoolingFare(tripKm, occupied).fareAmount.toDouble()

            val result = RideRequestClient.createRideRequest(
                passengerName = userName,
                passengerMobile = userPhone,
                pickupAddress = _uiState.value.pickupQuery.ifBlank { "Pickup Location" },
                pickupLat = pLat,
                pickupLng = pLng,
                dropAddress = _uiState.value.dropQuery.ifBlank { "Dropoff Location" },
                dropLat = dLat,
                dropLng = dLng,
                targetDriverId = targetVehicle.driver.driverId.ifBlank { null },
                requestedSeats = 1,
                fare = calculatedPoolFare,
                otp = generatedOtp
            )

            if (result.isSuccess) {
                val reqId = result.getOrNull() ?: ""
                _uiState.update { it.copy(activeRequestId = reqId) }
                startRideStatusPolling(reqId, targetVehicle)
            } else {
                _uiState.update {
                    it.copy(
                        rideBookingStatus = "NONE",
                        bookingError = "No vehicle available"
                    )
                }
            }
        }
    }

    /**
     * Books a dedicated Personal Vehicle (Rapido / Uber Style).
     * Finds the nearest online driver to passenger's pickup location who matches the EXACT vehicle category
     * and is strictly within 8 KM proximity radius.
     */
    fun bookPersonalRide(
        selectedOption: PersonalFareEstimate,
        userName: String,
        userPhone: String
    ) {
        viewModelScope.launch {
            val generatedOtp = String.format(java.util.Locale.US, "%04d", (1000..9999).random())

            var pLat = _uiState.value.pickupLat
            var pLng = _uiState.value.pickupLng
            var dLat = _uiState.value.dropLat
            var dLng = _uiState.value.dropLng

            // Guard against 0.0 coordinates violating Supabase not-null constraints
            if (pLat == 0.0 || pLng == 0.0) {
                pLat = 21.107345
                pLng = 81.018838
            }
            if (dLat == 0.0 || dLng == 0.0) {
                dLat = pLat + 0.012
                dLng = pLng + 0.012
            }

            // Fresh query from Supabase to find online drivers
            val freshOnlineDrivers = try {
                RideRequestClient.fetchActiveDrivers()
            } catch (e: Exception) {
                _uiState.value.allOnlineDrivers
            }

            // STRICT MATCH: Only drivers who are:
            // 1. Online
            // 2. Matching this vehicle category
            // 3. Within strict 8 KM proximity radius (MAX_DISPATCH_RADIUS_METERS) of pickup location
            val matchingDriversInRadius = freshOnlineDrivers.filter { drv ->
                if (!drv.isOnline || !drv.matchesCategory(selectedOption.type.name)) return@filter false
                if (drv.currentLat == 0.0 || drv.currentLng == 0.0) return@filter false
                val results = FloatArray(1)
                Location.distanceBetween(drv.currentLat, drv.currentLng, pLat, pLng, results)
                results[0] <= MAX_DISPATCH_RADIUS_METERS
            }

            if (matchingDriversInRadius.isEmpty()) {
                // No driver of this vehicle type is within 8 KM!
                // Prevent sending request to distant drivers in other cities.
                _uiState.update {
                    it.copy(
                        rideBookingStatus = "NONE",
                        bookingError = "No vehicle available"
                    )
                }
                return@launch
            }

            // Immediately set REQUESTED state only when a nearby driver is genuinely available
            _uiState.update {
                it.copy(
                    rideBookingStatus = "REQUESTED",
                    bookingError = null,
                    rideOtp = generatedOtp
                )
            }

            // Nearest driver matching THIS vehicle category ONLY within 8 KM
            val nearestDriver = matchingDriversInRadius.minByOrNull { drv ->
                val results = FloatArray(1)
                Location.distanceBetween(drv.currentLat, drv.currentLng, pLat, pLng, results)
                results[0]
            }

            val targetDriverUuid = nearestDriver?.id?.takeIf { RideRequestClient.isValidUuid(it) }

            val result = RideRequestClient.createRideRequest(
                passengerName = userName.ifBlank { "Passenger" },
                passengerMobile = userPhone.ifBlank { "9876543210" },
                pickupAddress = _uiState.value.pickupQuery.ifBlank { "Pickup Location" },
                pickupLat = pLat,
                pickupLng = pLng,
                dropAddress = _uiState.value.dropQuery.ifBlank { "Dropoff Location" },
                dropLat = dLat,
                dropLng = dLng,
                targetDriverId = targetDriverUuid,
                requestedSeats = selectedOption.type.capacity,
                fare = selectedOption.fareAmount.toDouble(),
                otp = generatedOtp
            )

            if (!result.isSuccess) {
                _uiState.update {
                    it.copy(
                        rideBookingStatus = "NONE",
                        bookingError = "No vehicle available"
                    )
                }
                return@launch
            }

            val targetVehicle = nearestDriver?.let { drv ->
                Vehicle(
                    vehicleId = drv.id,
                    licensePlateMasked = drv.vehicleNumber.ifBlank { "DL 01 ** 9821" },
                    category = when (selectedOption.type) {
                        PersonalVehicleType.AUTO -> VehicleCategory.AUTO_RICKSHAW
                        PersonalVehicleType.E_RICKSHAW -> VehicleCategory.E_RICKSHAW
                        PersonalVehicleType.CAB -> VehicleCategory.MINI_CAR
                    },
                    modelName = "${selectedOption.type.displayName} (Personal Dedicated)",
                    currentLat = drv.currentLat,
                    currentLng = drv.currentLng,
                    headingDegrees = drv.bearing,
                    route = Route(
                        routeId = "personal_${drv.id}",
                        originName = _uiState.value.pickupQuery,
                        destinationName = _uiState.value.dropQuery,
                        originLat = pLat,
                        originLng = pLng,
                        destinationLat = dLat,
                        destinationLng = dLng,
                        polylinePoints = emptyList(),
                        totalDistanceKm = selectedOption.distanceKm,
                        estimatedDurationMinutes = selectedOption.etaMinutes
                    ),
                    routeStatus = RouteStatus.EN_ROUTE,
                    seatAvailability = SeatAvailability(
                        totalSeats = selectedOption.type.capacity,
                        occupiedSeats = selectedOption.type.capacity
                    ),
                    driver = DriverProfile(
                        driverId = drv.id,
                        fullName = drv.fullName.ifBlank { "Driver Partner" },
                        rating = 4.9,
                        totalRidesCompleted = 142,
                        isVerified = true
                    ),
                    farePerKmEstimate = selectedOption.fareAmount.toDouble(),
                    isDemoSimulated = false,
                    routeColorHex = "#F59E0B",
                    etaMinutes = selectedOption.etaMinutes,
                    driverPhone = drv.mobileNumber
                )
            } ?: Vehicle(
                vehicleId = "searching_driver",
                licensePlateMasked = "Connecting...",
                category = when (selectedOption.type) {
                    PersonalVehicleType.AUTO -> VehicleCategory.AUTO_RICKSHAW
                    PersonalVehicleType.E_RICKSHAW -> VehicleCategory.E_RICKSHAW
                    PersonalVehicleType.CAB -> VehicleCategory.MINI_CAR
                },
                modelName = selectedOption.type.displayName,
                currentLat = pLat,
                currentLng = pLng,
                headingDegrees = 0f,
                route = Route(
                    routeId = "pending_route",
                    originName = _uiState.value.pickupQuery,
                    destinationName = _uiState.value.dropQuery,
                    originLat = pLat,
                    originLng = pLng,
                    destinationLat = dLat,
                    destinationLng = dLng,
                    polylinePoints = emptyList(),
                    totalDistanceKm = selectedOption.distanceKm,
                    estimatedDurationMinutes = selectedOption.etaMinutes
                ),
                routeStatus = RouteStatus.EN_ROUTE,
                seatAvailability = SeatAvailability(
                    totalSeats = selectedOption.type.capacity,
                    occupiedSeats = 0
                ),
                driver = DriverProfile(
                    driverId = "pending",
                    fullName = "Connecting Driver...",
                    rating = 5.0,
                    totalRidesCompleted = 0,
                    isVerified = true
                ),
                farePerKmEstimate = selectedOption.fareAmount.toDouble(),
                isDemoSimulated = false,
                routeColorHex = "#F59E0B",
                etaMinutes = 2,
                driverPhone = ""
            )

            val reqId = if (result.isSuccess) {
                result.getOrNull() ?: "req_${System.currentTimeMillis()}"
            } else {
                "req_${System.currentTimeMillis()}"
            }

            _uiState.update {
                it.copy(
                    activeRequestId = reqId,
                    selectedVehicle = targetVehicle
                )
            }
            startRideStatusPolling(reqId, targetVehicle)
        }
    }

    private fun startRideStatusPolling(requestId: String, targetVehicle: Vehicle) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            while (true) {
                delay(2000L) // Poll every 2 seconds
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed > 120_000L) { // 120s TTL (2 minutes)
                    RideRequestClient.cancelRideRequest(requestId)
                    _uiState.update {
                        it.copy(
                            rideBookingStatus = "NONE",
                            isPersonalRideMode = true,
                            bookingError = "No vehicle available"
                        )
                    }
                    startPeriodicDriverSearch()
                    break
                }

                val statusResult = RideRequestClient.pollRideStatus(requestId)
                if (statusResult != null) {
                    if (statusResult.status.equals("ACCEPTED", ignoreCase = true)) {
                        // Driver accepted the request!
                        onRideAccepted(statusResult, targetVehicle)
                        break
                    } else if (statusResult.status.equals("DECLINED", ignoreCase = true) ||
                        statusResult.status.equals("CANCELLED", ignoreCase = true) ||
                        statusResult.status.equals("EXPIRED", ignoreCase = true)
                    ) {
                        _uiState.update {
                            it.copy(
                                rideBookingStatus = "NONE",
                                bookingError = "No vehicle available"
                            )
                        }
                        startPeriodicDriverSearch()
                        break
                    }
                }
            }
        }
    }

    private fun onRideAccepted(statusResult: RideStatusResult, targetVehicle: Vehicle) {
        viewModelScope.launch {
            val dLat = if (statusResult.driverLat != 0.0) statusResult.driverLat else targetVehicle.currentLat
            val dLng = if (statusResult.driverLng != 0.0) statusResult.driverLng else targetVehicle.currentLng

            val accepted = targetVehicle.copy(
                currentLat = dLat,
                currentLng = dLng,
                headingDegrees = statusResult.driverBearing,
                driverPhone = statusResult.driverPhone ?: targetVehicle.driverPhone,
                licensePlateMasked = statusResult.vehicleNumber ?: targetVehicle.licensePlateMasked,
                driver = targetVehicle.driver.copy(
                    fullName = statusResult.driverName ?: targetVehicle.driver.fullName
                )
            )

            // Per user requirement: polyline ONLY shows passenger's pickup to dropoff!
            val pLat = _uiState.value.pickupLat.takeIf { it != 0.0 } ?: dLat
            val pLng = _uiState.value.pickupLng.takeIf { it != 0.0 } ?: dLng
            val dropLat = _uiState.value.dropLat.takeIf { it != 0.0 } ?: targetVehicle.route.destinationLat
            val dropLng = _uiState.value.dropLng.takeIf { it != 0.0 } ?: targetVehicle.route.destinationLng

            val roadResult = OsrmRoutingHelper.fetchRoadRoute(
                originLat = pLat,
                originLng = pLng,
                destLat = dropLat,
                destLng = dropLng
            )

            val passengerGeoPoints = roadResult?.points ?: listOf(
                GeoPoint(pLat, pLng),
                GeoPoint(dropLat, dropLng)
            )

            _uiState.update {
                it.copy(
                    rideBookingStatus = "ACCEPTED",
                    acceptedVehicle = accepted,
                    selectedVehicle = accepted,
                    // Per user requirement: Remove other vehicles' polylines, only keep accepted vehicle!
                    filteredVehicles = listOf(accepted),
                    passengerRoutePoints = passengerGeoPoints
                )
            }

            // Start live GPS tracking of accepted driver
            startDriverLiveTracking(statusResult.driverPhone ?: targetVehicle.driverPhone)
        }
    }

    private fun startDriverLiveTracking(driverPhone: String) {
        driverTrackingJob?.cancel()
        driverTrackingJob = viewModelScope.launch {
            while (true) {
                delay(2500L)
                val currentStatus = _uiState.value.rideBookingStatus
                if (currentStatus != "ACCEPTED" && currentStatus != "IN_PROGRESS") break
                try {
                    val reqId = _uiState.value.activeRequestId ?: ""
                    val updatedStatus = RideRequestClient.pollRideStatus(reqId)
                    if (updatedStatus != null) {
                        if (updatedStatus.status.equals("COMPLETED", ignoreCase = true)) {
                            // Trip successfully completed by driver
                            val finishedVehicle = _uiState.value.acceptedVehicle ?: _uiState.value.selectedVehicle
                            _uiState.update {
                                it.copy(
                                    rideBookingStatus = "COMPLETED",
                                    completedDriver = finishedVehicle,
                                    activeRequestId = null,
                                    acceptedVehicle = null,
                                    selectedVehicle = null,
                                    passengerRoutePoints = emptyList(),
                                    liveTripDistanceKm = 0.0,
                                    liveEtaMinutes = 0,
                                    isRidePooled = false,
                                    pickupQuery = "",
                                    dropQuery = "",
                                    pickupLat = 0.0,
                                    pickupLng = 0.0,
                                    dropLat = 0.0,
                                    dropLng = 0.0,
                                    personalOptions = emptyList(),
                                    tripDistanceKm = 0.0,
                                    bookingError = null
                                )
                            }
                            break
                        } else if (updatedStatus.status.equals("CANCELLED", ignoreCase = true) ||
                            updatedStatus.status.equals("DECLINED", ignoreCase = true)
                        ) {
                            // Trip cancelled by driver
                            _uiState.update {
                                it.copy(
                                    rideBookingStatus = "NONE",
                                    activeRequestId = null,
                                    acceptedVehicle = null,
                                    selectedVehicle = null,
                                    passengerRoutePoints = emptyList(),
                                    liveTripDistanceKm = 0.0,
                                    liveEtaMinutes = 0,
                                    isRidePooled = false,
                                    bookingError = "Ride cancelled by driver"
                                )
                            }
                            applyRealDriverSearch()
                            startPeriodicDriverSearch()
                            break
                        }

                        val isNowInProgress = updatedStatus.status.equals("IN_PROGRESS", ignoreCase = true)

                        if (updatedStatus.driverLat != 0.0) {
                            val dropLat = _uiState.value.dropLat.takeIf { it != 0.0 } ?: 21.12
                            val dropLng = _uiState.value.dropLng.takeIf { it != 0.0 } ?: 81.03

                            // Calculate distance from driver live location to dropoff
                            val distResults = FloatArray(1)
                            Location.distanceBetween(
                                updatedStatus.driverLat,
                                updatedStatus.driverLng,
                                dropLat,
                                dropLng,
                                distResults
                            )
                            val remainingDistKm = (distResults[0] / 1000.0 * 10.0).roundToInt() / 10.0
                            val remainingEtaMins = ((remainingDistKm / 28.0) * 60).roundToInt().coerceAtLeast(1)

                            _uiState.update { current ->
                                val currentAcc = current.acceptedVehicle ?: return@update current
                                val updatedAcc = currentAcc.copy(
                                    currentLat = updatedStatus.driverLat,
                                    currentLng = updatedStatus.driverLng,
                                    headingDegrees = updatedStatus.driverBearing,
                                    farePerKmEstimate = if (updatedStatus.fareAmount > 0.0) updatedStatus.fareAmount else currentAcc.farePerKmEstimate
                                )
                                val isPooled = updatedStatus.fareAmount > 0.0 && updatedStatus.fareAmount < currentAcc.farePerKmEstimate

                                current.copy(
                                    rideBookingStatus = if (isNowInProgress) "IN_PROGRESS" else current.rideBookingStatus,
                                    acceptedVehicle = updatedAcc,
                                    selectedVehicle = updatedAcc,
                                    filteredVehicles = listOf(updatedAcc),
                                    liveTripDistanceKm = remainingDistKm,
                                    liveEtaMinutes = remainingEtaMins,
                                    liveFareAmount = if (updatedStatus.fareAmount > 0.0) updatedStatus.fareAmount else current.liveFareAmount,
                                    isRidePooled = isPooled
                                )
                            }
                        } else if (isNowInProgress) {
                            _uiState.update { current ->
                                current.copy(
                                    rideBookingStatus = "IN_PROGRESS",
                                    liveTripDistanceKm = if (current.tripDistanceKm > 0) current.tripDistanceKm else 2.6,
                                    liveEtaMinutes = if (current.liveEtaMinutes > 0) current.liveEtaMinutes else 6,
                                    liveFareAmount = if (updatedStatus.fareAmount > 0.0) updatedStatus.fareAmount else current.liveFareAmount
                                )
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun cancelCurrentRide() {
        viewModelScope.launch {
            pollJob?.cancel()
            driverTrackingJob?.cancel()
            routeSearchTimerJob?.cancel()
            val reqId = _uiState.value.activeRequestId
            if (!reqId.isNullOrBlank()) {
                RideRequestClient.cancelRideRequest(reqId)
            }
            _uiState.update {
                it.copy(
                    rideBookingStatus = "NONE",
                    activeRequestId = null,
                    acceptedVehicle = null,
                    selectedVehicle = null,
                    bookingError = null,
                    isSearchingRouteVehicles = false,
                    liveTripDistanceKm = 0.0,
                    liveEtaMinutes = 0,
                    isRidePooled = false
                )
            }
            applyRealDriverSearch()
            startPeriodicDriverSearch()
        }
    }

    /**
     * Resets the home screen after a completed ride has been dismissed/rated.
     * Restores state so the user is ready to book a new ride from clean pickup/drop input.
     */
    fun resetHomeAfterRideCompleted() {
        _uiState.update {
            it.copy(
                rideBookingStatus = "NONE",
                completedDriver = null,
                activeRequestId = null,
                acceptedVehicle = null,
                selectedVehicle = null,
                passengerRoutePoints = emptyList(),
                liveTripDistanceKm = 0.0,
                liveEtaMinutes = 0,
                isRidePooled = false,
                pickupQuery = "",
                dropQuery = "",
                pickupLat = 0.0,
                pickupLng = 0.0,
                dropLat = 0.0,
                dropLng = 0.0,
                personalOptions = emptyList(),
                tripDistanceKm = 0.0,
                bookingError = null,
                isPersonalRideMode = false,
                isSearchingRouteVehicles = false
            )
        }
        applyRealDriverSearch()
        startPeriodicDriverSearch()
    }

    /**
     * Checks Supabase for active ride if passenger killed/closed the app during a ride.
     * Restores state so the ride tracker, map polyline, and driver details immediately resume.
     */
    suspend fun checkAndRestoreActiveRide(userPhone: String): RideStatusResult? {
        if (_uiState.value.rideBookingStatus != "NONE" && _uiState.value.activeRequestId != null) {
            return null // Already tracking an active ride
        }
        val cleanPhone = userPhone.replace(Regex("[^0-9]"), "").takeLast(10)
        if (cleanPhone.length < 10) return null

        val activeRide = RideRequestClient.fetchActiveRideForPassenger(cleanPhone)
        if (activeRide != null) {
            Log.d(TAG, "Restoring active ride from Supabase for passenger $cleanPhone: ${activeRide.requestId} (${activeRide.status})")
            val pLat = activeRide.pickupLat
            val pLng = activeRide.pickupLng
            val dLat = activeRide.dropLat
            val dLng = activeRide.dropLng

            val bookingStatus = when (activeRide.status.uppercase()) {
                "PENDING" -> "REQUESTED"
                "ACCEPTED" -> "ACCEPTED"
                "IN_PROGRESS" -> "IN_PROGRESS"
                else -> "NONE"
            }

            val mockDriver = Vehicle(
                vehicleId = activeRide.driverId ?: "drv-active",
                licensePlateMasked = activeRide.vehicleNumber ?: "Auto",
                category = VehicleCategory.AUTO_RICKSHAW,
                modelName = "Haze Partner",
                currentLat = activeRide.driverLat,
                currentLng = activeRide.driverLng,
                headingDegrees = activeRide.driverBearing,
                route = Route(
                    routeId = "rt_${activeRide.requestId}",
                    originName = activeRide.pickupName ?: "",
                    originLat = pLat,
                    originLng = pLng,
                    destinationName = activeRide.dropName ?: "",
                    destinationLat = dLat,
                    destinationLng = dLng,
                    polylinePoints = emptyList(),
                    totalDistanceKm = 3.0,
                    estimatedDurationMinutes = 8
                ),
                routeStatus = RouteStatus.EN_ROUTE,
                seatAvailability = SeatAvailability(
                    totalSeats = 4,
                    occupiedSeats = 1
                ),
                driver = DriverProfile(
                    driverId = activeRide.driverId ?: "active",
                    fullName = activeRide.driverName ?: "Driver Partner",
                    rating = 4.9,
                    totalRidesCompleted = 120,
                    isVerified = true
                ),
                farePerKmEstimate = activeRide.fareAmount,
                isDemoSimulated = false,
                routeColorHex = "#10B981",
                etaMinutes = 4,
                driverPhone = activeRide.driverPhone ?: ""
            )

            val roadRes = if (pLat != 0.0 && dLat != 0.0) {
                OsrmRoutingHelper.fetchRoadRoute(pLat, pLng, dLat, dLng)
            } else null

            val passengerGeoPoints = roadRes?.points ?: if (pLat != 0.0 && dLat != 0.0) {
                listOf(GeoPoint(pLat, pLng), GeoPoint(dLat, dLng))
            } else emptyList()

            _uiState.update {
                it.copy(
                    activeRequestId = activeRide.requestId,
                    rideBookingStatus = bookingStatus,
                    rideOtp = activeRide.otp ?: "1234",
                    pickupQuery = activeRide.pickupName ?: "",
                    dropQuery = activeRide.dropName ?: "",
                    pickupLat = pLat,
                    pickupLng = pLng,
                    dropLat = dLat,
                    dropLng = dLng,
                    acceptedVehicle = mockDriver,
                    selectedVehicle = mockDriver,
                    liveFareAmount = activeRide.fareAmount,
                    passengerRoutePoints = passengerGeoPoints,
                    filteredVehicles = listOf(mockDriver),
                    isSearchingRouteVehicles = false
                )
            }

            if (bookingStatus == "REQUESTED") {
                startRideStatusPolling(activeRide.requestId, mockDriver)
            } else if (bookingStatus == "ACCEPTED" || bookingStatus == "IN_PROGRESS") {
                startDriverLiveTracking(activeRide.driverPhone ?: "")
            }
            return activeRide
        }
        return null
    }

    fun clearBookingError() {
        _uiState.update { it.copy(bookingError = null) }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
        driverTrackingJob?.cancel()
        driverRefreshJob?.cancel()
        routeSearchTimerJob?.cancel()
    }
}
