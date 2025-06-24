-- Check database tables and vessel data
-- Connect to: psql -U postgres -d ship_tracking_db -f check_db.sql

-- 1. Check if tables exist
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
  AND table_name LIKE '%ship%' OR table_name LIKE '%vessel%'
ORDER BY table_name;

-- 2. Check Ship table count
SELECT 'ship' as table_name, COUNT(*) as record_count 
FROM ship;

-- 3. Check ShipTracking table count  
SELECT 'ship_tracking' as table_name, COUNT(*) as record_count 
FROM ship_tracking;

-- 4. Check RawVesselData table count
SELECT 'raw_vessel_data' as table_name, COUNT(*) as record_count 
FROM raw_vessel_data;

-- 5. Check recent Ship records (last 10)
SELECT id, mmsi, name, ship_type, last_seen, created_at
FROM ship 
ORDER BY created_at DESC 
LIMIT 10;

-- 6. Check recent ShipTracking records (last 10)
SELECT id, mmsi, latitude, longitude, speed, course, timestamp, update_time
FROM ship_tracking 
ORDER BY timestamp DESC 
LIMIT 10;

-- 7. Check recent RawVesselData records (last 5)
SELECT id, data_source, mmsi, vessel_name, latitude, longitude, received_at
FROM raw_vessel_data 
ORDER BY received_at DESC 
LIMIT 5;

-- 8. Check tracking data for last hour
SELECT COUNT(*) as recent_tracking_count
FROM ship_tracking 
WHERE timestamp > NOW() - INTERVAL '1 hour';

-- 9. Check data sources in raw data
SELECT data_source, COUNT(*) as count, MAX(received_at) as latest_data
FROM raw_vessel_data 
GROUP BY data_source 
ORDER BY latest_data DESC;

-- 10. Check database connection and current time
SELECT NOW() as current_time, version() as db_version; 