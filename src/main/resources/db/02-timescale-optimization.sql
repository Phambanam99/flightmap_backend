-- =====================================================
-- TimescaleDB Optimization Configuration
-- =====================================================
-- Note: This script should be run AFTER Spring Boot application creates tables
-- Tables will be created by JPA/Hibernate when application starts

-- Function to safely create hypertable only if table exists
CREATE OR REPLACE FUNCTION safe_create_hypertable(
    table_name TEXT, 
    time_column TEXT, 
    chunk_interval INTERVAL
) RETURNS VOID AS $$
BEGIN
    -- Check if table exists before creating hypertable
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = $1) THEN
        -- Check if it's not already a hypertable
        IF NOT EXISTS (SELECT 1 FROM timescaledb_information.hypertables WHERE hypertable_name = $1) THEN
            PERFORM create_hypertable($1, $2, chunk_time_interval => $3);
            RAISE NOTICE 'Created hypertable for %', $1;
        ELSE
            RAISE NOTICE 'Table % is already a hypertable', $1;
        END IF;
    ELSE
        RAISE NOTICE 'Table % does not exist, skipping hypertable creation', $1;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Create hypertables for time-series data (only if tables exist)
SELECT safe_create_hypertable('flight_tracking', 'last_seen', INTERVAL '1 hour');
SELECT safe_create_hypertable('ship_tracking', 'timestamp', INTERVAL '1 hour');

-- =====================================================
-- COMPRESSION POLICIES (Applied conditionally)
-- =====================================================

-- Function to safely add compression policy
CREATE OR REPLACE FUNCTION safe_add_compression_policy(
    table_name TEXT,
    compress_after INTERVAL
) RETURNS VOID AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM timescaledb_information.hypertables WHERE hypertable_name = $1) THEN
        -- Enable compression settings
        EXECUTE format('ALTER TABLE %I SET (
            timescaledb.compress,
            timescaledb.compress_segmentby = %L,
            timescaledb.compress_orderby = %L
        )', $1, 
        CASE 
            WHEN $1 = 'flight_tracking' THEN 'hex_ident'
            WHEN $1 = 'ship_tracking' THEN 'mmsi'
            ELSE 'id'
        END,
        CASE 
            WHEN $1 = 'flight_tracking' THEN 'last_seen DESC'
            WHEN $1 = 'ship_tracking' THEN 'timestamp DESC'
            ELSE 'created_at DESC'
        END);
        
        -- Add compression policy
        PERFORM add_compression_policy($1, $2);
        RAISE NOTICE 'Added compression policy for %', $1;
    ELSE
        RAISE NOTICE 'Hypertable % does not exist, skipping compression policy', $1;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Apply compression policies
SELECT safe_add_compression_policy('flight_tracking', INTERVAL '1 day');
SELECT safe_add_compression_policy('ship_tracking', INTERVAL '1 day');

-- =====================================================
-- RETENTION POLICIES (Applied conditionally)
-- =====================================================

-- Function to safely add retention policy
CREATE OR REPLACE FUNCTION safe_add_retention_policy(
    table_name TEXT,
    retention_period INTERVAL
) RETURNS VOID AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM timescaledb_information.hypertables WHERE hypertable_name = $1) THEN
        PERFORM add_retention_policy($1, $2);
        RAISE NOTICE 'Added retention policy for % (% days)', $1, extract(days from $2);
    ELSE
        RAISE NOTICE 'Hypertable % does not exist, skipping retention policy', $1;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Auto-delete data older than 7 days
SELECT safe_add_retention_policy('flight_tracking', INTERVAL '7 days');
SELECT safe_add_retention_policy('ship_tracking', INTERVAL '7 days');

-- =====================================================
-- CONTINUOUS AGGREGATES (Created conditionally)
-- =====================================================

