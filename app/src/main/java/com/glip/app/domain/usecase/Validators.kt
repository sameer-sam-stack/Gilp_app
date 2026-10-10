package com.glip.app.domain.usecase

import java.util.regex.Pattern

object PhoneValidator {
    // Standard Indian 10-digit mobile number format starting with 6, 7, 8, or 9
    // Optionally allows +91 or 0 prefix
    private val INDIAN_MOBILE_REGEX = Pattern.compile("^(?:\\+91|91|0)?[6-9]\\d{9}$")

    fun isValidIndianMobile(phone: String): Boolean {
        val sanitized = phone.replace(" ", "").replace("-", "")
        return INDIAN_MOBILE_REGEX.matcher(sanitized).matches()
    }

    fun sanitizeIndianMobile(phone: String): String {
        val cleaned = phone.replace(" ", "").replace("-", "")
        return when {
            cleaned.startsWith("+91") -> cleaned.substring(3)
            cleaned.startsWith("91") && cleaned.length == 12 -> cleaned.substring(2)
            cleaned.startsWith("0") && cleaned.length == 11 -> cleaned.substring(1)
            else -> cleaned
        }
    }

    fun maskPhoneNumber(phone: String): String {
        val sanitized = sanitizeIndianMobile(phone)
        return if (sanitized.length == 10) {
            "${sanitized.substring(0, 2)}******${sanitized.substring(8)}"
        } else {
            "******"
        }
    }
}

object EmailValidator {
    private val EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return true // Email is optional
        return EMAIL_REGEX.matcher(email.trim()).matches()
    }
}
