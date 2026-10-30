package com.haze.app.ui.personal

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Data model for Personal Vehicle ride bookings.
 * Strictly stored locally in the app, NOT in any external database.
 */
data class PersonalBookingItem(
    val id: String = UUID.randomUUID().toString(),
    val vehicleName: String, // "Auto Rickshaw", "Bike", "E-Rickshaw", "Mini Cab"
    val vehicleType: String, // "auto", "bike", "erickshaw", "cab"
    val pickupLocation: String,
    val dropoffLocation: String,
    val fare: Double,
    val etaMinutes: Int = 3,
    val bookingDate: String = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date()),
    val status: String = "Confirmed",
    val driverName: String = "Ramesh Verma",
    val vehicleNumber: String = "CG 08 M 4182"
)

/**
 * Local repository to persist and retrieve Personal Vehicle bookings solely in the app's SharedPreferences.
 * No data is sent to Supabase or external servers as per user requirement.
 */
object PersonalBookingRepository {
    private const val PREFS_NAME = "haze_personal_vehicle_bookings"
    private const val KEY_BOOKINGS = "personal_bookings_json"

    fun getBookings(context: Context): List<PersonalBookingItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_BOOKINGS, null) ?: return defaultBookings()
        val list = mutableListOf<PersonalBookingItem>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    PersonalBookingItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        vehicleName = obj.optString("vehicleName", "Auto Rickshaw"),
                        vehicleType = obj.optString("vehicleType", "auto"),
                        pickupLocation = obj.optString("pickupLocation", ""),
                        dropoffLocation = obj.optString("dropoffLocation", ""),
                        fare = obj.optDouble("fare", 70.0),
                        etaMinutes = obj.optInt("etaMinutes", 3),
                        bookingDate = obj.optString("bookingDate", "Today"),
                        status = obj.optString("status", "Confirmed"),
                        driverName = obj.optString("driverName", "Ramesh Verma"),
                        vehicleNumber = obj.optString("vehicleNumber", "CG 08 M 4182")
                    )
                )
            }
        } catch (e: Exception) {
            return defaultBookings()
        }
        return if (list.isEmpty()) defaultBookings() else list
    }

    fun saveBooking(context: Context, item: PersonalBookingItem) {
        val current = getBookings(context).toMutableList()
        current.add(0, item)
        val jsonArray = JSONArray()
        for (b in current) {
            val obj = JSONObject().apply {
                put("id", b.id)
                put("vehicleName", b.vehicleName)
                put("vehicleType", b.vehicleType)
                put("pickupLocation", b.pickupLocation)
                put("dropoffLocation", b.dropoffLocation)
                put("fare", b.fare)
                put("etaMinutes", b.etaMinutes)
                put("bookingDate", b.bookingDate)
                put("status", b.status)
                put("driverName", b.driverName)
                put("vehicleNumber", b.vehicleNumber)
            }
            jsonArray.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BOOKINGS, jsonArray.toString()).apply()
    }

    private fun defaultBookings(): List<PersonalBookingItem> {
        return listOf(
            PersonalBookingItem(
                id = "p-demo-1",
                vehicleName = "Auto Rickshaw",
                vehicleType = "auto",
                pickupLocation = "SH5, Gol Bazar, Mamta Nagar",
                dropoffLocation = "Rajnandgaon Railway Station",
                fare = 60.0,
                etaMinutes = 3,
                bookingDate = "Today, 10:30 AM",
                status = "Completed",
                driverName = "Suresh Sahu",
                vehicleNumber = "CG 08 F 1928"
            )
        )
    }
}
