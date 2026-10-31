package com.haze.app.data.supabase

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
import java.net.URLEncoder

/**
 * Data model for a service lead / enquiry in the Haze Services Provider database.
 */
data class ServiceLeadItem(
    val id: String,
    val providerId: String? = null,
    val customerName: String,
    val customerPhone: String,
    val serviceCategory: String,
    val description: String,
    val status: String,
    val city: String,
    val customerId: String? = null,
    val customerAddress: String? = null,
    val visitingCharges: Double = 0.0,
    val quotedAmount: Double = 0.0,
    val estimatedArrival: String? = null,
    val providerNotes: String? = null,
    val createdAt: String? = null
)

/**
 * REST Client connecting Haze Passenger / Customer App directly to the
 * Haze Services Provider Supabase project (https://aqksqqjoioztotizzvad.supabase.co).
 */
object ServicesSupabaseClient {
    private const val TAG = "ServicesSupabaseClient"

    private fun openConnection(endpoint: String, method: String): HttpURLConnection {
        val base = SupabaseConfig.SERVICES_SUPABASE_URL.trimEnd('/')
        val url = URL("$base/rest/v1$endpoint")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("apikey", SupabaseConfig.SERVICES_SUPABASE_ANON_KEY)
        conn.setRequestProperty("Authorization", "Bearer ${SupabaseConfig.SERVICES_SUPABASE_ANON_KEY}")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        return conn
    }

    /**
     * Submits a non-travel service request (e.g., Bathroom Cleaning, Electrician, Plumber)
     * directly into the Shared Haze Services Provider Supabase database.
     */
    suspend fun createServiceLead(
        customerId: String,
        customerName: String,
        customerPhone: String,
        serviceCategory: String,
        serviceTitle: String,
        description: String,
        city: String,
        address: String? = null
    ): Result<ServiceLeadItem> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = openConnection("/${SupabaseConfig.TABLE_SERVICES_LEADS}", "POST").apply {
                doOutput = true
                setRequestProperty("Prefer", "return=representation")
            }

            val cleanPhone = customerPhone.replace(Regex("[^0-9]"), "").takeLast(10).ifBlank { "9876543210" }
            val cleanCity = city.trim().ifBlank { "Bhopal" }

            val payload = JSONObject().apply {
                put("customer_id", customerId)
                put("customer_name", customerName.ifBlank { "Haze Customer" })
                put("customer_phone", cleanPhone)
                put("service_category", serviceCategory)
                put("description", if (description.isNotBlank()) "$serviceTitle: $description" else serviceTitle)
                put("city", cleanCity)
                put("customer_address", address ?: "")
                put("status", "new")
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Lead created in Services DB: $responseText")
                val jsonArray = JSONArray(responseText)
                if (jsonArray.length() > 0) {
                    val item = parseLeadItem(jsonArray.getJSONObject(0))
                    Result.success(item)
                } else {
                    Result.failure(Exception("Empty response array from Supabase"))
                }
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "Failed to create lead, HTTP $responseCode: $errorBody")
                Result.failure(Exception("HTTP $responseCode: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error posting lead to Services Supabase", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Fetches all service requests made by this customer to check status,
     * provider quotes, visiting charges, and estimated arrival.
     */
    suspend fun fetchCustomerLeads(customerPhone: String): Result<List<ServiceLeadItem>> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val cleanPhone = customerPhone.replace(Regex("[^0-9]"), "").takeLast(10)
            val endpoint = "/${SupabaseConfig.TABLE_SERVICES_LEADS}?customer_phone=eq.$cleanPhone&order=created_at.desc"
            connection = openConnection(endpoint, "GET")

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                val list = mutableListOf<ServiceLeadItem>()
                for (i in 0 until jsonArray.length()) {
                    list.add(parseLeadItem(jsonArray.getJSONObject(i)))
                }
                Result.success(list)
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Result.failure(Exception("HTTP $responseCode: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching customer leads", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Customer accepts the quotation sent by a provider.
     */
    suspend fun acceptQuotation(leadId: String): Result<Unit> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val endpoint = "/${SupabaseConfig.TABLE_SERVICES_LEADS}?id=eq.$leadId"
            connection = openConnection(endpoint, "PATCH").apply {
                doOutput = true
                setRequestProperty("Prefer", "return=minimal")
            }

            val payload = JSONObject().apply {
                put("status", "accepted")
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                Result.success(Unit)
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Result.failure(Exception("HTTP $responseCode: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting quotation", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseLeadItem(obj: JSONObject): ServiceLeadItem {
        return ServiceLeadItem(
            id = obj.optString("id", ""),
            providerId = if (obj.has("provider_id") && !obj.isNull("provider_id")) obj.getString("provider_id") else null,
            customerName = obj.optString("customer_name", "Customer"),
            customerPhone = obj.optString("customer_phone", ""),
            serviceCategory = obj.optString("service_category", "general"),
            description = obj.optString("description", ""),
            status = obj.optString("status", "new"),
            city = obj.optString("city", "Bhopal"),
            customerId = if (obj.has("customer_id") && !obj.isNull("customer_id")) obj.getString("customer_id") else null,
            customerAddress = if (obj.has("customer_address") && !obj.isNull("customer_address")) obj.getString("customer_address") else null,
            visitingCharges = obj.optDouble("visiting_charges", 0.0),
            quotedAmount = obj.optDouble("quoted_amount", 0.0),
            estimatedArrival = if (obj.has("estimated_arrival") && !obj.isNull("estimated_arrival")) obj.getString("estimated_arrival") else null,
            providerNotes = if (obj.has("provider_notes") && !obj.isNull("provider_notes")) obj.getString("provider_notes") else null,
            createdAt = if (obj.has("created_at") && !obj.isNull("created_at")) obj.getString("created_at") else null
        )
    }
}
