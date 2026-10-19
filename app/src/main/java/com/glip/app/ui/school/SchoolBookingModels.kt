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
    val requestCode: String = (100000..999999).random().toString(),
    val userId: String = "",
    val parentName: String = "",
    val parentPhone: String = "",
    val studentName: String = "",
    val studentClass: String = "",
    val pickupLocation: String = "",
    val pickupLat: Double = 0.0,
    val pickupLng: Double = 0.0,
    val dropoffLocation: String = "",
    val dropoffLat: Double = 0.0,
    val dropoffLng: Double = 0.0,
    val pickupTime: String = "06:00 AM",
    val dropTime: String = "08:00 AM",
    val bookingDate: String = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date()),
    val status: String = "Requested",
    val fareEstimate: Double = 80.0,
    val driverId: String? = null,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val vehicleNumber: String? = null
)

/**
 * Local repository to persist and retrieve School Vehicle bookings (Offline Cache).
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
                val id = obj.optString("id", UUID.randomUUID().toString())
                if (id == "sch-demo-1" || id.startsWith("sch-demo")) continue

                list.add(
                    SchoolBookingItem(
                        id = id,
                        requestCode = obj.optString("requestCode", (100000..999999).random().toString()),
                        userId = obj.optString("userId", ""),
                        parentName = obj.optString("parentName", ""),
                        parentPhone = obj.optString("parentPhone", ""),
                        studentName = obj.optString("studentName", ""),
                        studentClass = obj.optString("studentClass", ""),
                        pickupLocation = obj.optString("pickupLocation", ""),
                        pickupLat = obj.optDouble("pickupLat", 0.0),
                        pickupLng = obj.optDouble("pickupLng", 0.0),
                        dropoffLocation = obj.optString("dropoffLocation", ""),
                        dropoffLat = obj.optDouble("dropoffLat", 0.0),
                        dropoffLng = obj.optDouble("dropoffLng", 0.0),
                        pickupTime = obj.optString("pickupTime", "06:00 AM"),
                        dropTime = obj.optString("dropTime", "08:00 AM"),
                        bookingDate = obj.optString("bookingDate", "Today"),
                        status = obj.optString("status", "Requested"),
                        fareEstimate = obj.optDouble("fareEstimate", 80.0),
                        driverId = if (obj.has("driverId") && !obj.isNull("driverId")) obj.getString("driverId") else null,
                        driverName = if (obj.has("driverName") && !obj.isNull("driverName")) obj.getString("driverName") else null,
                        driverPhone = if (obj.has("driverPhone") && !obj.isNull("driverPhone")) obj.getString("driverPhone") else null,
                        vehicleNumber = if (obj.has("vehicleNumber") && !obj.isNull("vehicleNumber")) obj.getString("vehicleNumber") else null
                    )
                )
            }
        } catch (e: Exception) {
            return defaultBookings()
        }
        return list
    }

    fun saveBooking(context: Context, item: SchoolBookingItem) {
        val current = getBookings(context).toMutableList()
        current.removeAll { it.id == item.id }
        current.add(0, item)
        saveBookingsList(context, current)
    }

    fun saveBookingsList(context: Context, items: List<SchoolBookingItem>) {
        val jsonArray = JSONArray()
        for (b in items) {
            if (b.id == "sch-demo-1" || b.id.startsWith("sch-demo")) continue
            val obj = JSONObject().apply {
                put("id", b.id)
                put("requestCode", b.requestCode)
                put("userId", b.userId)
                put("parentName", b.parentName)
                put("parentPhone", b.parentPhone)
                put("studentName", b.studentName)
                put("studentClass", b.studentClass)
                put("pickupLocation", b.pickupLocation)
                put("pickupLat", b.pickupLat)
                put("pickupLng", b.pickupLng)
                put("dropoffLocation", b.dropoffLocation)
                put("dropoffLat", b.dropoffLat)
                put("dropoffLng", b.dropoffLng)
                put("pickupTime", b.pickupTime)
                put("dropTime", b.dropTime)
                put("bookingDate", b.bookingDate)
                put("status", b.status)
                put("fareEstimate", b.fareEstimate)
                if (b.driverId != null) put("driverId", b.driverId)
                if (b.driverName != null) put("driverName", b.driverName)
                if (b.driverPhone != null) put("driverPhone", b.driverPhone)
                if (b.vehicleNumber != null) put("vehicleNumber", b.vehicleNumber)
            }
            jsonArray.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BOOKINGS, jsonArray.toString()).apply()
    }

    private fun defaultBookings(): List<SchoolBookingItem> {
        return emptyList()
    }
}

/**
 * Real-world catalog of schools in Rajnandgaon, Chhattisgarh.
 */
data class RajnandgaonSchool(
    val name: String,
    val address: String,
    val area: String,
    val latitude: Double,
    val longitude: Double
)

