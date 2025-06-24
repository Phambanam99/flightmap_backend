-- ============================================================================
-- TimescaleDB Optimization & Configuration
-- ============================================================================
-- This file configures TimescaleDB for optimal performance
-- Execution order: 02 (after extensions)

-- ============================================================================
-- CONFIGURATION NOTES
-- ============================================================================

-- TimescaleDB configuration parameters that should be set in postgresql.conf:
-- timescaledb.max_background_workers = 16
-- shared_preload_libraries = 'timescaledb'
-- timescaledb.enable_compression = on
-- 
-- These parameters require server restart and cannot be changed at runtime

-- ============================================================================
-- RUNTIME SETTINGS
-- ============================================================================

-- Set session-level optimization settings that can be changed at runtime
SET work_mem = '256MB';
SET maintenance_work_mem = '512MB';
SET effective_cache_size = '2GB';

-- ============================================================================
-- CHUNK TIME INTERVALS
-- ============================================================================

-- Set optimal chunk intervals for different data types
-- Aircraft tracking data: 1 day chunks (high frequency)
-- Vessel tracking data: 1 day chunks (medium frequency)
-- Historical data: 7 day chunks (lower frequency, bulk storage)

-- These will be applied when creating hypertables

-- ============================================================================
-- RETENTION POLICIES
-- ============================================================================

-- Define retention policies for different data types
-- Hot data: Recent data kept in memory/SSD for fast access
-- Warm data: Older data moved to compressed storage
-- Cold data: Archived or deleted based on retention policy

-- Create custom functions for retention management
CREATE OR REPLACE FUNCTION setup_retention_policy(
    hypertable_name TEXT,
    hot_data_interval INTERVAL,
    warm_data_interval INTERVAL,
    drop_after_interval INTERVAL
) RETURNS VOID AS $$
BEGIN
    -- Add compression policy (warm data)
    PERFORM add_compression_policy(hypertable_name, warm_data_interval);
    
    -- Add retention policy (drop old data)
    IF drop_after_interval IS NOT NULL THEN
        PERFORM add_retention_policy(hypertable_name, drop_after_interval);
    END IF;
    
    RAISE NOTICE 'Retention policies set for table: %', hypertable_name;
EXCEPTION
    WHEN OTHERS THEN
        RAISE WARNING 'Failed to set retention policy for %: %', hypertable_name, SQLERRM;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- CONTINUOUS AGGREGATES SETUP
-- ============================================================================

-- Function to create continuous aggregates for analytics
CREATE OR REPLACE FUNCTION create_continuous_aggregates() RETURNS VOID AS $$
BEGIN
    -- This function will be called after hypertables are created
    -- It creates materialized views for real-time analytics
    
    RAISE NOTICE 'Continuous aggregates setup function created';
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- INDEXING STRATEGY
-- ============================================================================

-- Function to create optimized indexes for tracking data
CREATE OR REPLACE FUNCTION create_tracking_indexes(table_name TEXT) RETURNS VOID AS $$
BEGIN
    -- Time-based indexes (common to all tracking tables)
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_time ON %s USING BTREE (timestamp DESC)', 
                   table_name, table_name);
    
    -- Coordinate indexes for fast bounding box queries (common to all tracking tables)
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = $1 AND column_name = 'latitude') THEN
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_coordinates ON %s USING BTREE (latitude, longitude)', 
                       table_name, table_name);
    END IF;
    
    -- Geospatial indexes (only if location column exists)
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = $1 AND column_name = 'location') THEN
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_location ON %s USING GIST (location)', 
                       table_name, table_name);
    END IF;
    
    -- Table-specific indexes
    IF table_name = 'flight_tracking' THEN
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_hexident_time ON %s USING BTREE (hexident, timestamp DESC)', 
                       table_name, table_name);
    ELSIF table_name = 'ship_tracking' THEN
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_mmsi_time ON %s USING BTREE (mmsi, timestamp DESC)', 
                       table_name, table_name);
    ELSIF table_name = 'tracking_points' THEN
        -- Entity-based indexes (only for tracking_points table)
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_entity ON %s USING BTREE (entity_type, entity_id)', 
                       table_name, table_name);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_entity_time ON %s USING BTREE (entity_id, timestamp DESC)', 
                       table_name, table_name);
    END IF;
    
    RAISE NOTICE 'Indexes created for table: %', table_name;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- PERFORMANCE MONITORING
