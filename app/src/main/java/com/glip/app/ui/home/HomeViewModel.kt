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
import com.glip.app.util.OsrmRoutingHelper
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
    val bookingError: String? = null
)

class HomeViewModel(
    private val mobilityRepository: MobilityRepository
) : ViewModel() {

    private val TAG = "HomeViewModel"

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
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
        _uiState.update {
            it.copy(
                pickupQuery = pickup,
                dropQuery = drop,
                pickupLat = pLat,
                pickupLng = pLng,
                dropLat = dLat,
                dropLng = dLng
            )
        }
        applyRealDriverSearch()
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
            _uiState.update { it.copy(isLoading = it.vehicles.isEmpty()) }

            val pLat = _uiState.value.pickupLat
            val pLng = _uiState.value.pickupLng
            val dLat = _uiState.value.dropLat
            val dLng = _uiState.value.dropLng

            val matchedVehicles = mutableListOf<Vehicle>()

            try {
                // 1. Fetch real online drivers & their active destination routes from Supabase
                val remoteDrivers = RideRequestClient.fetchActiveDrivers()
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
                        farePerKmEstimate = 12.0,
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

            // Real live vehicles only - no fake demo fallbacks
            _uiState.update {
                it.copy(
                    vehicles = matchedVehicles,
                    filteredVehicles = matchedVehicles,
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
            _uiState.update {
                it.copy(
                    selectedVehicle = targetVehicle,
                    rideBookingStatus = "REQUESTED",
                    bookingError = null
                )
            }

            val pLat = _uiState.value.pickupLat.takeIf { it != 0.0 } ?: targetVehicle.currentLat
            val pLng = _uiState.value.pickupLng.takeIf { it != 0.0 } ?: targetVehicle.currentLng
            val dLat = _uiState.value.dropLat.takeIf { it != 0.0 } ?: targetVehicle.route.destinationLat
            val dLng = _uiState.value.dropLng.takeIf { it != 0.0 } ?: targetVehicle.route.destinationLng

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
                fare = 35.0
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

    private fun startRideStatusPolling(requestId: String, targetVehicle: Vehicle) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            while (true) {
                delay(2000L) // Poll every 2 seconds
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed > 60_000L) { // 60s TTL - Auto-expire request so it never lingers in driver app
                    RideRequestClient.cancelRideRequest(requestId)
                    _uiState.update {
                        it.copy(
                            rideBookingStatus = "NONE",
                            bookingError = "Driver did not respond in time. Please try another vehicle."
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

            // Fetch passenger's destination polyline from driver position to destination
            val pLat = _uiState.value.pickupLat.takeIf { it != 0.0 } ?: dLat
            val pLng = _uiState.value.pickupLng.takeIf { it != 0.0 } ?: dLng
            val dropLat = _uiState.value.dropLat.takeIf { it != 0.0 } ?: targetVehicle.route.destinationLat
            val dropLng = _uiState.value.dropLng.takeIf { it != 0.0 } ?: targetVehicle.route.destinationLng

            val roadResult = OsrmRoutingHelper.fetchRoadRoute(
                originLat = dLat,
                originLng = dLng,
                destLat = dropLat,
                destLng = dropLng
            )

            val passengerGeoPoints = roadResult?.points ?: listOf(
                GeoPoint(dLat, dLng),
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
            val reqId = _uiState.value.activeRequestId
            if (!reqId.isNullOrBlank()) {
                RideRequestClient.cancelRideRequest(reqId)
            }
            _uiState.update {
                it.copy(
                    rideBookingStatus = "NONE",
                    activeRequestId = null,
                    acceptedVehicle = null,
                    passengerRoutePoints = emptyList(),
                    bookingError = null
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
    }
}
