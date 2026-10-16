package com.glip.app.data.supabase

/**
 * Supabase Backend Configuration for Glip Mobility.
 * Connects Glip_go (Passenger App) with glip_driver_app (Driver App) & Admin Panel.
 */
object SupabaseConfig {
    /**
     * Supabase Project URL
     */
    var SUPABASE_URL: String = "https://yrvustyzhhiciyrawwxr.supabase.co"

    /**
     * Supabase Public Anonymous API Key (Safe for client-side queries with RLS)
     */
    var SUPABASE_ANON_KEY: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlydnVzdHl6aGhpY2l5cmF3d3hyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTE3NDc1ODcsImV4cCI6MjEwNzMyMzU4N30.TrQEX8ANq4ogMhh-IiWGUYjU2l8NuHFjqn8UPBY91Ik"

    /**
     * Supabase REST endpoint for ride requests
     */
    const val TABLE_RIDE_REQUESTS: String = "ride_requests"
}