-- Function to safely create continuous aggregate
CREATE OR REPLACE FUNCTION safe_create_continuous_aggregate(
    view_name TEXT,
    source_table TEXT,
    time_column TEXT,
    bucket_interval TEXT
) RETURNS VOID AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM timescaledb_information.hypertables WHERE hypertable_name = $2) THEN
        -- Drop view if exists
        EXECUTE format('DROP MATERIALIZED VIEW IF EXISTS %I', $1);
        
        -- Create continuous aggregate based on table type
        IF $2 = 'flight_tracking' THEN
            EXECUTE format('
                CREATE MATERIALIZED VIEW %I
                WITH (timescaledb.continuous) AS
                SELECT 
                    time_bucket(%L, %I) AS time_bucket,
                    hex_ident,
                    AVG(altitude) as avg_altitude,
                    MAX(altitude) as max_altitude,
                    MIN(altitude) as min_altitude,
                    AVG(ground_speed) as avg_speed,
                    MAX(ground_speed) as max_speed,
                    COUNT(*) as data_points
                FROM %I
                GROUP BY time_bucket, hex_ident
            ', $1, $4, $3, $2);
        ELSIF $2 = 'ship_tracking' THEN
            EXECUTE format('
                CREATE MATERIALIZED VIEW %I
                WITH (timescaledb.continuous) AS
                SELECT 
                    time_bucket(%L, %I) AS time_bucket,
                    mmsi,
                    AVG(speed) as avg_speed,
                    MAX(speed) as max_speed,
                    AVG(course) as avg_course,
                    COUNT(*) as data_points
                FROM %I
                GROUP BY time_bucket, mmsi
            ', $1, $4, $3, $2);
        END IF;
        
        RAISE NOTICE 'Created continuous aggregate %', $1;
    ELSE
        RAISE NOTICE 'Source table % does not exist, skipping continuous aggregate %', $2, $1;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Create continuous aggregates
SELECT safe_create_continuous_aggregate('flight_stats_5min', 'flight_tracking', 'last_seen', '5 minutes');
SELECT safe_create_continuous_aggregate('ship_stats_10min', 'ship_tracking', 'timestamp', '10 minutes');

-- Add compression policies for continuous aggregates
SELECT safe_add_compression_policy('flight_stats_5min', INTERVAL '1 day');
SELECT safe_add_compression_policy('ship_stats_10min', INTERVAL '1 day');

-- =====================================================
-- OPTIMIZED INDEXES (Applied conditionally)
-- =====================================================

-- Function to safely create indexes
CREATE OR REPLACE FUNCTION safe_create_indexes() RETURNS VOID AS $$
BEGIN
    -- Flight tracking indexes
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'flight_tracking') THEN
        CREATE INDEX IF NOT EXISTS idx_flight_tracking_hex_ident_time 
            ON flight_tracking (hex_ident, last_seen DESC);
        
        -- Only create spatial index if PostGIS is available
        IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'postgis') THEN
            CREATE INDEX IF NOT EXISTS idx_flight_tracking_location 
                ON flight_tracking USING GIST (ST_Point(longitude, latitude));
        END IF;
        
        CREATE INDEX IF NOT EXISTS idx_flight_tracking_altitude 
            ON flight_tracking (altitude) WHERE altitude IS NOT NULL;
            
        CREATE INDEX IF NOT EXISTS idx_flight_tracking_squawk 
            ON flight_tracking (squawk) WHERE squawk IN ('7500', '7600', '7700');
            
        RAISE NOTICE 'Created indexes for flight_tracking';
    END IF;
    
    -- Ship tracking indexes  
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ship_tracking') THEN
        CREATE INDEX IF NOT EXISTS idx_ship_tracking_mmsi_time 
            ON ship_tracking (mmsi, timestamp DESC);
            
        -- Only create spatial index if PostGIS is available
        IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'postgis') THEN
            CREATE INDEX IF NOT EXISTS idx_ship_tracking_location 
                ON ship_tracking USING GIST (ST_Point(longitude, latitude));
        END IF;
        
        CREATE INDEX IF NOT EXISTS idx_ship_tracking_speed 
            ON ship_tracking (speed) WHERE speed IS NOT NULL;
            
        RAISE NOTICE 'Created indexes for ship_tracking';
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Create indexes
SELECT safe_create_indexes();

-- =====================================================
-- VIEWS FOR LATEST POSITIONS (Created conditionally)
-- =====================================================

-- Function to safely create views
CREATE OR REPLACE FUNCTION safe_create_views() RETURNS VOID AS $$
BEGIN
    -- Latest flight positions
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'flight_tracking') THEN
        DROP VIEW IF EXISTS latest_flight_positions;
        CREATE VIEW latest_flight_positions AS
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
        
        RAISE NOTICE 'Created view latest_flight_positions';
    END IF;
    
    -- Latest ship positions
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ship_tracking') THEN
        DROP VIEW IF EXISTS latest_ship_positions;
        CREATE VIEW latest_ship_positions AS
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
        
        RAISE NOTICE 'Created view latest_ship_positions';
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Create views
SELECT safe_create_views();

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
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'flight_tracking') THEN
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
    ELSE
        RAISE NOTICE 'Table flight_tracking does not exist';
    END IF;
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
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ship_tracking') THEN
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
    ELSE
        RAISE NOTICE 'Table ship_tracking does not exist';
    END IF;
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
    IF p_vehicle_type IN ('flight', 'both') AND 
       EXISTS (SELECT 1 FROM information_schema.views WHERE table_name = 'latest_flight_positions') THEN
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
    
    IF p_vehicle_type IN ('ship', 'both') AND
       EXISTS (SELECT 1 FROM information_schema.views WHERE table_name = 'latest_ship_positions') THEN
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
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'flight_tracking') 
        THEN pg_size_pretty(pg_total_relation_size('flight_tracking'))
        ELSE 'Table not exists'
    END as total_size,
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'flight_tracking')
        THEN (SELECT count(*) FROM flight_tracking)::TEXT
        ELSE '0'
    END as total_rows,
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'flight_tracking')
        THEN (SELECT count(*) FROM flight_tracking WHERE last_seen >= NOW() - INTERVAL '1 hour')::TEXT
        ELSE '0'
    END as recent_rows
UNION ALL
SELECT 
    'ship_tracking' as table_name,
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ship_tracking')
        THEN pg_size_pretty(pg_total_relation_size('ship_tracking'))
        ELSE 'Table not exists'
    END as total_size,
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ship_tracking')
        THEN (SELECT count(*) FROM ship_tracking)::TEXT
        ELSE '0'
    END as total_rows,
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ship_tracking')
        THEN (SELECT count(*) FROM ship_tracking WHERE timestamp >= NOW() - INTERVAL '1 hour')::TEXT
        ELSE '0'
    END as recent_rows;

-- =====================================================
-- INITIALIZATION COMPLETE
-- =====================================================

-- Log completion
DO $$
BEGIN
    RAISE NOTICE '=== TimescaleDB Optimization Setup Complete ===';
    RAISE NOTICE 'Tables will be optimized when Spring Boot application creates them';
    RAISE NOTICE 'Functions created: safe_create_hypertable, safe_add_compression_policy, etc.';
    RAISE NOTICE 'Views created: performance_stats';
    RAISE NOTICE 'Utility functions: get_flight_trail, get_ship_trail, get_vehicles_in_area';
END $$;

COMMIT; 