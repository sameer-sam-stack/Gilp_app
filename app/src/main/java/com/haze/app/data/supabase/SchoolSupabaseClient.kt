package com.haze.app.data.supabase

import android.util.Log
import com.haze.app.ui.school.SchoolBookingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Dedicated Lightweight REST Client for School Vehicle Supabase Database.
 * Connects directly to the user's dedicated Supabase project (https://uugzolriwmbhssgfjbap.supabase.co).
 */
object SchoolSupabaseClient {
    private const val TAG = "SchoolSupabaseClient"

    private fun openConnection(endpoint: String, method: String): HttpURLConnection {
        val base = SupabaseConfig.SCHOOL_SUPABASE_URL.trimEnd('/')
        val url = URL("$base/rest/v1$endpoint")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("apikey", SupabaseConfig.SCHOOL_SUPABASE_ANON_KEY)
        conn.setRequestProperty("Authorization", "Bearer ${SupabaseConfig.SCHOOL_SUPABASE_ANON_KEY}")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        return conn
    }

    /**
     * Submits a new school vehicle booking request into the dedicated Supabase database.
     */
    suspend fun createSchoolBooking(
        userId: String,
        parentName: String,
        parentPhone: String,
        studentName: String,
        studentClass: String,
        pickupLocation: String,
        pickupLat: Double,
        pickupLng: Double,
        dropoffLocation: String,
        dropoffLat: Double,
        dropoffLng: Double,
        pickupTime: String,
        dropTime: String,
        fareEstimate: Double = 80.0,
        requestCode: String = (100000..999999).random().toString()
    ): Result<SchoolBookingItem> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = openConnection("/${SupabaseConfig.TABLE_SCHOOL_BOOKINGS}", "POST").apply {
                doOutput = true
                setRequestProperty("Prefer", "return=representation")
            }

            val cleanPhone = parentPhone.replace(Regex("[^0-9]"), "").takeLast(10).ifBlank { "9876543210" }

