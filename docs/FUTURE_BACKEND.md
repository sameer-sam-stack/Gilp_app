# Supabase Future Backend Architecture

## 1. Overview
The Glip app is designed with domain repository interfaces (`UserRepository`, `MobilityRepository`) to allow seamless transition to Supabase once live backend services are deployed.

## 2. Recommended Database Schema

### `profiles`
```sql
create table public.profiles (
  id uuid references auth.users not null primary key,
  full_name text not null,
  phone_number text not null unique,
  email text,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null
);
```

### `vehicles`
```sql
create table public.vehicles (
  id uuid default gen_random_uuid() primary key,
  driver_id uuid references public.profiles(id) not null,
  license_plate text not null,
  category text not null,
  model_name text not null,
  total_seats int not null default 4,
  occupied_seats int not null default 0,
  current_lat double precision not null,
  current_lng double precision not null,
  is_active boolean default true,
  updated_at timestamp with time zone default timezone('utc'::text, now()) not null
);
```

### `routes`
```sql
create table public.routes (
  id uuid default gen_random_uuid() primary key,
  vehicle_id uuid references public.vehicles(id) not null,
  origin_name text not null,
  destination_name text not null,
  origin_lat double precision not null,
  origin_lng double precision not null,
  destination_lat double precision not null,
  destination_lng double precision not null,
  polyline jsonb not null,
  status text not null default 'EN_ROUTE',
  created_at timestamp with time zone default timezone('utc'::text, now()) not null
);
```

### `local_promotions`
```sql
create table public.local_promotions (
  id uuid default gen_random_uuid() primary key,
  title text not null,
  business_name text not null,
  category text not null,
  discount_description text not null,
  distance_description text not null,
  valid_until text not null,
  terms text,
  is_active boolean default true
);
```

## 3. Security & Access Control
- **Row Level Security (RLS)**: Enable RLS on all tables.
- **Anon Key Only in Client**: Never include the Supabase `service_role` key in the mobile application.
- **Driver Location Access**: Only expose active vehicle coordinates to passengers searching along that route.
- **Phone Verification**: Use Supabase Phone Auth via Twilio or MSG91 when moving from local demo OTP to production.
