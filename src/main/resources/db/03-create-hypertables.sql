-- ============================================================================
-- TimescaleDB Hypertables Creation
-- ============================================================================
-- This script creates hypertables for time-series tracking data
-- Execution order: 03 (after JPA table creation)

-- ============================================================================
-- HELPER FUNCTIONS
-- ============================================================================

-- Function to safely create hypertable only if table exists and is not already a hypertable
CREATE OR REPLACE FUNCTION safe_create_hypertable(
    table_name TEXT, 
    time_column TEXT, 
    chunk_interval INTERVAL,
    compress_segmentby TEXT DEFAULT NULL,
    compress_orderby TEXT DEFAULT NULL
) RETURNS VOID AS $$
BEGIN
    -- Check if table exists before creating hypertable
    IF EXISTS (SELECT 1 FROM information_schema.tables 
               WHERE table_schema = 'public' AND information_schema.tables.table_name = $1) THEN
        
        -- Check if it's not already a hypertable
        IF NOT EXISTS (SELECT 1 FROM timescaledb_information.hypertables 
                       WHERE hypertable_name = $1) THEN
            
            -- Create hypertable
            PERFORM create_hypertable($1, $2, chunk_time_interval => $3);
            
            -- Set up compression if parameters provided
            IF $4 IS NOT NULL AND $5 IS NOT NULL THEN
                EXECUTE format('ALTER TABLE %I SET (
                    timescaledb.compress,
                    timescaledb.compress_segmentby = %L,
                    timescaledb.compress_orderby = %L
                )', $1, $4, $5);
            ELSIF $5 IS NOT NULL THEN
                EXECUTE format('ALTER TABLE %I SET (
                    timescaledb.compress,
                    timescaledb.compress_orderby = %L
                )', $1, $5);
            ELSE
                EXECUTE format('ALTER TABLE %I SET (timescaledb.compress)', $1);
            END IF;
            
            RAISE NOTICE 'Created hypertable for % with chunk interval %', $1, $3;
        ELSE
            RAISE NOTICE 'Table % is already a hypertable', $1;
        END IF;
    ELSE
        RAISE NOTICE 'Table % does not exist, skipping hypertable creation', $1;
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        RAISE WARNING 'Failed to create hypertable for %: %', $1, SQLERRM;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- CREATE HYPERTABLES FOR TRACKING DATA
-- ============================================================================

-- Flight Tracking Hypertable (high frequency data)
SELECT safe_create_hypertable(
    'flight_tracking', 
    'timestamp', 
    INTERVAL '1 day',
    'hexident',
    'timestamp DESC'
);

-- Ship Tracking Hypertable (medium frequency data)
SELECT safe_create_hypertable(
    'ship_tracking', 
    'timestamp', 
    INTERVAL '1 day',
    'mmsi',
    'timestamp DESC'
);

-- Tracking Points Hypertable (unified time-series data)
SELECT safe_create_hypertable(
    'tracking_points', 
    'timestamp', 
    INTERVAL '1 day',
    'entity_type,entity_id',
    'timestamp DESC'
);

-- Alert Events Hypertable (alert history)
SELECT safe_create_hypertable(
    'alert_event', 
    'event_time', 
    INTERVAL '7 days',
    'entity_type',
    'event_time DESC'
);

-- Data Source Status Hypertable (monitoring data)
SELECT safe_create_hypertable(
    'data_source_status', 
    'check_time', 
    INTERVAL '1 hour',
    'data_source_id',
    'check_time DESC'
);

-- ============================================================================
-- CREATE INDEXES FOR OPTIMAL PERFORMANCE
-- ============================================================================

-- Flight Tracking Indexes
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'flight_tracking') THEN
        -- Check if function exists before calling it
        IF EXISTS (SELECT 1 FROM information_schema.routines WHERE routine_name = 'create_tracking_indexes') THEN
            PERFORM create_tracking_indexes('flight_tracking');
        END IF;
        
        -- Additional specialized indexes for flight data
        CREATE INDEX IF NOT EXISTS idx_flight_tracking_hexident_time 
            ON flight_tracking (hexident, timestamp DESC);
        
        CREATE INDEX IF NOT EXISTS idx_flight_tracking_altitude 
            ON flight_tracking (altitude) WHERE altitude IS NOT NULL;
        
        CREATE INDEX IF NOT EXISTS idx_flight_tracking_emergency 
            ON flight_tracking (emergency) WHERE emergency = true;
        
        CREATE INDEX IF NOT EXISTS idx_flight_tracking_squawk 
            ON flight_tracking (squawk) WHERE squawk IN ('7500', '7600', '7700');
            
        CREATE INDEX IF NOT EXISTS idx_flight_tracking_flight_phase 
            ON flight_tracking (flight_phase);
            
        RAISE NOTICE 'Flight tracking indexes created successfully';
    END IF;
