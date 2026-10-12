package com.glip.app.data.remote

/**
 * Supabase Project Configuration.
 *
 * To connect your Supabase database:
 * 1. Log in to https://supabase.com
 * 2. Create a project or open your project settings -> API.
 * 3. Copy "Project URL" into SUPABASE_URL.
 * 4. Copy "anon public key" into SUPABASE_ANON_KEY.
 * 5. Run the provided SQL in Supabase SQL Editor to create the 'users' table.
 */
object SupabaseConfig {
    // User Supabase Project URL
    var SUPABASE_URL: String = "https://yrvustyzhhiciyrawwxr.supabase.co"

    // User Supabase Anon Public Key
    var SUPABASE_ANON_KEY: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlydnVzdHl6aGhpY2l5cmF3d3hyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTE3NDc1ODcsImV4cCI6MjEwNzMyMzU4N30.TrQEX8ANq4ogMhh-IiWGUYjU2l8NuHFjqn8UPBY91Ik"

    const val TABLE_USERS = "users"
}
