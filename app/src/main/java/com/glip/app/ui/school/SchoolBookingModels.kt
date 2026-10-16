package com.glip.app.ui.school

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Data model for School Vehicle ride bookings.
 */
data class SchoolBookingItem(
    val id: String = UUID.randomUUID().toString(),
    val studentName: String,
    val studentClass: String,
    val pickupLocation: String,
    val dropoffLocation: String,
    val pickupTime: String,
    val dropTime: String,
    val bookingDate: String = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date()),
    val status: String = "Requested",
    val fareEstimate: Double = 80.0
)

/**
 * Local repository to persist and retrieve School Vehicle bookings.
 */
object SchoolBookingRepository {
    private const val PREFS_NAME = "glip_school_vehicle_bookings"
    private const val KEY_BOOKINGS = "school_bookings_json"

    fun getBookings(context: Context): List<SchoolBookingItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_BOOKINGS, null) ?: return defaultBookings()
        val list = mutableListOf<SchoolBookingItem>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    SchoolBookingItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        studentName = obj.optString("studentName", ""),
                        studentClass = obj.optString("studentClass", ""),
                        pickupLocation = obj.optString("pickupLocation", ""),
                        dropoffLocation = obj.optString("dropoffLocation", ""),
                        pickupTime = obj.optString("pickupTime", "06:00 AM"),
                        dropTime = obj.optString("dropTime", "08:00 AM"),
                        bookingDate = obj.optString("bookingDate", "Today"),
                        status = obj.optString("status", "Requested"),
                        fareEstimate = obj.optDouble("fareEstimate", 80.0)
                    )
                )
            }
        } catch (e: Exception) {
            return defaultBookings()
        }
        return if (list.isEmpty()) defaultBookings() else list
    }

    fun saveBooking(context: Context, item: SchoolBookingItem) {
        val current = getBookings(context).toMutableList()
        current.add(0, item)
        val jsonArray = JSONArray()
        for (b in current) {
            val obj = JSONObject().apply {
                put("id", b.id)
                put("studentName", b.studentName)
                put("studentClass", b.studentClass)
                put("pickupLocation", b.pickupLocation)
                put("dropoffLocation", b.dropoffLocation)
                put("pickupTime", b.pickupTime)
                put("dropTime", b.dropTime)
                put("bookingDate", b.bookingDate)
                put("status", b.status)
                put("fareEstimate", b.fareEstimate)
            }
            jsonArray.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BOOKINGS, jsonArray.toString()).apply()
    }

    private fun defaultBookings(): List<SchoolBookingItem> {
        return listOf(
            SchoolBookingItem(
                id = "sch-demo-1",
                studentName = "Aarav Sharma",
                studentClass = "KG 2 (UKG)",
                pickupLocation = "SH5, Gol Bazar, Mamta Nagar",
                dropoffLocation = "Delhi Public School (DPS)",
                pickupTime = "06:00 AM",
                dropTime = "08:00 AM",
                bookingDate = "Today, 06:15 AM",
                status = "Driver Assigned",
                fareEstimate = 85.0
            )
        )
    }
}
