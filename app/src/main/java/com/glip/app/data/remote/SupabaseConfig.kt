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
    // Replace with your real Supabase Project URL (e.g. "https://abcdefghijkl.supabase.co")
    var SUPABASE_URL: String = "https://YOUR_SUPABASE_PROJECT_URL.supabase.co"

    // Replace with your real Supabase Anon Public Key
    var SUPABASE_ANON_KEY: String = "YOUR_SUPABASE_ANON_KEY"

    const val TABLE_USERS = "users"
}
