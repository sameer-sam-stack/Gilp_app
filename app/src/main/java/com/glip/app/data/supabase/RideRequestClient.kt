package com.glip.app.data.supabase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class RemoteDriver(
    val id: String,
    val fullName: String,
    val mobileNumber: String,
    val vehicleNumber: String,
    val vehicleModel: String,
    val vehicleType: String,
    val totalSeats: Int,
    val availableSeats: Int,
    val currentLat: Double,
    val currentLng: Double,
    val bearing: Float,
    val isOnline: Boolean
) {
    fun matchesCategory(categoryQuery: String): Boolean {
        val q = categoryQuery.trim().uppercase()
        val vt = vehicleType.trim().uppercase()
        val vm = vehicleModel.trim().uppercase()
        val vn = vehicleNumber.trim().uppercase()
        val combined = "$vt $vm $vn"

        return when (q) {
            "BIKE", "MOTORCYCLE", "TWO_WHEELER" -> {
                combined.contains("BIKE") || combined.contains("MOTORCYCLE") || combined.contains("SCOOTER") || combined.contains("TWO")
            }
            "E_RICKSHAW", "E-RICKSHAW", "TOTO" -> {
                combined.contains("E_RICKSHAW") || combined.contains("E-RICKSHAW") || combined.contains("E RICKSHAW") ||
                combined.contains("TOTO") || combined.contains("BATTERY") || (combined.contains("RICKSHAW") && (combined.contains("E") || combined.contains("ELECTRIC")))
            }
            "AUTO", "AUTO_RICKSHAW" -> {
                (combined.contains("AUTO") || combined.contains("RICKSHAW") || combined.contains("THREE")) &&
                !combined.contains("E_") && !combined.contains("E-") && !combined.contains("ELECTRIC") && !combined.contains("BATTERY") && !combined.contains("TOTO")
            }
            "CAB", "CAR", "CAB_MINI", "CAB_PREMIUM", "CAB_XL", "TAXI" -> {
                combined.contains("CAR") || combined.contains("CAB") || combined.contains("TAXI") ||
                combined.contains("SEDAN") || combined.contains("HATCHBACK") || combined.contains("SUV") || combined.contains("VAN")
            }
            else -> combined.contains(q)
        }
    }
}

data class RemoteDriverRoute(
    val driverMobile: String,
    val destinationName: String,
    val destLat: Double,
    val destLng: Double
)

data class RideStatusResult(
    val requestId: String,
    val status: String, // PENDING, ACCEPTED, DECLINED, CANCELLED
    val driverId: String?,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val vehicleNumber: String? = null,
    val driverLat: Double = 0.0,
    val driverLng: Double = 0.0,
    val driverBearing: Float = 0f
)

/**
 * Lightweight, zero-dependency REST Client for Supabase PostgREST API.
 * Securely writes and manages Passenger Ride Requests and connects with Driver App.
 */
object RideRequestClient {
    private const val TAG = "RideRequestClient"

    private fun openConnection(endpoint: String, method: String): HttpURLConnection {
        val url = URL("${SupabaseConfig.SUPABASE_URL}/rest/v1$endpoint")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
        conn.setRequestProperty("Authorization", "Bearer ${SupabaseConfig.SUPABASE_ANON_KEY}")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        return conn
    }

