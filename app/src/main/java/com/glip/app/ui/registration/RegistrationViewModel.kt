package com.glip.app.ui.registration

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glip.app.data.supabase.UserSupabaseClient
import com.glip.app.domain.model.UserProfile
import com.glip.app.domain.repository.UserRepository
import com.glip.app.domain.usecase.OtpVerificationManager
import com.glip.app.domain.usecase.PhoneValidator
import com.glip.app.util.OtpNotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
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
    val resendCountdownSeconds: Int = 45,
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

    private var otpNotificationJob: Job? = null
    private var resendCountdownJob: Job? = null

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

    fun proceedToOtp(context: Context) {
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

        if (state.isExistingUserMode) {
            // Check if account exists first - direct login without existing account is prohibited
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, phoneError = null, generalMessage = null) }
                val cleanPhone = PhoneValidator.sanitizeIndianMobile(state.phoneNumber)
                val searchResult = UserSupabaseClient.findUserByPhone(cleanPhone)
                val existingProfile = searchResult.getOrNull()

                val localProfile = userRepository.getUserProfile().firstOrNull()
                val hasLocal = localProfile != null && (localProfile.phoneNumber == cleanPhone || localProfile.phoneNumber.endsWith(state.phoneNumber))

                if (existingProfile == null && !hasLocal) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            phoneError = "Account not found for this number. Please register first."
                        )
                    }
                    return@launch
                }

                // Account exists! Proceed to OTP with empty boxes and start 7s silent delivery
                val randomOtp = (100000..999999).random().toString()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentStep = RegistrationStep.OTP_VERIFY,
                        generatedOtp = randomOtp,
                        otpDigits = List(6) { "" },
                        otpInput = "",
                        showOtpPopup = false,
                        otpError = null,
                        phoneError = null,
                        resendCountdownSeconds = 45
                    )
                }
                startOtpDeliveryAndCountdown(context, randomOtp)
            }
            return
        }

        // New User mode: proceed to OTP with empty boxes and start 7s silent delivery
        val randomOtp = (100000..999999).random().toString()

        _uiState.update {
            it.copy(
                currentStep = RegistrationStep.OTP_VERIFY,
                generatedOtp = randomOtp,
                otpDigits = List(6) { "" },
                otpInput = "",
                showOtpPopup = false,
                otpError = null,
                phoneError = null,
                resendCountdownSeconds = 45
            )
        }
        startOtpDeliveryAndCountdown(context, randomOtp)
    }

    private fun startOtpDeliveryAndCountdown(context: Context, otpCode: String) {
        // 7-second background delivery: Silent in background, pops native notification at 7s
        otpNotificationJob?.cancel()
        otpNotificationJob = viewModelScope.launch {
            delay(7000L)
            OtpNotificationHelper.sendOtpNotification(context, otpCode)
        }

        // 45-second resend countdown timer (spam protection)
        resendCountdownJob?.cancel()
        resendCountdownJob = viewModelScope.launch {
            for (sec in 45 downTo 0) {
                _uiState.update { it.copy(resendCountdownSeconds = sec) }
                delay(1000L)
            }
        }
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

    fun goBackToPhoneInput() {
        otpNotificationJob?.cancel()
        resendCountdownJob?.cancel()
        _uiState.update {
            it.copy(
                currentStep = RegistrationStep.INPUT,
                showOtpPopup = false,
                otpError = null
            )
        }
    }

    fun resendOtp(context: Context) {
        if (_uiState.value.resendCountdownSeconds > 0) return
        val randomOtp = (100000..999999).random().toString()
        _uiState.update {
            it.copy(
                generatedOtp = randomOtp,
                otpDigits = List(6) { "" },
                otpInput = "",
                showOtpPopup = false,
                otpError = null,
                resendCountdownSeconds = 45
            )
        }
        startOtpDeliveryAndCountdown(context, randomOtp)
    }

    fun verifyOtpAndContinue() {
        val state = _uiState.value
        val enteredOtp = state.otpDigits.joinToString("")

        if (enteredOtp.length != 6) {
            _uiState.update { it.copy(otpError = "Please enter all 6 digits of the OTP") }
            return
        }

        // Strict verification against generated OTP or universal testing code 123456
        val isValidOtp = enteredOtp == state.generatedOtp || enteredOtp == "123456"
        if (!isValidOtp) {
            _uiState.update { it.copy(otpError = "Invalid verification code. Please check the OTP in your notification and try again.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, otpError = null) }
            val cleanPhone = PhoneValidator.sanitizeIndianMobile(state.phoneNumber)

            if (state.isExistingUserMode) {
                // Look up existing account in Supabase
                val searchResult = UserSupabaseClient.findUserByPhone(cleanPhone)
                val existingProfile = searchResult.getOrNull()
                val localProfile = userRepository.getUserProfile().firstOrNull()
                val resolvedProfile = existingProfile ?: if (localProfile != null && (localProfile.phoneNumber == cleanPhone || localProfile.phoneNumber.endsWith(state.phoneNumber))) localProfile else null

                if (resolvedProfile != null) {
                    // Restore existing account into local profile preferences
                    userRepository.saveUserProfile(resolvedProfile)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRegistrationSuccess = true,
                            restoredUserName = resolvedProfile.fullName,
                            generalMessage = "Welcome back, ${resolvedProfile.fullName}!"
                        )
                    }
                } else {
                    // Account not found: Do NOT log in directly! Prompt user to register!
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentStep = RegistrationStep.INPUT,
                            isExistingUserMode = false, // Switch to register mode
                            fullName = "",
                            phoneError = "No account found with this number. Please register your name to continue.",
                            otpError = null
                        )
                    }
                }
            } else {
                // New User Registration: Check if this phone number is already registered in Supabase
                val searchResult = UserSupabaseClient.findUserByPhone(cleanPhone)
                val existingProfile = searchResult.getOrNull()

                val resolvedProfile = if (existingProfile != null) {
                    val finalName = if (state.fullName.trim().isNotBlank()) state.fullName.trim() else existingProfile.fullName
                    existingProfile.copy(fullName = finalName)
                } else {
                    UserProfile(
                        userId = "USR-${UUID.randomUUID().toString().take(8).uppercase()}",
                        fullName = state.fullName.trim().ifBlank { "Haze Rider" },
                        phoneNumber = cleanPhone,
                        email = null
                    )
                }

                // Save locally and sync to Supabase
                UserSupabaseClient.saveUserToSupabase(resolvedProfile)
                userRepository.saveUserProfile(resolvedProfile)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRegistrationSuccess = true,
                        restoredUserName = resolvedProfile.fullName,
                        generalMessage = if (existingProfile != null) "Welcome back, ${resolvedProfile.fullName}!" else null
                    )
                }
            }
        }
    }
}
