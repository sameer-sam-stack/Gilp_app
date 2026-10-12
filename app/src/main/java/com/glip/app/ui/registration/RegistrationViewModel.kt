package com.glip.app.ui.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glip.app.domain.model.UserProfile
import com.glip.app.domain.repository.UserRepository
import com.glip.app.domain.usecase.OtpVerificationManager
import com.glip.app.domain.usecase.PhoneValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class RegistrationUiState(
    val fullName: String = "",
    val phoneNumber: String = "",
    val otpInput: String = "",
    val isOtpRequested: Boolean = false,
    val demoGeneratedOtp: String? = null,
    val attemptsRemaining: Int = 3,
    val cooldownSeconds: Long = 0,
    val isLoading: Boolean = false,
    val fullNameError: String? = null,
    val phoneError: String? = null,
    val otpError: String? = null,
    val isRegistrationSuccess: Boolean = false,
    val generalMessage: String? = null
)

class RegistrationViewModel(
    private val userRepository: UserRepository,
    private val otpManager: OtpVerificationManager = OtpVerificationManager()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    fun onFullNameChange(name: String) {
        _uiState.update { it.copy(fullName = name, fullNameError = null) }
    }

    fun onPhoneNumberChange(phone: String) {
        _uiState.update { it.copy(phoneNumber = phone, phoneError = null) }
    }

    fun onOtpInputChange(otp: String) {
        if (otp.length <= 6 && otp.all { it.isDigit() }) {
            _uiState.update { it.copy(otpInput = otp, otpError = null) }
        }
    }

    fun requestOtp() {
        val state = _uiState.value
        var hasError = false

        if (state.fullName.trim().isEmpty()) {
            _uiState.update { it.copy(fullNameError = "Please enter your full name") }
            hasError = true
        }

        if (!PhoneValidator.isValidIndianMobile(state.phoneNumber)) {
            _uiState.update { it.copy(phoneError = "Please enter a valid 10-digit Indian mobile number") }
            hasError = true
        }

        if (hasError) return

        if (!otpManager.canRequestNewOtp()) {
            val remaining = otpManager.getRemainingCooldownSeconds()
            _uiState.update {
                it.copy(
                    cooldownSeconds = remaining,
                    generalMessage = "Please wait $remaining seconds before requesting a new OTP"
                )
            }
            return
        }

        val code = otpManager.generateOtp()
        _uiState.update {
            it.copy(
                isOtpRequested = true,
                demoGeneratedOtp = code,
                attemptsRemaining = 3,
                cooldownSeconds = 30,
                otpError = null,
                generalMessage = "Demo OTP generated for local device testing."
            )
        }
    }

    fun verifyOtpAndRegister() {
        val state = _uiState.value
        if (state.otpInput.length != 6) {
            _uiState.update { it.copy(otpError = "Please enter the complete 6-digit OTP") }
            return
        }

        when (val result = otpManager.verifyOtp(state.otpInput)) {
            is OtpVerificationManager.OtpResult.Success -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true, otpError = null) }
                    val cleanPhone = PhoneValidator.sanitizeIndianMobile(state.phoneNumber)
                    val profile = UserProfile(
                        userId = "USR-${UUID.randomUUID().toString().take(8).uppercase()}",
                        fullName = state.fullName.trim(),
                        phoneNumber = cleanPhone,
                        email = null
                    )
                    val saveResult = userRepository.saveUserProfile(profile)
                    if (saveResult.isSuccess) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRegistrationSuccess = true
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                generalMessage = "Failed to save profile: ${saveResult.exceptionOrNull()?.message}"
                            )
                        }
                    }
                }
            }
            is OtpVerificationManager.OtpResult.Invalid -> {
                _uiState.update { it.copy(otpError = result.message) }
            }
            is OtpVerificationManager.OtpResult.AttemptsExhausted -> {
                _uiState.update {
                    it.copy(
                        otpError = result.message,
                        demoGeneratedOtp = null
                    )
                }
            }
            is OtpVerificationManager.OtpResult.Error -> {
                _uiState.update { it.copy(otpError = result.message) }
            }
        }
    }

    fun fillDemoOtp() {
        _uiState.value.demoGeneratedOtp?.let { code ->
            _uiState.update { it.copy(otpInput = code, otpError = null) }
        }
    }
}
