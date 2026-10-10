package com.glip.app

import com.glip.app.domain.usecase.EmailValidator
import com.glip.app.domain.usecase.OtpVerificationManager
import com.glip.app.domain.usecase.PhoneValidator
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ValidationAndOtpTest {

    private lateinit var otpManager: OtpVerificationManager

    @Before
    fun setUp() {
        otpManager = OtpVerificationManager(maxAttempts = 3, cooldownSeconds = 30)
    }

    @Test
    fun testValidIndianMobileNumbers() {
        assertTrue(PhoneValidator.isValidIndianMobile("9876543210"))
        assertTrue(PhoneValidator.isValidIndianMobile("+919876543210"))
        assertTrue(PhoneValidator.isValidIndianMobile("919876543210"))
        assertTrue(PhoneValidator.isValidIndianMobile("09876543210"))
        assertTrue(PhoneValidator.isValidIndianMobile("8888888888"))
        assertTrue(PhoneValidator.isValidIndianMobile("7777777777"))
        assertTrue(PhoneValidator.isValidIndianMobile("6666666666"))
    }

    @Test
    fun testInvalidIndianMobileNumbers() {
        assertFalse(PhoneValidator.isValidIndianMobile("1234567890")) // Doesn't start with 6-9
        assertFalse(PhoneValidator.isValidIndianMobile("98765"))      // Too short
        assertFalse(PhoneValidator.isValidIndianMobile("987654321000")) // Too long
        assertFalse(PhoneValidator.isValidIndianMobile("abcdefghij")) // Non-digits
        assertFalse(PhoneValidator.isValidIndianMobile(""))           // Empty
    }

    @Test
    fun testSanitizeIndianMobile() {
        assertEquals("9876543210", PhoneValidator.sanitizeIndianMobile("+919876543210"))
        assertEquals("9876543210", PhoneValidator.sanitizeIndianMobile("919876543210"))
        assertEquals("9876543210", PhoneValidator.sanitizeIndianMobile("09876543210"))
        assertEquals("9876543210", PhoneValidator.sanitizeIndianMobile("9876543210"))
    }

    @Test
    fun testMaskPhoneNumber() {
        assertEquals("98******10", PhoneValidator.maskPhoneNumber("9876543210"))
        assertEquals("98******10", PhoneValidator.maskPhoneNumber("+919876543210"))
    }

    @Test
    fun testEmailValidation() {
        assertTrue(EmailValidator.isValidEmail(null)) // Null is allowed because email is optional
        assertTrue(EmailValidator.isValidEmail(""))   // Empty string allowed
        assertTrue(EmailValidator.isValidEmail("   ")) // Blank allowed
        assertTrue(EmailValidator.isValidEmail("ramesh@glip.in"))
        assertTrue(EmailValidator.isValidEmail("user.name+tag@domain.co.in"))
        assertFalse(EmailValidator.isValidEmail("plainaddress"))
        assertFalse(EmailValidator.isValidEmail("missing@domain"))
    }

    @Test
    fun testOtpGenerationAndVerificationSuccess() {
        val otp = otpManager.generateOtp()
        assertEquals(6, otp.length)
        assertTrue(otp.all { it.isDigit() })

        val result = otpManager.verifyOtp(otp)
        assertTrue(result is OtpVerificationManager.OtpResult.Success)
    }

    @Test
    fun testOtpVerificationFailsOnWrongOtp() {
        val otp = otpManager.generateOtp()
        val wrongOtp = if (otp == "111111") "222222" else "111111"

        val result = otpManager.verifyOtp(wrongOtp)
        assertTrue(result is OtpVerificationManager.OtpResult.Invalid)
    }

    @Test
    fun testOtpExhaustsAttemptsAfterMaxTries() {
        val otp = otpManager.generateOtp()
        val wrongOtp = "000000"

        val attempt1 = otpManager.verifyOtp(wrongOtp)
        assertTrue(attempt1 is OtpVerificationManager.OtpResult.Invalid)

        val attempt2 = otpManager.verifyOtp(wrongOtp)
        assertTrue(attempt2 is OtpVerificationManager.OtpResult.Invalid)

        val attempt3 = otpManager.verifyOtp(wrongOtp)
        assertTrue(attempt3 is OtpVerificationManager.OtpResult.AttemptsExhausted)

        // Further attempt should be blocked
        val attempt4 = otpManager.verifyOtp(otp)
        assertTrue(attempt4 is OtpVerificationManager.OtpResult.AttemptsExhausted)
    }

    @Test
    fun testOtpCooldownPreventsInstantRegeneration() {
        otpManager.generateOtp()
        assertFalse(otpManager.canRequestNewOtp())
        assertTrue(otpManager.getRemainingCooldownSeconds() > 0)
    }
}
