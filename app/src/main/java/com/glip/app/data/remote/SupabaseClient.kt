package com.glip.app.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object SupabaseClient {
    private const val TAG = "SupabaseClient"

    /**
     * Inserts user full name and phone number into Supabase 'users' table.
     */
    suspend fun insertUser(fullName: String, phoneNumber: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val urlString = "${SupabaseConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/${SupabaseConfig.TABLE_USERS}"

        // If credentials are still placeholders, skip remote call gracefully
        if (SupabaseConfig.SUPABASE_URL.contains("YOUR_SUPABASE_PROJECT_URL") ||
            SupabaseConfig.SUPABASE_ANON_KEY.contains("YOUR_SUPABASE_ANON_KEY")
        ) {
            Log.d(TAG, "Supabase credentials not configured yet. Skipping remote insert.")
            return@withContext Result.success(true)
        }

        try {
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${SupabaseConfig.SUPABASE_ANON_KEY}")
                setRequestProperty("Prefer", "return=minimal")
            }

            val payload = JSONObject().apply {
                put("full_name", fullName)
                put("phone_number", phoneNumber)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                Log.d(TAG, "Successfully inserted user to Supabase: $fullName, $phoneNumber")
                Result.success(true)
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "Failed to insert user to Supabase: $responseCode - $errorStream")
                Result.failure(Exception("Supabase HTTP $responseCode: $errorStream"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting user to Supabase", e)
            Result.failure(e)
        }
    }
}
