package com.glip.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glip.app.domain.model.UserProfile
import com.glip.app.domain.repository.UserRepository
import com.glip.app.domain.usecase.PhoneValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val userProfile: UserProfile? = null,
    val maskedPhone: String = "",
    val isLoggedOut: Boolean = false,
    val showClearDataDialog: Boolean = false,
    val showPrivacyDialog: Boolean = false,
    val message: String? = null
)

class ProfileViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeProfile()
    }

    private fun observeProfile() {
        viewModelScope.launch {
            userRepository.getUserProfile().collect { profile ->
                _uiState.update {
                    it.copy(
                        userProfile = profile,
                        maskedPhone = profile?.let { p -> PhoneValidator.maskPhoneNumber(p.phoneNumber) } ?: ""
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.clearUserProfile()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }

    fun clearDemoData() {
        viewModelScope.launch {
            userRepository.clearUserProfile()
            _uiState.update {
                it.copy(
                    showClearDataDialog = false,
                    isLoggedOut = true,
                    message = "Demo profile and local preferences cleared."
                )
            }
        }
    }

    fun showClearDataDialog(show: Boolean) {
        _uiState.update { it.copy(showClearDataDialog = show) }
    }

    fun showPrivacyDialog(show: Boolean) {
        _uiState.update { it.copy(showPrivacyDialog = show) }
    }
}
