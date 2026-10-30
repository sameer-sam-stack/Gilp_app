-- ==============================================================================
-- HAZE MOBILITY: SUPABASE COMPLETE DATABASE SCHEMA
-- (Users Authentication, Account Restoration, and Driver Real-time Ride Requests)
-- Copy & Run this script in your Supabase SQL Editor (https://supabase.com/dashboard)
-- ==============================================================================

-- 1. Create the users table (Supports Phone lookup and Existing Account Restoration)
CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    phone TEXT NOT NULL,
    email TEXT,
    status VARCHAR(30) DEFAULT 'active',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for instant phone number lookups
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);

-- Users Table Row Level Security (RLS)
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow public read on users" ON users;
CREATE POLICY "Allow public read on users"
ON users FOR SELECT
USING (true);

DROP POLICY IF EXISTS "Allow public insert on users" ON users;
CREATE POLICY "Allow public insert on users"
ON users FOR INSERT
WITH CHECK (true);

DROP POLICY IF EXISTS "Allow public update on users" ON users;
CREATE POLICY "Allow public update on users"
ON users FOR UPDATE
USING (true);


-- 2. Create the ride_requests table (Connects Passenger App to Driver App)
CREATE TABLE IF NOT EXISTS ride_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id TEXT NOT NULL,
    user_name TEXT NOT NULL,
    user_phone TEXT NOT NULL,
    pickup_address TEXT NOT NULL,
    pickup_lat DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    pickup_lng DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    dropoff_address TEXT NOT NULL,
    dropoff_lat DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    dropoff_lng DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    status VARCHAR(30) DEFAULT 'requested' CHECK (status IN ('requested', 'accepted', 'in_progress', 'completed', 'cancelled')),
    fare NUMERIC(10, 2) DEFAULT 0.00,
    driver_id TEXT,
    driver_name TEXT,
    driver_phone TEXT,
    vehicle_number TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Fast querying indexes for Driver app filtering and real-time feeds
CREATE INDEX IF NOT EXISTS idx_ride_requests_status ON ride_requests(status);
CREATE INDEX IF NOT EXISTS idx_ride_requests_created ON ride_requests(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ride_requests_driver ON ride_requests(driver_id);

-- Ride Requests Row Level Security (RLS)
ALTER TABLE ride_requests ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow public insert on ride_requests" ON ride_requests;
CREATE POLICY "Allow public insert on ride_requests" 
ON ride_requests FOR INSERT 
WITH CHECK (true);

DROP POLICY IF EXISTS "Allow public select on ride_requests" ON ride_requests;
CREATE POLICY "Allow public select on ride_requests" 
ON ride_requests FOR SELECT 
USING (true);

DROP POLICY IF EXISTS "Allow public update on ride_requests" ON ride_requests;
CREATE POLICY "Allow public update on ride_requests" 
ON ride_requests FOR UPDATE 
USING (true);


-- 3. Enable Supabase Realtime for instant Driver & Passenger sync
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' AND tablename = 'ride_requests'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE ride_requests;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' AND tablename = 'users'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE users;
    END IF;
END $$;


-- 4. Auto-update timestamp trigger
CREATE OR REPLACE FUNCTION update_timestamp_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_update_ride_requests_timestamp ON ride_requests;
CREATE TRIGGER trg_update_ride_requests_timestamp
BEFORE UPDATE ON ride_requests
FOR EACH ROW
EXECUTE FUNCTION update_timestamp_column();

DROP TRIGGER IF EXISTS trg_update_users_timestamp ON users;
CREATE TRIGGER trg_update_users_timestamp
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION update_timestamp_column();
