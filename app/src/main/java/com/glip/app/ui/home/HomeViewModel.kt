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
    val rideBookingStatus: String = "NONE", // NONE, REQUESTED, ACCEPTED, CANCELLED
    val acceptedVehicle: Vehicle? = null,
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
    val rideOtp: String = ""
)

class HomeViewModel(
    private val mobilityRepository: MobilityRepository
) : ViewModel() {

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
                delay(3500L) // Auto-refresh active matching drivers every 3.5s
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
            for (sec in 120 downTo 1) {
                _uiState.update { it.copy(routeSearchSecondsRemaining = sec) }
                delay(1000L)
                // If a vehicle matched along the route, cancel countdown
                if (_uiState.value.filteredVehicles.isNotEmpty()) {
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
                // 1. Fetch real online drivers & their active destination routes from Supabase
                remoteDrivers = RideRequestClient.fetchActiveDrivers()
                val remoteRoutes = RideRequestClient.fetchActiveDriverRoutes()

                var colorIdx = 0
                for (drv in remoteDrivers) {
                    if (drv.currentLat == 0.0 || drv.currentLng == 0.0) continue
                    if (drv.availableSeats <= 0) continue

                    val routeInfo = remoteRoutes[drv.id] ?: remoteRoutes[drv.mobileNumber]
                    val hasPassengerTrip = (pLat != 0.0 && pLng != 0.0 && dLat != 0.0 && dLng != 0.0)

                    // If passenger specified a destination corridor, driver MUST have an active destination route
                    if (hasPassengerTrip && (routeInfo == null || routeInfo.destLat == 0.0 || routeInfo.destLng == 0.0)) {
                        continue
                    }

                    // Preserve DRIVER'S OWN independent destination - NEVER overwrite with passenger dropoff!
                    val destName = routeInfo?.destinationName?.ifBlank { "City Route Corridor" } ?: "City Route Corridor"
                    val destLat = if (routeInfo != null && routeInfo.destLat != 0.0) {
                        routeInfo.destLat
                    } else {
                        drv.currentLat + 0.015
                    }
                    val destLng = if (routeInfo != null && routeInfo.destLng != 0.0) {
                        routeInfo.destLng
                    } else {
                        drv.currentLng + 0.015
                    }

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
                    if (hasPassengerTrip) {
                        val matchesCorridor = CorridorMatchingHelper.isPassengerOnDriverCorridor(
                            pLat = pLat,
                            pLng = pLng,
                            dLat = dLat,
                            dLng = dLng,
                            routePoints = polylinePoints,
                            maxCorridorMeters = 2500.0
                        )
                        if (!matchesCorridor) {
                            Log.d(TAG, "Driver ${drv.fullName} destination $destName does not overlap passenger corridor - excluded.")
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
                    selectedVehicle = it.selectedVehicle?.takeIf { sel -> matchedVehicles.any { v -> v.vehicleId == sel.vehicleId } } ?: matchedVehicles.firstOrNull()
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
            val generatedOtp = String.format(java.util.Locale.US, "%04d", (1000..9999).random())
            _uiState.update {
                it.copy(
                    selectedVehicle = targetVehicle,
                    rideBookingStatus = "REQUESTED",
                    bookingError = null,
                    rideOtp = generatedOtp
                )
            }

            val pLat = _uiState.value.pickupLat.takeIf { it != 0.0 } ?: targetVehicle.currentLat
            val pLng = _uiState.value.pickupLng.takeIf { it != 0.0 } ?: targetVehicle.currentLng
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
                        rideBookingStatus = "REQUESTED",
                        activeRequestId = "local_${System.currentTimeMillis()}"
                    )
                }
            }
        }
    }

    /**
     * Books a dedicated Personal Vehicle (Rapido / Uber Style).
     * Finds the nearest online driver to passenger's pickup location who matches the EXACT vehicle category.
     */
    fun bookPersonalRide(
        selectedOption: PersonalFareEstimate,
        userName: String,
        userPhone: String
    ) {
        viewModelScope.launch {
            val generatedOtp = String.format(java.util.Locale.US, "%04d", (1000..9999).random())

            // Immediately set REQUESTED state so the UI transitions instantly to Searching In Progress
            _uiState.update {
                it.copy(
                    rideBookingStatus = "REQUESTED",
                    bookingError = null,
                    rideOtp = generatedOtp
                )
            }

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

            // STRICT MATCH: Only drivers who are online AND match the requested vehicle category
            // (e.g. Auto only to Auto drivers, Bike only to Bike drivers, E-Rickshaw only to E-Rickshaw drivers)
            val matchingDrivers = freshOnlineDrivers.filter { drv ->
                drv.isOnline && drv.matchesCategory(selectedOption.type.name)
            }

            // Nearest driver matching THIS vehicle category ONLY (NEVER fallback to a different vehicle type!)
            val nearestDriver = matchingDrivers.minByOrNull { drv ->
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

            val targetVehicle = nearestDriver?.let { drv ->
                Vehicle(
                    vehicleId = drv.id,
                    licensePlateMasked = drv.vehicleNumber.ifBlank { "DL 01 ** 9821" },
                    category = when (selectedOption.type) {
                        PersonalVehicleType.AUTO -> VehicleCategory.AUTO_RICKSHAW
                        PersonalVehicleType.E_RICKSHAW -> VehicleCategory.E_RICKSHAW
                        PersonalVehicleType.BIKE -> VehicleCategory.BIKE
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
                    PersonalVehicleType.BIKE -> VehicleCategory.BIKE
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
                            bookingError = "Driver response time expired (2 minutes). Kripya doobara prayas karein."
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
                                bookingError = "Driver was unavailable. Please pick another vehicle."
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
                delay(3000L)
                if (_uiState.value.rideBookingStatus != "ACCEPTED") break
                try {
                    val reqId = _uiState.value.activeRequestId ?: ""
                    val updatedStatus = RideRequestClient.pollRideStatus(reqId)
                    if (updatedStatus != null && updatedStatus.driverLat != 0.0) {
                        _uiState.update { current ->
                            val currentAcc = current.acceptedVehicle ?: return@update current
                            val updatedAcc = currentAcc.copy(
                                currentLat = updatedStatus.driverLat,
                                currentLng = updatedStatus.driverLng,
                                headingDegrees = updatedStatus.driverBearing
                            )
                            current.copy(
                                acceptedVehicle = updatedAcc,
                                selectedVehicle = updatedAcc,
                                filteredVehicles = listOf(updatedAcc)
                            )
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
                    passengerRoutePoints = emptyList(),
                    bookingError = null,
                    isSearchingRouteVehicles = false
                )
            }
            applyRealDriverSearch()
            startPeriodicDriverSearch()
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
        driverTrackingJob?.cancel()
        driverRefreshJob?.cancel()
        routeSearchTimerJob?.cancel()
    }
}
