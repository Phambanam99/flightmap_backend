-- =====================================================
-- TimescaleDB Optimization Configuration
-- =====================================================

-- Create hypertables for time-series data
SELECT create_hypertable('flight_tracking', 'last_seen', chunk_time_interval => INTERVAL '1 hour');
SELECT create_hypertable('ship_tracking', 'timestamp', chunk_time_interval => INTERVAL '1 hour');

-- =====================================================
-- COMPRESSION POLICIES
-- =====================================================

-- Enable compression for flight_tracking (after 1 day)
ALTER TABLE flight_tracking SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'hex_ident',
    timescaledb.compress_orderby = 'last_seen DESC'
);

SELECT add_compression_policy('flight_tracking', INTERVAL '1 day');

-- Enable compression for ship_tracking (after 1 day)  
ALTER TABLE ship_tracking SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'mmsi',
    timescaledb.compress_orderby = 'timestamp DESC'
);

SELECT add_compression_policy('ship_tracking', INTERVAL '1 day');

-- =====================================================
-- RETENTION POLICIES
-- =====================================================

-- Auto-delete flight data older than 7 days
SELECT add_retention_policy('flight_tracking', INTERVAL '7 days');

-- Auto-delete ship data older than 7 days
SELECT add_retention_policy('ship_tracking', INTERVAL '7 days');

-- =====================================================
-- CONTINUOUS AGGREGATES
-- =====================================================

-- Flight statistics by 5-minute intervals
CREATE MATERIALIZED VIEW flight_stats_5min
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('5 minutes', last_seen) AS time_bucket,
    hex_ident,
    AVG(altitude) as avg_altitude,
    MAX(altitude) as max_altitude,
    MIN(altitude) as min_altitude,
    AVG(ground_speed) as avg_speed,
    MAX(ground_speed) as max_speed,
    COUNT(*) as data_points,
    ST_Centroid(ST_Collect(ST_Point(longitude, latitude))) as center_point
FROM flight_tracking
GROUP BY time_bucket, hex_ident;

-- Add compression policy for flight stats
SELECT add_compression_policy('flight_stats_5min', INTERVAL '1 day');

-- Ship statistics by 10-minute intervals
CREATE MATERIALIZED VIEW ship_stats_10min
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('10 minutes', timestamp) AS time_bucket,
    mmsi,
    AVG(speed) as avg_speed,
    MAX(speed) as max_speed,
    AVG(course) as avg_course,
    COUNT(*) as data_points,
    ST_Centroid(ST_Collect(ST_Point(longitude, latitude))) as center_point
FROM ship_tracking
GROUP BY time_bucket, mmsi;

-- Add compression policy for ship stats
SELECT add_compression_policy('ship_stats_10min', INTERVAL '1 day');

-- =====================================================
-- OPTIMIZED INDEXES
-- =====================================================

-- Flight tracking indexes
CREATE INDEX IF NOT EXISTS idx_flight_tracking_hex_ident_time 
    ON flight_tracking (hex_ident, last_seen DESC);
    
CREATE INDEX IF NOT EXISTS idx_flight_tracking_location 
    ON flight_tracking USING GIST (ST_Point(longitude, latitude));

CREATE INDEX IF NOT EXISTS idx_flight_tracking_altitude 
    ON flight_tracking (altitude) WHERE altitude IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_flight_tracking_squawk 
    ON flight_tracking (squawk) WHERE squawk IN ('7500', '7600', '7700');

-- Ship tracking indexes
CREATE INDEX IF NOT EXISTS idx_ship_tracking_mmsi_time 
    ON ship_tracking (mmsi, timestamp DESC);
    
CREATE INDEX IF NOT EXISTS idx_ship_tracking_location 
    ON ship_tracking USING GIST (ST_Point(longitude, latitude));

CREATE INDEX IF NOT EXISTS idx_ship_tracking_speed 
    ON ship_tracking (speed) WHERE speed IS NOT NULL;

-- =====================================================
-- VIEWS FOR LATEST POSITIONS
-- =====================================================

-- Latest flight positions
CREATE OR REPLACE VIEW latest_flight_positions AS
SELECT DISTINCT ON (hex_ident) 
    hex_ident,
    latitude,
    longitude,
    altitude,
    ground_speed,
    track,
    last_seen,
    flight_id
FROM flight_tracking
ORDER BY hex_ident, last_seen DESC;

-- Latest ship positions
CREATE OR REPLACE VIEW latest_ship_positions AS
SELECT DISTINCT ON (mmsi)
    mmsi,
    latitude,
    longitude,
    speed,
    course,
    heading,
    nav_status,
    timestamp,
    voyage_id