END $$;

-- Ship Tracking Indexes  
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ship_tracking') THEN
        -- Check if function exists before calling it
        IF EXISTS (SELECT 1 FROM information_schema.routines WHERE routine_name = 'create_tracking_indexes') THEN
            PERFORM create_tracking_indexes('ship_tracking');
        END IF;
        
        -- Additional specialized indexes for ship data
        CREATE INDEX IF NOT EXISTS idx_ship_tracking_mmsi_time 
            ON ship_tracking (mmsi, timestamp DESC);
        
        CREATE INDEX IF NOT EXISTS idx_ship_tracking_navigation_status 
            ON ship_tracking (navigation_status);
        
        CREATE INDEX IF NOT EXISTS idx_ship_tracking_speed 
            ON ship_tracking (speed) WHERE speed IS NOT NULL;
        
        CREATE INDEX IF NOT EXISTS idx_ship_tracking_security_alert 
            ON ship_tracking (security_alert) WHERE security_alert = true;
            
        CREATE INDEX IF NOT EXISTS idx_ship_tracking_dangerous_cargo 
            ON ship_tracking (dangerous_cargo) WHERE dangerous_cargo = true;
            
        RAISE NOTICE 'Ship tracking indexes created successfully';
    END IF;
END $$;

-- Tracking Points Indexes
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'tracking_points') THEN
        -- Check if function exists before calling it
        IF EXISTS (SELECT 1 FROM information_schema.routines WHERE routine_name = 'create_tracking_indexes') THEN
            PERFORM create_tracking_indexes('tracking_points');
        END IF;
        
        -- Additional indexes for tracking points
        CREATE INDEX IF NOT EXISTS idx_tracking_points_data_quality 
            ON tracking_points (data_quality);
        
        CREATE INDEX IF NOT EXISTS idx_tracking_points_aggregation 
            ON tracking_points (aggregation_count, aggregation_window);
            
        RAISE NOTICE 'Tracking points indexes created successfully';
    END IF;
END $$;

-- ============================================================================
-- SETUP RETENTION AND COMPRESSION POLICIES
-- ============================================================================

-- Flight Tracking Policies
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM timescaledb_information.hypertables 
               WHERE hypertable_name = 'flight_tracking') THEN
        
        -- Compression after 1 day (warm storage)
        PERFORM add_compression_policy('flight_tracking', INTERVAL '1 day');
        
        -- Retention policy - delete data older than 30 days
        PERFORM add_retention_policy('flight_tracking', INTERVAL '30 days');
        
        RAISE NOTICE 'Flight tracking policies applied: compress after 1 day, delete after 30 days';
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        RAISE WARNING 'Failed to apply policies for flight_tracking: %', SQLERRM;
END $$;

-- Ship Tracking Policies
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM timescaledb_information.hypertables 
               WHERE hypertable_name = 'ship_tracking') THEN
        
        -- Compression after 1 day (warm storage)
        PERFORM add_compression_policy('ship_tracking', INTERVAL '1 day');
        
        -- Retention policy - delete data older than 30 days
        PERFORM add_retention_policy('ship_tracking', INTERVAL '30 days');
        
        RAISE NOTICE 'Ship tracking policies applied: compress after 1 day, delete after 30 days';
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        RAISE WARNING 'Failed to apply policies for ship_tracking: %', SQLERRM;
END $$;

-- Tracking Points Policies (longer retention for unified data)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM timescaledb_information.hypertables 
               WHERE hypertable_name = 'tracking_points') THEN
        
        -- Compression after 7 days (warm storage)
        PERFORM add_compression_policy('tracking_points', INTERVAL '7 days');
        
        -- Retention policy - delete data older than 90 days
        PERFORM add_retention_policy('tracking_points', INTERVAL '90 days');
        
        RAISE NOTICE 'Tracking points policies applied: compress after 7 days, delete after 90 days';
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        RAISE WARNING 'Failed to apply policies for tracking_points: %', SQLERRM;
END $$;