            val payload = JSONObject().apply {
                put("request_code", requestCode)
                put("user_id", userId)
                put("parent_name", parentName.ifBlank { "Parent" })
                put("parent_phone", cleanPhone)
                put("student_name", studentName)
                put("student_class", studentClass)
                put("pickup_location", pickupLocation)
                put("pickup_lat", pickupLat)
                put("pickup_lng", pickupLng)
                put("dropoff_location", dropoffLocation)
                put("dropoff_lat", dropoffLat)
                put("dropoff_lng", dropoffLng)
                put("pickup_time", pickupTime)
                put("drop_time", dropTime)
                put("fare_estimate", fareEstimate)
                put("status", "Requested")
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "School booking created in Supabase: $responseBody")
                val arr = JSONArray(responseBody)
                if (arr.length() > 0) {
                    val obj = arr.getJSONObject(0)
                    val item = parseBookingJsonObject(obj)
                    Result.success(item)
                } else {
                    Result.success(
                        SchoolBookingItem(
                            requestCode = requestCode,
                            studentName = studentName,
                            studentClass = studentClass,
                            pickupLocation = pickupLocation,
                            dropoffLocation = dropoffLocation,
                            pickupTime = pickupTime,
                            dropTime = dropTime,
                            fareEstimate = fareEstimate
                        )
                    )
                }
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "Failed to create school booking. HTTP $responseCode: $errorBody")
                Result.failure(Exception("Supabase HTTP $responseCode: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error creating school booking", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Fetches school bookings history from the dedicated Supabase database for this user/parent.
     */
    suspend fun fetchSchoolBookings(
        userId: String? = null,
        parentPhone: String? = null
    ): Result<List<SchoolBookingItem>> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val cleanPhone = parentPhone?.replace(Regex("[^0-9]"), "")?.takeLast(10)

            val queryFilter = when {
                !userId.isNullOrBlank() && !cleanPhone.isNullOrBlank() ->
                    "?or=(user_id.eq.$userId,parent_phone.eq.$cleanPhone,parent_phone.ilike.*$cleanPhone*)&order=created_at.desc&limit=50"
                !userId.isNullOrBlank() ->
                    "?user_id=eq.$userId&order=created_at.desc&limit=50"
                !cleanPhone.isNullOrBlank() ->
                    "?parent_phone=ilike.*$cleanPhone*&order=created_at.desc&limit=50"
                else ->
                    "?order=created_at.desc&limit=50"
            }

            connection = openConnection("/${SupabaseConfig.TABLE_SCHOOL_BOOKINGS}$queryFilter", "GET")
            val code = connection.responseCode
            if (code in 200..299) {
                val body = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val arr = JSONArray(body)
                val list = mutableListOf<SchoolBookingItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(parseBookingJsonObject(obj))
                }
                Log.d(TAG, "Fetched ${list.size} school bookings from Supabase")
                Result.success(list)
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.w(TAG, "Fetch school bookings HTTP $code: $errorBody")
                Result.failure(Exception("Supabase HTTP $code: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching school bookings from Supabase", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Cancels an existing school booking in Supabase.
     */
    suspend fun cancelSchoolBooking(bookingId: String): Boolean = withContext(Dispatchers.IO) {
        if (bookingId.isBlank()) return@withContext false
        var connection: HttpURLConnection? = null
        try {
            connection = openConnection("/${SupabaseConfig.TABLE_SCHOOL_BOOKINGS}?id=eq.$bookingId", "PATCH").apply {
                doOutput = true
            }
            val payload = JSONObject().apply {
                put("status", "Cancelled")
            }
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }
            connection.responseCode in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling school booking", e)
            false
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseBookingJsonObject(obj: JSONObject): SchoolBookingItem {
        val rawDate = obj.optString("created_at", "")
        val formattedDate = formatIsoTimestamp(rawDate)
        val reqCode = if (obj.has("request_code") && !obj.isNull("request_code")) {
            obj.getString("request_code")
        } else {
            val id = obj.optString("id", "")
            if (id.length >= 6) id.take(6) else (100000..999999).random().toString()
        }

        return SchoolBookingItem(
            id = obj.optString("id", ""),
            requestCode = reqCode,
            userId = obj.optString("user_id", ""),
            parentName = obj.optString("parent_name", ""),
            parentPhone = obj.optString("parent_phone", ""),
            studentName = obj.optString("student_name", "Student"),
            studentClass = obj.optString("student_class", ""),
            pickupLocation = obj.optString("pickup_location", ""),
            pickupLat = obj.optDouble("pickup_lat", 0.0),
            pickupLng = obj.optDouble("pickup_lng", 0.0),
            dropoffLocation = obj.optString("dropoff_location", ""),
            dropoffLat = obj.optDouble("dropoff_lat", 0.0),
            dropoffLng = obj.optDouble("dropoff_lng", 0.0),
            pickupTime = obj.optString("pickup_time", "06:00 AM"),
            dropTime = obj.optString("drop_time", "08:00 AM"),
            bookingDate = formattedDate.ifBlank { "Today" },
            status = obj.optString("status", "Requested"),
            fareEstimate = obj.optDouble("fare_estimate", 80.0),
            driverId = if (obj.has("driver_id") && !obj.isNull("driver_id")) obj.getString("driver_id") else null,
            driverName = if (obj.has("driver_name") && !obj.isNull("driver_name")) obj.getString("driver_name") else null,
            driverPhone = if (obj.has("driver_phone") && !obj.isNull("driver_phone")) obj.getString("driver_phone") else null,
            vehicleNumber = if (obj.has("vehicle_number") && !obj.isNull("vehicle_number")) obj.getString("vehicle_number") else null
        )
    }

    private fun formatIsoTimestamp(iso: String): String {
        if (iso.isBlank()) return "Today"
        return try {
            // e.g. 2026-10-19T05:46:00+00:00 or 2026-10-19T05:46:00.123Z
            val clean = iso.replace("Z", "+00:00")
            val sdfIn = if (clean.contains(".")) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault())
            } else {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            }
            sdfIn.timeZone = TimeZone.getTimeZone("UTC")
            val date: Date = sdfIn.parse(clean) ?: return "Today"

            val sdfOut = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            sdfOut.format(date)
        } catch (_: Exception) {
            "Today"
        }
    }
}