FROM ship_tracking
ORDER BY mmsi, timestamp DESC;

-- =====================================================
-- UTILITY FUNCTIONS
-- =====================================================

-- Function to get flight trail
CREATE OR REPLACE FUNCTION get_flight_trail(
    p_hex_ident TEXT,
    p_hours INTEGER DEFAULT 24
) RETURNS TABLE (
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    altitude DOUBLE PRECISION,
    last_seen TIMESTAMP,
    ground_speed DOUBLE PRECISION
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        ft.latitude,
        ft.longitude,
        ft.altitude,
        ft.last_seen,
        ft.ground_speed
    FROM flight_tracking ft
    WHERE ft.hex_ident = p_hex_ident
        AND ft.last_seen >= NOW() - INTERVAL '1 hour' * p_hours
    ORDER BY ft.last_seen;
END;
$$ LANGUAGE plpgsql;

-- Function to get ship trail
CREATE OR REPLACE FUNCTION get_ship_trail(
    p_mmsi TEXT,
    p_hours INTEGER DEFAULT 24
) RETURNS TABLE (
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    speed DOUBLE PRECISION,
    course DOUBLE PRECISION,
    timestamp TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        st.latitude,
        st.longitude,
        st.speed,
        st.course,
        st.timestamp
    FROM ship_tracking st
    WHERE st.mmsi = p_mmsi
        AND st.timestamp >= NOW() - INTERVAL '1 hour' * p_hours
    ORDER BY st.timestamp;
END;
$$ LANGUAGE plpgsql;

-- Function to get vehicles in area
CREATE OR REPLACE FUNCTION get_vehicles_in_area(
    p_min_lat DOUBLE PRECISION,
    p_max_lat DOUBLE PRECISION,
    p_min_lon DOUBLE PRECISION,
    p_max_lon DOUBLE PRECISION,
    p_vehicle_type TEXT DEFAULT 'both'
) RETURNS TABLE (
    vehicle_id TEXT,
    vehicle_type TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    last_update TIMESTAMP
) AS $$
BEGIN
    IF p_vehicle_type IN ('flight', 'both') THEN
        RETURN QUERY
        SELECT 
            lfp.hex_ident as vehicle_id,
            'flight'::TEXT as vehicle_type,
            lfp.latitude,
            lfp.longitude,
            lfp.last_seen as last_update
        FROM latest_flight_positions lfp
        WHERE lfp.latitude BETWEEN p_min_lat AND p_max_lat
            AND lfp.longitude BETWEEN p_min_lon AND p_max_lon;
    END IF;
    
    IF p_vehicle_type IN ('ship', 'both') THEN
        RETURN QUERY
        SELECT 
            lsp.mmsi as vehicle_id,
            'ship'::TEXT as vehicle_type,
            lsp.latitude,
            lsp.longitude,
            lsp.timestamp as last_update
        FROM latest_ship_positions lsp
        WHERE lsp.latitude BETWEEN p_min_lat AND p_max_lat
            AND lsp.longitude BETWEEN p_min_lon AND p_max_lon;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- PERFORMANCE MONITORING
-- =====================================================

-- View for performance monitoring
CREATE OR REPLACE VIEW performance_stats AS
SELECT 
    'flight_tracking' as table_name,
    pg_size_pretty(pg_total_relation_size('flight_tracking')) as total_size,
    (SELECT count(*) FROM flight_tracking) as total_rows,
    (SELECT count(*) FROM flight_tracking WHERE last_seen >= NOW() - INTERVAL '1 hour') as recent_rows
UNION ALL
SELECT 
    'ship_tracking' as table_name,
    pg_size_pretty(pg_total_relation_size('ship_tracking')) as total_size,
    (SELECT count(*) FROM ship_tracking) as total_rows,
    (SELECT count(*) FROM ship_tracking WHERE timestamp >= NOW() - INTERVAL '1 hour') as recent_rows;

-- =====================================================
-- SAMPLE QUERIES FOR TESTING
-- =====================================================

-- Test flight trail
-- SELECT * FROM get_flight_trail('ABC123', 6);

-- Test ship trail  
-- SELECT * FROM get_ship_trail('123456789', 12);

-- Test vehicles in area
-- SELECT * FROM get_vehicles_in_area(40.0, 41.0, -74.0, -73.0, 'both');

-- Performance monitoring
-- SELECT * FROM performance_stats;

-- Latest positions
-- SELECT * FROM latest_flight_positions LIMIT 10;
-- SELECT * FROM latest_ship_positions LIMIT 10;

COMMIT; 