-- Alert Events Policies (keep alerts longer)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM timescaledb_information.hypertables 
               WHERE hypertable_name = 'alert_event') THEN
        
        -- Compression after 30 days
        PERFORM add_compression_policy('alert_event', INTERVAL '30 days');
        
        -- Retention policy - delete data older than 1 year
        PERFORM add_retention_policy('alert_event', INTERVAL '1 year');
        
        RAISE NOTICE 'Alert events policies applied: compress after 30 days, delete after 1 year';
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        RAISE WARNING 'Failed to apply policies for alert_event: %', SQLERRM;
END $$;

-- Data Source Status Policies (monitoring data)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM timescaledb_information.hypertables 
               WHERE hypertable_name = 'data_source_status') THEN
        
        -- Compression after 1 day
        PERFORM add_compression_policy('data_source_status', INTERVAL '1 day');
        
        -- Retention policy - delete data older than 30 days
        PERFORM add_retention_policy('data_source_status', INTERVAL '30 days');
        
        RAISE NOTICE 'Data source status policies applied: compress after 1 day, delete after 30 days';
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        RAISE WARNING 'Failed to apply policies for data_source_status: %', SQLERRM;
END $$;

-- ============================================================================
-- CREATE CONTINUOUS AGGREGATES FOR ANALYTICS
-- ============================================================================

-- Function to safely create continuous aggregate
CREATE OR REPLACE FUNCTION safe_create_continuous_aggregate(
    view_name TEXT,
    source_table TEXT,
    time_column TEXT,
    bucket_interval TEXT,
    aggregate_query TEXT
) RETURNS VOID AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM timescaledb_information.hypertables 
               WHERE hypertable_name = source_table) THEN
        
        -- Drop view if exists
        EXECUTE format('DROP MATERIALIZED VIEW IF EXISTS %I', view_name);
        
        -- Create continuous aggregate
        EXECUTE format('CREATE MATERIALIZED VIEW %I
                        WITH (timescaledb.continuous) AS %s',
                       view_name, aggregate_query);
        
        RAISE NOTICE 'Created continuous aggregate: %', view_name;
    ELSE
        RAISE NOTICE 'Source table % does not exist or is not a hypertable, skipping %', 
                     source_table, view_name;
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        RAISE WARNING 'Failed to create continuous aggregate %: %', view_name, SQLERRM;
END;
$$ LANGUAGE plpgsql;

-- Aircraft Statistics (5-minute buckets)
SELECT safe_create_continuous_aggregate(
    'aircraft_stats_5min',
    'flight_tracking',
    'timestamp',
    '5 minutes',
    'SELECT 
        time_bucket(''5 minutes'', timestamp) AS time_bucket,
        hexident,
        AVG(altitude) as avg_altitude,
        MAX(altitude) as max_altitude,
        MIN(altitude) as min_altitude,
        AVG(ground_speed) as avg_speed,
        MAX(ground_speed) as max_speed,
        COUNT(*) as data_points,
        LAST(latitude, timestamp) as last_latitude,
        LAST(longitude, timestamp) as last_longitude
    FROM flight_tracking
    GROUP BY time_bucket, hexident'
);

-- Vessel Statistics (10-minute buckets)
SELECT safe_create_continuous_aggregate(
    'vessel_stats_10min',
    'ship_tracking',
    'timestamp',
    '10 minutes',
    'SELECT 
        time_bucket(''10 minutes'', timestamp) AS time_bucket,
        mmsi,
        AVG(speed) as avg_speed,
        MAX(speed) as max_speed,
        AVG(course) as avg_course,
        COUNT(*) as data_points,
        LAST(latitude, timestamp) as last_latitude,
        LAST(longitude, timestamp) as last_longitude,
        LAST(navigation_status, timestamp) as last_nav_status
    FROM ship_tracking
    GROUP BY time_bucket, mmsi'
);

-- Global Traffic Statistics (hourly)
SELECT safe_create_continuous_aggregate(
    'traffic_stats_hourly',
    'tracking_points',
    'timestamp',
    '1 hour',
    'SELECT 
        time_bucket(''1 hour'', timestamp) AS time_bucket,
        entity_type,
        COUNT(DISTINCT entity_id) as unique_entities,
        COUNT(*) as total_data_points,
        AVG(data_quality) as avg_data_quality,
        AVG(speed) as avg_speed,
        MIN(latitude) as min_lat,
        MAX(latitude) as max_lat,
        MIN(longitude) as min_lon,
        MAX(longitude) as max_lon
    FROM tracking_points
    GROUP BY time_bucket, entity_type'
);

