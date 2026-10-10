package com.glip.app.domain.usecase

import java.security.SecureRandom

class OtpVerificationManager(
    private val maxAttempts: Int = 3,
    private val cooldownSeconds: Long = 30
) {
    private val secureRandom = SecureRandom()
    private var generatedOtp: String? = null
    private var attemptsRemaining: Int = maxAttempts
    private var otpGeneratedTimestamp: Long = 0L

    fun generateOtp(): String {
        // 6-digit random number between 100000 and 999999
        val code = 100000 + secureRandom.nextInt(900000)
        generatedOtp = code.toString()
        attemptsRemaining = maxAttempts
        otpGeneratedTimestamp = System.currentTimeMillis()
        return generatedOtp!!
    }

    fun canRequestNewOtp(): Boolean {
        if (otpGeneratedTimestamp == 0L) return true
        val elapsedSeconds = (System.currentTimeMillis() - otpGeneratedTimestamp) / 1000
        return elapsedSeconds >= cooldownSeconds
    }

    fun getRemainingCooldownSeconds(): Long {
        if (otpGeneratedTimestamp == 0L) return 0L
        val elapsedSeconds = (System.currentTimeMillis() - otpGeneratedTimestamp) / 1000
        val remaining = cooldownSeconds - elapsedSeconds
        return if (remaining > 0) remaining else 0L
    }

    fun verifyOtp(inputOtp: String): OtpResult {
        val currentOtp = generatedOtp ?: return OtpResult.Error("No OTP requested. Please request an OTP first.")

        if (attemptsRemaining <= 0) {
            return OtpResult.AttemptsExhausted("Maximum attempts reached. Please request a new OTP.")
        }

        if (inputOtp.trim() == currentOtp) {
            // Invalidate OTP once verified
            generatedOtp = null
            return OtpResult.Success
        } else {
            attemptsRemaining--
            return if (attemptsRemaining <= 0) {
                OtpResult.AttemptsExhausted("Invalid OTP. Maximum attempts reached. Please request a new OTP.")
            } else {
                OtpResult.Invalid("Invalid OTP. $attemptsRemaining attempt(s) remaining.")
            }
        }
    }

    fun reset() {
        generatedOtp = null
        attemptsRemaining = maxAttempts
        otpGeneratedTimestamp = 0L
    }

    sealed class OtpResult {
        object Success : OtpResult()
        data class Invalid(val message: String) : OtpResult()
        data class AttemptsExhausted(val message: String) : OtpResult()
        data class Error(val message: String) : OtpResult()
    }
}
