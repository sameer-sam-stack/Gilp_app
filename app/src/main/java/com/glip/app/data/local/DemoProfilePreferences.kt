package com.glip.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.glip.app.domain.model.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart

class DemoProfilePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "glip_demo_profile_prefs"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_PHONE_NUMBER = "phone_number"
        private const val KEY_EMAIL = "email"
        private const val KEY_CREATED_AT = "created_at"
        private const val KEY_APP_LANGUAGE = "app_language"
    }

    fun getUserProfile(): UserProfile? {
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val fullName = prefs.getString(KEY_FULL_NAME, "") ?: ""
        val phoneNumber = prefs.getString(KEY_PHONE_NUMBER, "") ?: ""
        val email = prefs.getString(KEY_EMAIL, null)
        val createdAt = prefs.getLong(KEY_CREATED_AT, 0L)
        return UserProfile(
            userId = userId,
            fullName = fullName,
            phoneNumber = phoneNumber,
            email = email,
            createdAtTimestamp = createdAt
        )
    }

    fun saveUserProfile(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_USER_ID, profile.userId)
            .putString(KEY_FULL_NAME, profile.fullName)
            .putString(KEY_PHONE_NUMBER, profile.phoneNumber)
            .putString(KEY_EMAIL, profile.email)
            .putLong(KEY_CREATED_AT, profile.createdAtTimestamp)
            .apply()
    }

    fun getAppLanguage(): String {
        return prefs.getString(KEY_APP_LANGUAGE, "English") ?: "English"
    }

    fun setAppLanguage(language: String) {
        prefs.edit().putString(KEY_APP_LANGUAGE, language).apply()
    }

    fun clearUserProfile() {
        prefs.edit().clear().apply()
    }

    fun getPermanentRideOtp(): String {
        val phone = getUserProfile()?.phoneNumber
        val userId = getUserProfile()?.userId
        return com.glip.app.util.UserRideOtpManager.getPersistentRideOtp(
            context = null,
            phoneNumber = phone,
            userId = userId
        )
    }

    fun observeUserProfile(): Flow<UserProfile?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(getUserProfile())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getUserProfile())
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.onStart {
        emit(getUserProfile())
    }.distinctUntilChanged()
}