-- ============================================================================
-- CREATE UTILITY VIEWS
-- ============================================================================

-- Latest Positions View (for real-time display)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'tracking_points') THEN
        EXECUTE 'CREATE OR REPLACE VIEW latest_positions AS
        SELECT DISTINCT ON (entity_type, entity_id)
            entity_type,
            entity_id,
            entity_name,
            latitude,
            longitude,
            altitude,
            speed,
            heading,
            timestamp as last_update,
            data_quality
        FROM tracking_points
        ORDER BY entity_type, entity_id, timestamp DESC';
        
        RAISE NOTICE 'Created latest_positions view successfully';
    ELSE
        RAISE NOTICE 'Table tracking_points does not exist, skipping latest_positions view creation';
    END IF;
END $$;

-- Active Alerts View
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'alert_event') AND
       EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'alert_rule') THEN
        EXECUTE 'CREATE OR REPLACE VIEW active_alerts AS
        SELECT 
            ae.id,
            ae.entity_type,
            ae.entity_id,
            ae.entity_name,
            ae.priority,
            ae.alert_message,
            ae.event_time,
            ae.latitude,
            ae.longitude,
            ar.name as rule_name,
            ar.rule_type
        FROM alert_event ae
        JOIN alert_rule ar ON ae.alert_rule_id = ar.id
        WHERE ae.status = ''ACTIVE''
        ORDER BY ae.event_time DESC';
        
        RAISE NOTICE 'Created active_alerts view successfully';
    ELSE
        RAISE NOTICE 'Tables alert_event or alert_rule do not exist, skipping active_alerts view creation';
    END IF;
END $$;

-- System Health View
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'data_source') AND
       EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'data_source_status') THEN
        EXECUTE 'CREATE OR REPLACE VIEW system_health AS
        SELECT 
            ds.name as data_source,
            ds.source_type,
            ds.is_enabled,
            ds.is_active,
            ds.last_success_time,
            ds.consecutive_failures,
            CASE 
                WHEN ds.total_requests = 0 THEN 0.0 
                ELSE (ds.successful_requests::float / ds.total_requests::float * 100)
            END as success_rate,
            dss.status as last_status,
            dss.check_time as last_check
        FROM data_source ds
        LEFT JOIN LATERAL (
            SELECT status, check_time
            FROM data_source_status
            WHERE data_source_id = ds.id
            ORDER BY check_time DESC
            LIMIT 1
        ) dss ON true
        ORDER BY ds.priority';
        
        RAISE NOTICE 'Created system_health view successfully';
    ELSE
        RAISE NOTICE 'Tables data_source or data_source_status do not exist, skipping system_health view creation';
    END IF;
END $$;

-- ============================================================================
-- COMPLETION MESSAGE
-- ============================================================================

DO $$
BEGIN
    RAISE NOTICE '=================================================================';
    RAISE NOTICE 'TimescaleDB Hypertables Setup Complete';
    RAISE NOTICE '=================================================================';
    RAISE NOTICE 'Hypertables created for:';
    RAISE NOTICE '  - flight_tracking (1 day chunks)';
    RAISE NOTICE '  - ship_tracking (1 day chunks)';
    RAISE NOTICE '  - tracking_points (1 day chunks)';
    RAISE NOTICE '  - alert_event (7 day chunks)';
    RAISE NOTICE '  - data_source_status (1 hour chunks)';
    RAISE NOTICE '';
    RAISE NOTICE 'Continuous aggregates (created when tables available):';
    RAISE NOTICE '  - aircraft_stats_5min';
    RAISE NOTICE '  - vessel_stats_10min';
    RAISE NOTICE '  - traffic_stats_hourly';
    RAISE NOTICE '';
    RAISE NOTICE 'Utility views (created when tables available):';
    RAISE NOTICE '  - latest_positions';
    RAISE NOTICE '  - active_alerts';
    RAISE NOTICE '  - system_health';
    RAISE NOTICE '';
    RAISE NOTICE 'Retention policies applied:';
    RAISE NOTICE '  - Flight/Ship tracking: 30 days';
    RAISE NOTICE '  - Tracking points: 90 days';
    RAISE NOTICE '  - Alert events: 1 year';
    RAISE NOTICE '  - Source status: 30 days';
    RAISE NOTICE '=================================================================';
END $$; 