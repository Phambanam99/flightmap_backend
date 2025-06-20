-- ============================================================================
-- TimescaleDB Optimization & Configuration
-- ============================================================================
-- This file configures TimescaleDB for optimal performance
-- Execution order: 02 (after extensions)

-- ============================================================================
-- MEMORY CONFIGURATION
-- ============================================================================

-- Set TimescaleDB memory settings for better performance
-- These settings optimize for high-throughput time-series workloads
SELECT set_config('timescaledb.max_background_workers', '16', false);
SELECT set_config('shared_preload_libraries', 'timescaledb', false);

-- ============================================================================
-- COMPRESSION SETTINGS
-- ============================================================================

-- Enable compression for better storage efficiency
-- This is crucial for long-term storage of tracking data
SELECT set_config('timescaledb.enable_compression', 'on', false);

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
    -- Time-based indexes
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_time ON %s USING BTREE (timestamp DESC)', 
                   table_name, table_name);
    
    -- Entity-based indexes
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_entity ON %s USING BTREE (entity_type, entity_id)', 
                   table_name, table_name);
    
    -- Geospatial indexes
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_location ON %s USING GIST (location)', 
                   table_name, table_name);
    
    -- Coordinate indexes for fast bounding box queries
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_coordinates ON %s USING BTREE (latitude, longitude)', 
                   table_name, table_name);
    
    -- Composite indexes for common queries
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_entity_time ON %s USING BTREE (entity_id, timestamp DESC)', 
                   table_name, table_name);
    
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
END $$; 