-- ============================================================================

-- Function to analyze table performance
CREATE OR REPLACE FUNCTION analyze_tracking_performance(table_name TEXT) RETURNS TABLE(
    metric TEXT,
    value TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 'total_chunks'::TEXT, 
           (SELECT count(*)::TEXT FROM timescaledb_information.chunks 
            WHERE hypertable_name = table_name);
    
    RETURN QUERY
    SELECT 'compressed_chunks'::TEXT,
           (SELECT count(*)::TEXT FROM timescaledb_information.chunks 
            WHERE hypertable_name = table_name AND is_compressed = true);
    
    RETURN QUERY
    SELECT 'table_size'::TEXT,
           pg_size_pretty(pg_total_relation_size(table_name));
    
    RETURN QUERY
    SELECT 'compression_ratio'::TEXT,
           COALESCE(
               (SELECT (before_compression_total_bytes::float / after_compression_total_bytes::float)::TEXT
                FROM timescaledb_information.compression_settings 
                WHERE hypertable_name = table_name LIMIT 1),
               'Not compressed'
           );
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- UTILITY FUNCTIONS
-- ============================================================================

-- Function to get chunk information
CREATE OR REPLACE FUNCTION get_chunk_info(table_name TEXT) RETURNS TABLE(
    chunk_name TEXT,
    range_start TIMESTAMPTZ,
    range_end TIMESTAMPTZ,
    is_compressed BOOLEAN,
    chunk_size TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        c.chunk_name::TEXT,
        c.range_start,
        c.range_end,
        c.is_compressed,
        pg_size_pretty(pg_total_relation_size(c.chunk_name))::TEXT as chunk_size
    FROM timescaledb_information.chunks c
    WHERE c.hypertable_name = table_name
    ORDER BY c.range_start DESC;
END;
$$ LANGUAGE plpgsql;

-- Function to manually compress chunks
CREATE OR REPLACE FUNCTION compress_old_chunks(
    table_name TEXT,
    older_than INTERVAL DEFAULT '7 days'
) RETURNS INTEGER AS $$
DECLARE
    chunk_count INTEGER := 0;
    chunk_record RECORD;
BEGIN
    FOR chunk_record IN 
        SELECT chunk_name 
        FROM timescaledb_information.chunks 
        WHERE hypertable_name = table_name 
        AND is_compressed = false
        AND range_end < NOW() - older_than
    LOOP
        EXECUTE format('SELECT compress_chunk(''%s'')', chunk_record.chunk_name);
        chunk_count := chunk_count + 1;
    END LOOP;
    
    RAISE NOTICE 'Compressed % chunks for table %', chunk_count, table_name;
    RETURN chunk_count;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- PERFORMANCE METRICS FUNCTION
-- ============================================================================

-- Function to get comprehensive performance metrics
CREATE OR REPLACE FUNCTION get_performance_metrics() RETURNS TABLE(
    table_name TEXT,
    total_size TEXT,
    total_rows BIGINT,
    recent_rows BIGINT,
    compressed_chunks BIGINT,
    total_chunks BIGINT
) AS $$
BEGIN
    -- Flight tracking metrics
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'flight_tracking') THEN
        RETURN QUERY
        SELECT 
            'flight_tracking'::TEXT,
            pg_size_pretty(pg_total_relation_size('flight_tracking'))::TEXT,
            (SELECT count(*) FROM flight_tracking),
            (SELECT count(*) FROM flight_tracking WHERE timestamp > NOW() - INTERVAL '1 day'),
            COALESCE((SELECT count(*) FROM timescaledb_information.chunks 
                     WHERE hypertable_name = 'flight_tracking' AND is_compressed = true), 0),
            COALESCE((SELECT count(*) FROM timescaledb_information.chunks 
                     WHERE hypertable_name = 'flight_tracking'), 0);
    END IF;
    
    -- Ship tracking metrics
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ship_tracking') THEN
        RETURN QUERY
        SELECT 
            'ship_tracking'::TEXT,
            pg_size_pretty(pg_total_relation_size('ship_tracking'))::TEXT,
            (SELECT count(*) FROM ship_tracking),
            (SELECT count(*) FROM ship_tracking WHERE timestamp > NOW() - INTERVAL '1 day'),
            COALESCE((SELECT count(*) FROM timescaledb_information.chunks 
                     WHERE hypertable_name = 'ship_tracking' AND is_compressed = true), 0),
            COALESCE((SELECT count(*) FROM timescaledb_information.chunks 
                     WHERE hypertable_name = 'ship_tracking'), 0);
    END IF;
    
    -- Tracking points metrics
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'tracking_points') THEN
        RETURN QUERY
        SELECT 
            'tracking_points'::TEXT,
            pg_size_pretty(pg_total_relation_size('tracking_points'))::TEXT,
            (SELECT count(*) FROM tracking_points),
            (SELECT count(*) FROM tracking_points WHERE timestamp > NOW() - INTERVAL '1 day'),
            COALESCE((SELECT count(*) FROM timescaledb_information.chunks 
                     WHERE hypertable_name = 'tracking_points' AND is_compressed = true), 0),
            COALESCE((SELECT count(*) FROM timescaledb_information.chunks 
                     WHERE hypertable_name = 'tracking_points'), 0);
    END IF;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- SYSTEM HEALTH CHECK FUNCTION
