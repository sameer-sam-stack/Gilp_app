-- ==============================================================================
-- GLIP MOBILITY: SCHOOL VEHICLE DEDICATED SUPABASE DATABASE SCHEMA
-- Database Project: https://uugzolriwmbhssgfjbap.supabase.co
-- Run this script in your Supabase SQL Editor (https://supabase.com/dashboard)
-- ==============================================================================

-- 1. Create the school_bookings table with 6-digit request_code and complete driver assignment fields
CREATE TABLE IF NOT EXISTS public.school_bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_code VARCHAR(10) NOT NULL,
    user_id TEXT NOT NULL,
    parent_name TEXT NOT NULL,
    parent_phone TEXT NOT NULL,
    student_name TEXT NOT NULL,
    student_class VARCHAR(50) NOT NULL,
    pickup_location TEXT NOT NULL,
    pickup_lat DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    pickup_lng DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    dropoff_location TEXT NOT NULL,
    dropoff_lat DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    dropoff_lng DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    pickup_time VARCHAR(30) NOT NULL,
    drop_time VARCHAR(30) NOT NULL,
    fare_estimate NUMERIC(10, 2) DEFAULT 80.00,
    status VARCHAR(50) DEFAULT 'Requested' CHECK (status IN ('Requested', 'Driver Assigned', 'In Progress', 'Completed', 'Cancelled')),
    driver_id TEXT,
    driver_name TEXT,
    driver_phone TEXT,
    vehicle_number TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- In case table already exists without request_code, add it safely
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' AND table_name = 'school_bookings' AND column_name = 'request_code'
    ) THEN
        ALTER TABLE public.school_bookings ADD COLUMN request_code VARCHAR(10) DEFAULT LPAD(FLOOR(RANDOM() * 900000 + 100000)::TEXT, 6, '0');
    END IF;
END $$;

-- 2. Fast querying indexes for Parent history, Phone lookup, and Driver assignment
CREATE INDEX IF NOT EXISTS idx_school_bookings_request_code ON public.school_bookings(request_code);
CREATE INDEX IF NOT EXISTS idx_school_bookings_parent_phone ON public.school_bookings(parent_phone);
CREATE INDEX IF NOT EXISTS idx_school_bookings_user_id ON public.school_bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_school_bookings_status ON public.school_bookings(status);
CREATE INDEX IF NOT EXISTS idx_school_bookings_created_at ON public.school_bookings(created_at DESC);

-- 3. Row Level Security (RLS) Configuration
ALTER TABLE public.school_bookings ENABLE ROW LEVEL SECURITY;

-- Allow public / anon client to insert school ride bookings
DROP POLICY IF EXISTS "Allow public insert on school_bookings" ON public.school_bookings;
CREATE POLICY "Allow public insert on school_bookings" 
ON public.school_bookings FOR INSERT 
WITH CHECK (true);

-- Allow public / anon client to select bookings (filtered by user_id/parent_phone)
DROP POLICY IF EXISTS "Allow public select on school_bookings" ON public.school_bookings;
CREATE POLICY "Allow public select on school_bookings" 
ON public.school_bookings FOR SELECT 
USING (true);

-- Allow public / anon client to update bookings (status cancellation or driver assignment)
DROP POLICY IF EXISTS "Allow public update on school_bookings" ON public.school_bookings;
CREATE POLICY "Allow public update on school_bookings" 
ON public.school_bookings FOR UPDATE 
USING (true);

-- 4. Enable Supabase Realtime for instant Driver & Parent sync
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' AND tablename = 'school_bookings'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.school_bookings;
    END IF;
END $$;

-- 5. Auto-update updated_at timestamp trigger
CREATE OR REPLACE FUNCTION update_school_bookings_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_update_school_bookings_timestamp ON public.school_bookings;
CREATE TRIGGER trg_update_school_bookings_timestamp
BEFORE UPDATE ON public.school_bookings
FOR EACH ROW
EXECUTE FUNCTION update_school_bookings_timestamp();
