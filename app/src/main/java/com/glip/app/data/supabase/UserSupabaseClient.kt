package com.glip.app.data.supabase

import android.util.Log
import com.glip.app.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Supabase client for User Account queries and registration.
 * Handles checking for existing users by phone number and saving new user profiles.
 */
object UserSupabaseClient {
    private const val TAG = "UserSupabaseClient"

    /**
     * Looks up an existing user in Supabase by their phone number.
     * Matches standard 10-digit number or +91 prefixed formats.
     */
    suspend fun findUserByPhone(phoneNumber: String): Result<UserProfile?> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val cleanPhone = phoneNumber.replace(Regex("[^0-9]"), "").takeLast(10)
            if (cleanPhone.length < 10) return@withContext Result.success(null)

            // PostgREST filter: match clean phone or with +91 across phone_number and phone columns
            val queryFilter = "or=(phone_number.eq.$cleanPhone,phone_number.eq.%2B91$cleanPhone,phone_number.ilike.*$cleanPhone*,phone.eq.$cleanPhone,phone.ilike.*$cleanPhone*)"
            val endpoint = "${SupabaseConfig.USER_SUPABASE_URL}/rest/v1/users?$queryFilter&select=*"
            val url = URL(endpoint)

            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("apikey", SupabaseConfig.USER_SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${SupabaseConfig.USER_SUPABASE_ANON_KEY}")
                setRequestProperty("Content-Type", "application/json")
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "User search response: $responseText")
                val jsonArray = JSONArray(responseText)
                if (jsonArray.length() > 0) {
                    val userObj = jsonArray.getJSONObject(0)
                    val id = userObj.optString("id", "user-${System.currentTimeMillis()}")
                    val name = if (userObj.has("full_name") && !userObj.isNull("full_name") && userObj.getString("full_name").isNotBlank()) {
                        userObj.getString("full_name")
                    } else if (userObj.has("name") && !userObj.isNull("name") && userObj.getString("name").isNotBlank()) {
                        userObj.getString("name")
                    } else {
                        "Haze Rider"
                    }
                    val phone = if (userObj.has("phone_number") && !userObj.isNull("phone_number") && userObj.getString("phone_number").isNotBlank()) {
                        userObj.getString("phone_number")
                    } else if (userObj.has("phone") && !userObj.isNull("phone") && userObj.getString("phone").isNotBlank()) {
                        userObj.getString("phone")
                    } else {
                        cleanPhone
                    }
                    val email = if (userObj.has("email") && !userObj.isNull("email")) userObj.getString("email") else null

                    val profile = UserProfile(
                        userId = id,
                        fullName = name,
                        phoneNumber = phone,
                        email = email,
                        createdAtTimestamp = System.currentTimeMillis()
                    )
                    Log.d(TAG, "Found existing user in Supabase: ${profile.fullName} (${profile.phoneNumber})")
                    Result.success(profile)
                } else {
                    Log.d(TAG, "No existing user found with phone: $cleanPhone")
                    Result.success(null)
                }
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.w(TAG, "Supabase user query HTTP $responseCode: $errorBody")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up user in Supabase", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Creates or updates a user profile in the Supabase `users` table.
     */
    suspend fun saveUserToSupabase(profile: UserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val endpoint = "${SupabaseConfig.USER_SUPABASE_URL}/rest/v1/users"
            val url = URL(endpoint)

            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("apikey", SupabaseConfig.USER_SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${SupabaseConfig.USER_SUPABASE_ANON_KEY}")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "resolution=merge-duplicates")
            }

            val cleanPhone = profile.phoneNumber.replace(Regex("[^0-9]"), "").takeLast(10)
            val jsonPayload = JSONObject().apply {
                put("id", profile.userId)
                put("full_name", profile.fullName)
                put("name", profile.fullName)
                put("phone_number", cleanPhone)
                put("phone", cleanPhone)
                put("email", profile.email ?: "${cleanPhone}@hazeapp.in")
                put("status", "active")
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(jsonPayload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                Log.d(TAG, "User saved to Supabase successfully: ${profile.fullName}")
                Result.success(Unit)
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "Failed to save user in Supabase. HTTP $responseCode: $errorBody")
                Result.failure(Exception("Supabase HTTP $responseCode: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error saving user in Supabase", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
}
