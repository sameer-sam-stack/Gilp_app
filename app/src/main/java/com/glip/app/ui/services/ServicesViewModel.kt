package com.glip.app.ui.services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glip.app.domain.model.LocalPromotion
import com.glip.app.domain.repository.MobilityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServicesUiState(
    val promotions: List<LocalPromotion> = emptyList(),
    val selectedServiceTab: Int = 0, // 0 = All Services, 1 = Offers
    val isLoading: Boolean = false
)

class ServicesViewModel(
    private val mobilityRepository: MobilityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServicesUiState(isLoading = true))
    val uiState: StateFlow<ServicesUiState> = _uiState.asStateFlow()

    init {
        loadPromotions()
    }

    private fun loadPromotions() {
        viewModelScope.launch {
            mobilityRepository.getLocalPromotions().collect { list ->
                _uiState.update {
                    it.copy(
                        promotions = list,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedServiceTab = tabIndex) }
    }
}
