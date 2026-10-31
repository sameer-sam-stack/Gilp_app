package com.haze.app.data.supabase

/**
 * Supabase Backend Configuration for Haze Mobility.
 * Connects Haze (Passenger App) with Haze Driver App (Driver App) & Admin Panel.
 */
object SupabaseConfig {
    /**
     * Shared Driver & Ride Requests Supabase Project URL (Connects Passenger App directly to Haze Driver App)
     */
    var SUPABASE_URL: String = "https://prolxgzzgsnznhvatnye.supabase.co"

    /**
     * Shared Driver & Ride Requests Supabase Public Anonymous API Key
     */
    var SUPABASE_ANON_KEY: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InByb2x4Z3p6Z3Nuem5odmF0bnllIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTIwOTcwMDAsImV4cCI6MjEwNzY3MzAwMH0.P5LwxLaB7W7yOwll-4Sp53n79NaX2FnnrXWsnH3cpyY"

    /**
     * Passenger User Database URL (Used for User account restore)
     */
    const val USER_SUPABASE_URL: String = "https://yrvustyzhhiciyrawwxr.supabase.co"
    const val USER_SUPABASE_ANON_KEY: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlydnVzdHl6aGhpY2l5cmF3d3hyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTE3NDc1ODcsImV4cCI6MjEwNzMyMzU4N30.TrQEX8ANq4ogMhh-IiWGUYjU2l8NuHFjqn8UPBY91Ik"

    const val TABLE_RIDE_REQUESTS: String = "ride_requests"
    const val TABLE_DRIVERS: String = "drivers"
    const val TABLE_DRIVER_ROUTES: String = "driver_routes"
    const val TABLE_USERS: String = "users"

    /**
     * Dedicated School Vehicle Supabase Database URL & Anon Key
     */
    const val SCHOOL_SUPABASE_URL: String = "https://uugzolriwmbhssgfjbap.supabase.co"
    const val SCHOOL_SUPABASE_ANON_KEY: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV1Z3pvbHJpd21iaHNzZ2ZqYmFwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTIzNTYzMTQsImV4cCI6MjEwNzkzMjMxNH0.rVBGf5CDhLSevaXbZrMptk0-w1xU4YOl2UIsr5FzeP8"
    const val TABLE_SCHOOL_BOOKINGS: String = "school_bookings"

    /**
     * Dedicated Home & Local Services Supabase Database URL & Anon Key (Provider App & Customer Directory)
     */
    const val SERVICES_SUPABASE_URL: String = "https://aqksqqjoioztotizzvad.supabase.co"
    const val SERVICES_SUPABASE_ANON_KEY: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFxa3NxcWpvaW96dG90aXp6dmFkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTM0NDQ5NTMsImV4cCI6MjEwOTAyMDk1M30.7TzA1HP9nry7bCpD1m23xX0m7q1ZakReiGxVf5deHeU"
    const val TABLE_SERVICES_LEADS: String = "leads"
}
