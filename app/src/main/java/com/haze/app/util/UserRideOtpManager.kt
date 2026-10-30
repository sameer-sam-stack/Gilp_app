package com.haze.app.util

import android.content.Context
import kotlin.math.abs

/**
 * Manages the user's permanent Ride OTP (Rapido style).
 * In Rapido/Ola, each customer has a single persistent 4-digit Ride PIN tied to their account/phone.
 * This PIN never changes across all rides taken by the user.
 */
object UserRideOtpManager {
    private const val PREFS_NAME = "glip_user_ride_otp_prefs"
    private const val KEY_OTP_PREFIX = "permanent_ride_otp_"

    /**
     * Retrieves or generates a permanent 4-digit Ride PIN for the given user.
     * Tied to the user's mobile number or user ID, and permanently persisted in SharedPreferences.
     */
    fun getPersistentRideOtp(
        context: Context? = null,
        phoneNumber: String? = null,
        userId: String? = null
    ): String {
        val cleanPhone = phoneNumber?.filter { it.isDigit() }?.takeLast(10).orEmpty()
        val userKey = when {
            cleanPhone.length == 10 -> "phone_$cleanPhone"
            !userId.isNullOrBlank() -> "id_${userId.trim()}"
            else -> "default_user"
        }

        // 1. Check local persistent SharedPreferences if context is available
        if (context != null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedOtp = prefs.getString("$KEY_OTP_PREFIX$userKey", null)
            if (!savedOtp.isNullOrBlank() && savedOtp.length == 4 && savedOtp.all { it.isDigit() }) {
                return savedOtp
            }

            // Derive a stable 4-digit PIN, save and return
            val stableOtp = deriveStableOtp(userKey)
            prefs.edit().putString("$KEY_OTP_PREFIX$userKey", stableOtp).apply()
            return stableOtp
        }

        // 2. Deterministic fallback when context is null
        return deriveStableOtp(userKey)
    }

    /**
     * Deterministically derives a 4-digit PIN (1000..9999) from the user's identifier.
     * Guaranteed to produce the exact same 4-digit PIN for the same user identifier.
     */
    private fun deriveStableOtp(identifier: String): String {
        val hash = abs(identifier.hashCode())
        val pin = 1000 + (hash % 9000)
        return String.format(java.util.Locale.US, "%04d", pin)
    }
}