    fun isValidUuid(str: String?): Boolean {
        if (str.isNullOrBlank()) return false
        return try {
            java.util.UUID.fromString(str)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Sends a new ride booking request to Supabase so target driver receives live alert.
     */
    suspend fun createRideRequest(
        passengerName: String = "Glip Rider",
        passengerMobile: String = "9876543210",
        pickupAddress: String,
        pickupLat: Double,
        pickupLng: Double,
        dropAddress: String,
        dropLat: Double,
        dropLng: Double,
        targetDriverId: String? = null,
        requestedSeats: Int = 1,
        fare: Double = 40.0,
        userId: String? = null,
        userName: String? = null,
        userPhone: String? = null,
        otp: String = "1234"
    ): Result<String> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = openConnection("/${SupabaseConfig.TABLE_RIDE_REQUESTS}", "POST").apply {
                doOutput = true
                setRequestProperty("Prefer", "return=representation")
            }

            val finalName = (userName ?: passengerName).ifBlank { "Glip Rider" }
            val finalMobile = (userPhone ?: passengerMobile).ifBlank { "9876543210" }

            val payload = JSONObject().apply {
                put("passenger_name", finalName)
                put("passenger_mobile", finalMobile)
                if (!targetDriverId.isNullOrBlank() && isValidUuid(targetDriverId)) {
                    put("driver_id", targetDriverId)
                }
                put("pickup_name", pickupAddress)
                put("pickup_lat", pickupLat)
                put("pickup_lng", pickupLng)
                put("drop_name", dropAddress)
                put("drop_lat", dropLat)
                put("drop_lng", dropLng)
                put("requested_seats", requestedSeats)
                put("status", "PENDING")
                put("fare_amount", fare)
                put("otp", otp)
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Ride request created in Supabase: $responseBody")
                val arr = JSONArray(responseBody)
                val id = if (arr.length() > 0) arr.getJSONObject(0).optString("id") else ""
                Result.success(id)
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "Failed to create ride request. HTTP $responseCode: $errorBody")
                Result.failure(Exception("Supabase HTTP $responseCode: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error creating ride request", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Poll current status of a ride request (PENDING -> ACCEPTED / DECLINED / CANCELLED).
     */
    suspend fun pollRideStatus(requestId: String): RideStatusResult? = withContext(Dispatchers.IO) {
        if (requestId.isBlank()) return@withContext null
        var connection: HttpURLConnection? = null
        try {
            connection = openConnection("/${SupabaseConfig.TABLE_RIDE_REQUESTS}?id=eq.$requestId&select=*", "GET")
            val code = connection.responseCode
            if (code in 200..299) {
                val body = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val arr = JSONArray(body)
                if (arr.length() > 0) {
                    val obj = arr.getJSONObject(0)
                    val status = obj.optString("status", "PENDING")
                    val driverId = obj.optString("driver_id", "").ifBlank { null }

                    var dName = "Driver"
                    var dPhone = driverId ?: ""
                    var vNum = "DL 01 ** 4812"
                    var dLat = 0.0
                    var dLng = 0.0
                    var dAngle = 0f

                    // If accepted and driverId is present, fetch live driver location & details
                    if (status.equals("ACCEPTED", ignoreCase = true) && driverId != null) {
                        val driverDetails = fetchDriverByIdOrMobile(driverId)
                        if (driverDetails != null) {
                            dName = driverDetails.fullName
                            dPhone = driverDetails.mobileNumber
                            vNum = driverDetails.vehicleNumber.ifBlank { "Auto" }
                            dLat = driverDetails.currentLat
                            dLng = driverDetails.currentLng
                            dAngle = driverDetails.bearing
                        }
                    }

                    return@withContext RideStatusResult(
                        requestId = requestId,
                        status = status,
                        driverId = driverId,
                        driverName = dName,
                        driverPhone = dPhone,
                        vehicleNumber = vNum,
                        driverLat = dLat,
                        driverLng = dLng,
                        driverBearing = dAngle
                    )
                }
            }
            null
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Cancel an active ride request.
     */
    suspend fun cancelRideRequest(requestId: String): Boolean = withContext(Dispatchers.IO) {
        if (requestId.isBlank()) return@withContext false
        var connection: HttpURLConnection? = null
        try {
            connection = openConnection("/${SupabaseConfig.TABLE_RIDE_REQUESTS}?id=eq.$requestId", "PATCH").apply {
                doOutput = true
            }
            val payload = JSONObject().apply {
                put("status", "CANCELLED")
            }
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }
            connection.responseCode in 200..299
        } catch (e: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Fetch active online drivers from Supabase `drivers` table.
     */
    suspend fun fetchActiveDrivers(): List<RemoteDriver> = withContext(Dispatchers.IO) {
        val list = mutableListOf<RemoteDriver>()
        var connection: HttpURLConnection? = null
        try {
            connection = openConnection("/${SupabaseConfig.TABLE_DRIVERS}?is_online=eq.true&select=*&order=last_updated_at.desc&limit=25", "GET")
            val code = connection.responseCode
            if (code in 200..299) {
                val body = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val arr = JSONArray(body)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val isOnline = obj.optBoolean("is_online", false)
                    if (!isOnline) continue

                    val id = obj.optString("id")
                    val name = obj.optString("full_name", "Driver Partner")
                    val mob = obj.optString("mobile_number", "")
                    val vNum = obj.optString("vehicle_number", "")
                    val vMod = obj.optString("vehicle_model", "")
                    val vType = obj.optString("vehicle_type", "AUTO")
                    val totalSeats = obj.optInt("total_seats", 4)
                    val availSeats = obj.optInt("available_seats", 3)
                    val lat = obj.optDouble("current_lat", 0.0)
                    val lng = obj.optDouble("current_lng", 0.0)
                    val bearing = obj.optDouble("bearing", 0.0).toFloat()

                    list.add(
                        RemoteDriver(
                            id = id,
                            fullName = name,
                            mobileNumber = mob,
                            vehicleNumber = vNum,
                            vehicleModel = vMod,
                            vehicleType = vType,
                            totalSeats = totalSeats,
                            availableSeats = availSeats,
                            currentLat = lat,
                            currentLng = lng,
                            bearing = bearing,
                            isOnline = isOnline
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchActiveDrivers error: ${e.message}")
        } finally {
            connection?.disconnect()
        }
        list
    }

    /**
     * Fetch active destinations from Supabase `driver_routes` table.
     */
    suspend fun fetchActiveDriverRoutes(): Map<String, RemoteDriverRoute> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<String, RemoteDriverRoute>()
        var connection: HttpURLConnection? = null
        try {
            connection = openConnection("/${SupabaseConfig.TABLE_DRIVER_ROUTES}?is_active=eq.true&select=*", "GET")
            val code = connection.responseCode
            if (code in 200..299) {
                val body = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val arr = JSONArray(body)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val drvId = obj.optString("driver_id", "")
                    val mob = obj.optString("driver_mobile", "")
                    val destName = obj.optString("destination_name", "")
                    val destLat = obj.optDouble("dest_lat", 0.0)
                    val destLng = obj.optDouble("dest_lng", 0.0)
                    val route = RemoteDriverRoute(drvId.ifBlank { mob }, destName, destLat, destLng)
                    if (drvId.isNotBlank()) {
                        map[drvId] = route
                    }
                    if (mob.isNotBlank()) {
                        map[mob] = route
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchActiveDriverRoutes error: ${e.message}")
        } finally {
            connection?.disconnect()
        }
        map
    }

    private suspend fun fetchDriverByIdOrMobile(identifier: String): RemoteDriver? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val filter = if (isValidUuid(identifier)) "id=eq.$identifier" else "mobile_number=eq.$identifier"
            connection = openConnection("/${SupabaseConfig.TABLE_DRIVERS}?$filter&select=*", "GET")
            val code = connection.responseCode
            if (code in 200..299) {
                val body = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val arr = JSONArray(body)
                if (arr.length() > 0) {
                    val obj = arr.getJSONObject(0)
                    return@withContext RemoteDriver(
                        id = obj.optString("id"),
                        fullName = obj.optString("full_name", "Driver"),
                        mobileNumber = obj.optString("mobile_number", ""),
                        vehicleNumber = obj.optString("vehicle_number", "Auto"),
                        vehicleModel = obj.optString("vehicle_model", ""),
                        vehicleType = obj.optString("vehicle_type", "AUTO"),
                        totalSeats = obj.optInt("total_seats", 4),
                        availableSeats = obj.optInt("available_seats", 3),
                        currentLat = obj.optDouble("current_lat", 0.0),
                        currentLng = obj.optDouble("current_lng", 0.0),
                        bearing = obj.optDouble("bearing", 0.0).toFloat(),
                        isOnline = obj.optBoolean("is_online", true)
                    )
                }
            }
            null
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
