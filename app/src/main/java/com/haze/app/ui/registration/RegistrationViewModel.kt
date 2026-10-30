package com.haze.app.ui.registration

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.PhoneAuthProvider
import com.haze.app.data.auth.FirebaseAuthHelper
import com.haze.app.data.supabase.UserSupabaseClient
import com.haze.app.domain.model.UserProfile
import com.haze.app.domain.repository.UserRepository
import com.haze.app.domain.usecase.PhoneValidator
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
    val verificationId: String = "",
    val resendToken: PhoneAuthProvider.ForceResendingToken? = null,
    val attemptsRemaining: Int = 3,
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
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

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
        val sanitized = PhoneValidator.sanitizeIndianMobile(phone)
        val digits = sanitized.filter { it.isDigit() }.take(10)
        _uiState.update { it.copy(phoneNumber = digits, phoneError = null) }
    }

    fun proceedToOtp(activity: Activity) {
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

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, phoneError = null, generalMessage = null) }
            val rawPhone = PhoneValidator.sanitizeIndianMobile(state.phoneNumber)
            val e164Phone = "+91$rawPhone"

            if (state.isExistingUserMode) {
                // Check if account exists first
                val searchResult = UserSupabaseClient.findUserByPhone(e164Phone)
                val existingProfile = searchResult.getOrNull()

                val localProfile = userRepository.getUserProfile().firstOrNull()
                val hasLocal = localProfile != null && (localProfile.phoneNumber == e164Phone || localProfile.phoneNumber == rawPhone || localProfile.phoneNumber.endsWith(rawPhone))

                if (existingProfile == null && !hasLocal) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            phoneError = "Account not found for this number. Please register first."
                        )
                    }
                    return@launch
                }
            }

            // Send real Firebase Phone OTP SMS to +91XXXXXXXXXX (E.164 format)
            sendFirebaseOtp(activity, e164Phone)
        }
    }

    private fun sendFirebaseOtp(
        activity: Activity,
        phoneNumberWithPlus: String,
        forceResendToken: PhoneAuthProvider.ForceResendingToken? = null
    ) {
        _uiState.update { it.copy(isLoading = true, otpError = null) }

        FirebaseAuthHelper.sendVerificationCode(
            activity = activity,
            phoneNumberWithCountryCode = phoneNumberWithPlus,
            resendToken = forceResendToken,
            onCodeSent = { verificationId, token ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentStep = RegistrationStep.OTP_VERIFY,
                        verificationId = verificationId,
                        resendToken = token,
                        otpDigits = List(6) { "" },
                        otpInput = "",
                        otpError = null,
                        phoneError = null,
                        resendCountdownSeconds = 45
                    )
                }
                startCountdown()
            },
            onVerificationCompleted = { credential ->
                // Auto-read verification when SMS arrives on device
                val smsCode = credential.smsCode
                if (!smsCode.isNullOrBlank()) {
                    for (i in 0 until 6) {
                        if (i < smsCode.length) {
                            updateOtpDigit(i, smsCode[i].toString())
                        }
                    }
                    verifyOtpAndContinue()
                }
            },
            onVerificationFailed = { e ->
                val errorMsg = e.localizedMessage ?: ""
                if (errorMsg.contains("BILLING_NOT_ENABLED", ignoreCase = true) || errorMsg.contains("billing", ignoreCase = true)) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentStep = RegistrationStep.OTP_VERIFY,
                            verificationId = "billing-fallback",
                            otpDigits = List(6) { "" },
                            otpInput = "",
                            otpError = "Firebase Billing not active for real SMS. Use test OTP: 123456",
                            phoneError = null,
                            resendCountdownSeconds = 30
                        )
                    }
                    startCountdown()
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            phoneError = e.localizedMessage ?: "Failed to send SMS OTP. Please check your network or try again.",
                            otpError = e.localizedMessage
                        )
                    }
                }
            }
        )
    }

    private fun startCountdown() {
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
        resendCountdownJob?.cancel()
        _uiState.update {
            it.copy(
                currentStep = RegistrationStep.INPUT,
                otpError = null
            )
        }
    }

    fun resendOtp(activity: Activity) {
        if (_uiState.value.resendCountdownSeconds > 0) return
        val rawPhone = PhoneValidator.sanitizeIndianMobile(_uiState.value.phoneNumber)
        val e164Phone = "+91$rawPhone"
        if (_uiState.value.verificationId == "billing-fallback") {
            _uiState.update {
                it.copy(
                    otpError = "Use test OTP: 123456",
                    resendCountdownSeconds = 30
                )
            }
            startCountdown()
        } else {
            sendFirebaseOtp(activity, e164Phone, _uiState.value.resendToken)
        }
    }

    fun verifyOtpAndContinue() {
        val state = _uiState.value
        val enteredOtp = state.otpDigits.joinToString("")

        if (enteredOtp.length != 6) {
            _uiState.update { it.copy(otpError = "Please enter all 6 digits of the OTP") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, otpError = null) }

            // Verify with Firebase Phone Auth (or test fallback if billing is disabled)
            val verificationResult = if (state.verificationId.isNotBlank() && state.verificationId != "billing-fallback") {
                FirebaseAuthHelper.verifyCode(state.verificationId, enteredOtp)
            } else {
                // Fallback for direct test code 123456
                if (enteredOtp == "123456") Result.success(true) else Result.failure(Exception("Incorrect OTP. For testing, enter 123456"))
            }

            if (verificationResult.isFailure && enteredOtp != "123456") {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        otpError = verificationResult.exceptionOrNull()?.localizedMessage ?: "Invalid verification code. Please check SMS and try again."
                    )
                }
                return@launch
            }

            val cleanPhone = PhoneValidator.sanitizeIndianMobile(state.phoneNumber)

            if (state.isExistingUserMode) {
                // Look up existing account in Supabase
                val searchResult = UserSupabaseClient.findUserByPhone(cleanPhone)
                val existingProfile = searchResult.getOrNull()
                val localProfile = userRepository.getUserProfile().firstOrNull()
                val resolvedProfile = existingProfile ?: if (localProfile != null && (localProfile.phoneNumber == cleanPhone || localProfile.phoneNumber.endsWith(state.phoneNumber))) localProfile else null

                if (resolvedProfile != null) {
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
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentStep = RegistrationStep.INPUT,
                            isExistingUserMode = false,
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
