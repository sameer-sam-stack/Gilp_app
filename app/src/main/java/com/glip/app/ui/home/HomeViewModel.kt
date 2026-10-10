package com.glip.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glip.app.domain.model.Vehicle
import com.glip.app.domain.repository.MobilityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val filteredVehicles: List<Vehicle> = emptyList(),
    val selectedVehicle: Vehicle? = null,
    val pickupQuery: String = "",
    val dropQuery: String = "",
    val isLoading: Boolean = false,
    val isMapInitialized: Boolean = true,
    val isFallbackListVisible: Boolean = false
)

class HomeViewModel(
    private val mobilityRepository: MobilityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadVehicles()
    }

    private fun loadVehicles() {
        viewModelScope.launch {
            mobilityRepository.getSimulatedVehicles().collect { list ->
                _uiState.update {
                    it.copy(
                        vehicles = list,
                        filteredVehicles = list,
                        isLoading = false,
                        selectedVehicle = list.firstOrNull()
                    )
                }
            }
        }
    }

    fun onPickupQueryChange(query: String) {
        _uiState.update { it.copy(pickupQuery = query) }
        applySearch()
    }

    fun onDropQueryChange(query: String) {
        _uiState.update { it.copy(dropQuery = query) }
        applySearch()
    }

    fun selectVehicle(vehicle: Vehicle?) {
        _uiState.update { it.copy(selectedVehicle = vehicle) }
    }

    fun toggleFallbackListView() {
        _uiState.update { it.copy(isFallbackListVisible = !it.isFallbackListVisible) }
    }

    fun recenterDemoArea() {
        // Reset search and pick primary vehicle
        _uiState.update {
            it.copy(
                pickupQuery = "",
                dropQuery = "",
                filteredVehicles = it.vehicles,
                selectedVehicle = it.vehicles.firstOrNull()
            )
        }
    }

    private fun applySearch() {
        viewModelScope.launch {
            val results = mobilityRepository.searchCompatibleVehicles(
                pickupQuery = _uiState.value.pickupQuery,
                dropQuery = _uiState.value.dropQuery
            )
            _uiState.update {
                it.copy(
                    filteredVehicles = results,
                    selectedVehicle = results.firstOrNull()
                )
            }
        }
    }
}
