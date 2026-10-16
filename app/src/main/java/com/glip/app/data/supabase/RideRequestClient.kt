package com.glip.app.data.supabase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Lightweight, zero-dependency REST Client for Supabase PostgREST API.
 * Securely writes and manages Passenger Ride Requests for Driver app consumption.
 */
object RideRequestClient {
    private const val TAG = "RideRequestClient"

    /**
     * Sends a new ride booking request to Supabase so nearby drivers can view and accept it.
     */
    suspend fun createRideRequest(
        userId: String,
        userName: String,
        userPhone: String,
        pickupAddress: String,
        pickupLat: Double,
        pickupLng: Double,
        dropAddress: String,
        dropLat: Double,
        dropLng: Double,
        fare: Double = 0.0
    ): Result<String> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val endpoint = "${SupabaseConfig.SUPABASE_URL}/rest/v1/${SupabaseConfig.TABLE_RIDE_REQUESTS}"
            val url = URL(endpoint)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${SupabaseConfig.SUPABASE_ANON_KEY}")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=representation")
            }

            val requestId = UUID.randomUUID().toString()
            val payload = JSONObject().apply {
                put("id", requestId)
                put("user_id", userId)
                put("user_name", userName)
                put("user_phone", userPhone)
                put("pickup_address", pickupAddress)
                put("pickup_lat", pickupLat)
                put("pickup_lng", pickupLng)
                put("dropoff_address", dropAddress)
                put("dropoff_lat", dropLat)
                put("dropoff_lng", dropLng)
                put("status", "requested")
                put("fare", fare)
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Ride request saved in Supabase successfully. ID: $requestId, Response: $responseBody")
                Result.success(requestId)
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "Failed to save ride request in Supabase. HTTP $responseCode: $errorBody")
                Result.failure(Exception("Supabase HTTP $responseCode: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error saving ride request to Supabase", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Updates an existing ride request status (e.g. cancelled by user)
     */
    suspend fun updateRideStatus(
        requestId: String,
        newStatus: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val endpoint = "${SupabaseConfig.SUPABASE_URL}/rest/v1/${SupabaseConfig.TABLE_RIDE_REQUESTS}?id=eq.$requestId"
            val url = URL(endpoint)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${SupabaseConfig.SUPABASE_ANON_KEY}")
                setRequestProperty("Content-Type", "application/json")
            }

            val payload = JSONObject().apply {
                put("status", newStatus)
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                Result.success(Unit)
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Result.failure(Exception("Supabase HTTP $responseCode: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
}
