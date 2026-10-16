package com.glip.app.ui.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glip.app.data.supabase.UserSupabaseClient
import com.glip.app.domain.model.UserProfile
import com.glip.app.domain.repository.UserRepository
import com.glip.app.domain.usecase.OtpVerificationManager
import com.glip.app.domain.usecase.PhoneValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class RegistrationStep {
    INPUT,          // Page 1: Name and Phone (or only Phone for existing accounts)
    OTP_VERIFY      // Page 2: 6-Box OTP Verification Screen
}

data class RegistrationUiState(
    val currentStep: RegistrationStep = RegistrationStep.INPUT,
    val isExistingUserMode: Boolean = false, // True if user selects "I have already an account"
    val fullName: String = "",
    val phoneNumber: String = "",
    val otpDigits: List<String> = List(6) { "" },
    val otpInput: String = "",
    val generatedOtp: String = "",
    val showOtpPopup: Boolean = false,
    val attemptsRemaining: Int = 3,
    val cooldownSeconds: Long = 0,
    val isLoading: Boolean = false,
    val fullNameError: String? = null,
    val phoneError: String? = null,
    val otpError: String? = null,
    val isRegistrationSuccess: Boolean = false,
    val generalMessage: String? = null,
    val restoredUserName: String? = null
)

class RegistrationViewModel(
    private val userRepository: UserRepository,
    private val otpManager: OtpVerificationManager = OtpVerificationManager()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    private var otpPopupJob: Job? = null

    fun toggleExistingUserMode(isExisting: Boolean) {
        _uiState.update {
            it.copy(
                isExistingUserMode = isExisting,
                fullNameError = null,
                phoneError = null,
                generalMessage = null
            )
        }
    }

    fun onFullNameChange(name: String) {
        _uiState.update { it.copy(fullName = name, fullNameError = null) }
    }

    fun onPhoneNumberChange(phone: String) {
        val digits = phone.filter { it.isDigit() }.take(10)
        _uiState.update { it.copy(phoneNumber = digits, phoneError = null) }
    }

    fun proceedToOtp() {
        val state = _uiState.value
        var hasError = false

        // In new user mode, Name is required
        if (!state.isExistingUserMode && state.fullName.trim().isEmpty()) {
            _uiState.update { it.copy(fullNameError = "Please enter your full name") }
            hasError = true
        }

        // 10-digit phone number is required in both modes
        if (!PhoneValidator.isValidIndianMobile(state.phoneNumber)) {
            _uiState.update { it.copy(phoneError = "Please enter a valid 10-digit mobile number") }
            hasError = true
        }

        if (hasError) return

        // Generate a random 6-digit OTP and auto-fill immediately
        val randomOtp = (100000..999999).random().toString()
        val autoFilledDigits = randomOtp.map { it.toString() }

        _uiState.update {
            it.copy(
                currentStep = RegistrationStep.OTP_VERIFY,
                generatedOtp = randomOtp,
                otpDigits = autoFilledDigits,
                otpInput = randomOtp,
                showOtpPopup = false,
                otpError = null
            )
        }
    }

    private fun startOtpPopupTimer() {
        // Demo popup timer removed - OTP auto-fills directly
        otpPopupJob?.cancel()
    }

    fun updateOtpDigit(index: Int, value: String) {
        val currentDigits = _uiState.value.otpDigits.toMutableList()

        if (value.length > 1) {
            // User pasted a multi-digit code
            val pastedDigits = value.filter { it.isDigit() }.take(6)
            for (i in 0 until 6) {
                currentDigits[i] = if (i < pastedDigits.length) pastedDigits[i].toString() else ""
            }
        } else {
            currentDigits[index] = value.filter { it.isDigit() }
        }

        val combined = currentDigits.joinToString("")
        _uiState.update {
            it.copy(
                otpDigits = currentDigits,
                otpInput = combined,
                otpError = null
            )
        }
    }

    fun copyAndFillOtp() {
        val code = _uiState.value.generatedOtp.ifBlank { "123456" }
        val digits = code.take(6).map { it.toString() }
        val fullList = digits + List(6 - digits.size) { "" }

        _uiState.update {
            it.copy(
                otpDigits = fullList,
                otpInput = code,
                showOtpPopup = false,
                otpError = null
            )
        }
    }

    fun dismissOtpPopup() {
        _uiState.update { it.copy(showOtpPopup = false) }
    }

    fun goBackToPhoneInput() {
        otpPopupJob?.cancel()
        _uiState.update {
            it.copy(
                currentStep = RegistrationStep.INPUT,
                showOtpPopup = false,
                otpError = null
            )
        }
    }

    fun resendOtp() {
        val randomOtp = (100000..999999).random().toString()
        val autoFilledDigits = randomOtp.map { it.toString() }
        _uiState.update {
            it.copy(
                generatedOtp = randomOtp,
                otpDigits = autoFilledDigits,
                otpInput = randomOtp,
                showOtpPopup = false,
                otpError = null
            )
        }
    }

    fun verifyOtpAndContinue() {
        val state = _uiState.value
        val enteredOtp = state.otpDigits.joinToString("")

        if (enteredOtp.length != 6) {
            _uiState.update { it.copy(otpError = "Please enter all 6 digits of the OTP") }
            return
        }

        // Verify against generated OTP or universal testing code 123456
        val isValidOtp = enteredOtp == state.generatedOtp || enteredOtp == "123456"
        if (!isValidOtp) {
            _uiState.update { it.copy(otpError = "Invalid verification code. Please check and try again.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, otpError = null) }
            val cleanPhone = PhoneValidator.sanitizeIndianMobile(state.phoneNumber)

            if (state.isExistingUserMode) {
                // Look up existing account in Supabase
                val searchResult = UserSupabaseClient.findUserByPhone(cleanPhone)
                val existingProfile = searchResult.getOrNull()

                if (existingProfile != null) {
                    // Restore existing account into local profile preferences
                    userRepository.saveUserProfile(existingProfile)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRegistrationSuccess = true,
                            restoredUserName = existingProfile.fullName,
                            generalMessage = "Welcome back, ${existingProfile.fullName}!"
                        )
                    }
                } else {
                    // Account not found in Supabase: Create a fresh profile with this phone number
                    val newProfile = UserProfile(
                        userId = "USR-${UUID.randomUUID().toString().take(8).uppercase()}",
                        fullName = "Glip Rider",
                        phoneNumber = cleanPhone,
                        email = null
                    )
                    UserSupabaseClient.saveUserToSupabase(newProfile)
                    userRepository.saveUserProfile(newProfile)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRegistrationSuccess = true
                        )
                    }
                }
            } else {
                // New User Registration: create profile with entered Name & Phone and sync to Supabase
                val newProfile = UserProfile(
                    userId = "USR-${UUID.randomUUID().toString().take(8).uppercase()}",
                    fullName = state.fullName.trim(),
                    phoneNumber = cleanPhone,
                    email = null
                )
                // Save locally and sync to Supabase
                UserSupabaseClient.saveUserToSupabase(newProfile)
                userRepository.saveUserProfile(newProfile)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRegistrationSuccess = true
                    )
                }
            }
        }
    }
}