object RajnandgaonSchoolsCatalog {
    val schools = listOf(
        RajnandgaonSchool(
            name = "Delhi Public School (DPS)",
            address = "G.E. Road, Near Bye-Pass, Rajnandgaon, Chhattisgarh 491441",
            area = "G.E. Road",
            latitude = 21.1120,
            longitude = 81.0450
        ),
        RajnandgaonSchool(
            name = "Yugantar Public School",
            address = "Yugantar Nagar, Rajnandgaon - Durg Bypass, Rajnandgaon, CG 491441",
            area = "Yugantar Nagar",
            latitude = 21.1085,
            longitude = 81.0520
        ),
        RajnandgaonSchool(
            name = "Kendriya Vidyalaya (KV)",
            address = "Pendri Road, Near Collectorate, Rajnandgaon, Chhattisgarh 491441",
            area = "Pendri",
            latitude = 21.0930,
            longitude = 81.0180
        ),
        RajnandgaonSchool(
            name = "St. Xavier's High School",
            address = "Kailash Nagar, Behind Bus Stand, Rajnandgaon, Chhattisgarh 491441",
            area = "Kailash Nagar",
            latitude = 21.1025,
            longitude = 81.0350
        ),
        RajnandgaonSchool(
            name = "Weslyan English Medium Hr. Sec. School",
            address = "Station Road, Opp. Railway Station, Rajnandgaon, Chhattisgarh 491441",
            area = "Station Road",
            latitude = 21.0990,
            longitude = 81.0380
        ),
        RajnandgaonSchool(
            name = "Neeraj Public School",
            address = "Chikhli, Near Industrial Area, Rajnandgaon, Chhattisgarh 491441",
            area = "Chikhli",
            latitude = 21.0850,
            longitude = 81.0410
        ),
        RajnandgaonSchool(
            name = "Saraswati Shishu Mandir Hr. Sec. School",
            address = "Ganj Para, Main Market, Rajnandgaon, Chhattisgarh 491441",
            area = "Ganj Para",
            latitude = 21.0965,
            longitude = 81.0325
        ),
        RajnandgaonSchool(
            name = "Royal Kids Convent",
            address = "Kailash Nagar, Near Church, Rajnandgaon, Chhattisgarh 491441",
            area = "Kailash Nagar",
            latitude = 21.1010,
            longitude = 81.0340
        ),
        RajnandgaonSchool(
            name = "Krishna Public School (KPS)",
            address = "National Highway 53, Rajnandgaon, Chhattisgarh 491441",
            area = "NH 53 Bypass",
            latitude = 21.1150,
            longitude = 81.0480
        ),
        RajnandgaonSchool(
            name = "Gayatri Vidya Peeth",
            address = "Shanti Nagar, Rajnandgaon, Chhattisgarh 491441",
            area = "Shanti Nagar",
            latitude = 21.0980,
            longitude = 81.0260
        ),
        RajnandgaonSchool(
            name = "Shri Guru Nanak Public School",
            address = "Gurudwara Road, Ganj Para, Rajnandgaon, Chhattisgarh 491441",
            area = "Ganj Para",
            latitude = 21.0955,
            longitude = 81.0310
        ),
        RajnandgaonSchool(
            name = "Somani Public School",
            address = "Somani, Rajnandgaon, Chhattisgarh 491441",
            area = "Somani",
            latitude = 21.1280,
            longitude = 81.0650
        ),
        RajnandgaonSchool(
            name = "Podar Learn School",
            address = "Mohara Road, Rajnandgaon, Chhattisgarh 491441",
            area = "Mohara Road",
            latitude = 21.0910,
            longitude = 81.0490
        ),
        RajnandgaonSchool(
            name = "Holy Cross Higher Secondary School",
            address = "Civil Lines, Near Circuit House, Rajnandgaon, Chhattisgarh 491441",
            area = "Civil Lines",
            latitude = 21.1040,
            longitude = 81.0300
        ),
        RajnandgaonSchool(
            name = "Bharat Mata English Medium School",
            address = "Mamta Nagar, G.E. Road, Rajnandgaon, Chhattisgarh 491441",
            area = "Mamta Nagar",
            latitude = 21.1015,
            longitude = 81.0375
        ),
        RajnandgaonSchool(
            name = "Government Multipurpose Hr. Sec. School",
            address = "Kachehri Chowk, Rajnandgaon, Chhattisgarh 491441",
            area = "Kachehri Chowk",
            latitude = 21.0985,
            longitude = 81.0290
        ),
        RajnandgaonSchool(
            name = "Adarsh Bal Mandir School",
            address = "Ganj Chowk, Rajnandgaon, Chhattisgarh 491441",
            area = "Ganj Chowk",
            latitude = 21.0972,
            longitude = 81.0330
        )
    )

    fun searchSchools(query: String): List<RajnandgaonSchool> {
        val clean = query.trim().lowercase()
        if (clean.isBlank()) return schools
        return schools.filter {
            it.name.lowercase().contains(clean) ||
            it.address.lowercase().contains(clean) ||
            it.area.lowercase().contains(clean)
        }
    }
}