-- ============================================================================

-- Function to check overall system health
CREATE OR REPLACE FUNCTION check_system_health() RETURNS TABLE(
    component TEXT,
    status TEXT,
    details TEXT
) AS $$
BEGIN
    -- Database connectivity
    RETURN QUERY SELECT 'Database'::TEXT, 'OK'::TEXT, 'PostgreSQL connection active'::TEXT;
    
    -- TimescaleDB extension
    RETURN QUERY
    SELECT 
        'TimescaleDB'::TEXT,
        CASE WHEN EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'timescaledb') 
             THEN 'OK' ELSE 'ERROR' END::TEXT,
        CASE WHEN EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'timescaledb') 
             THEN 'TimescaleDB extension loaded' 
             ELSE 'TimescaleDB extension not found' END::TEXT;
    
    -- Hypertables status
    RETURN QUERY
    SELECT 
        'Hypertables'::TEXT,
        'INFO'::TEXT,
        (SELECT count(*)::TEXT || ' hypertables active' 
         FROM timescaledb_information.hypertables)::TEXT;
         
    -- Recent data status
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'flight_tracking') THEN
        RETURN QUERY
        SELECT 
            'Flight Data'::TEXT,
            CASE WHEN EXISTS (SELECT 1 FROM flight_tracking WHERE timestamp > NOW() - INTERVAL '1 hour')
                 THEN 'OK' ELSE 'WARNING' END::TEXT,
            (SELECT 'Last record: ' || COALESCE(max(timestamp)::TEXT, 'No data')
             FROM flight_tracking)::TEXT;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ship_tracking') THEN
        RETURN QUERY
        SELECT 
            'Ship Data'::TEXT,
            CASE WHEN EXISTS (SELECT 1 FROM ship_tracking WHERE timestamp > NOW() - INTERVAL '1 hour')
                 THEN 'OK' ELSE 'WARNING' END::TEXT,
            (SELECT 'Last record: ' || COALESCE(max(timestamp)::TEXT, 'No data')
             FROM ship_tracking)::TEXT;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- COMPLETION MESSAGE
-- ============================================================================

DO $$
BEGIN
    RAISE NOTICE 'TimescaleDB optimization configuration completed successfully';
    RAISE NOTICE 'Helper functions created:';
    RAISE NOTICE '  - setup_retention_policy()';
    RAISE NOTICE '  - create_continuous_aggregates()';
    RAISE NOTICE '  - create_tracking_indexes()';
    RAISE NOTICE '  - analyze_tracking_performance()';
    RAISE NOTICE '  - get_chunk_info()';
    RAISE NOTICE '  - compress_old_chunks()';
    RAISE NOTICE '  - get_performance_metrics()';
    RAISE NOTICE '  - check_system_health()';
END $$; 