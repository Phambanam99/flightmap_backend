-- ============================================================================
-- TimescaleDB & PostGIS Extensions Initialization
-- ============================================================================
-- This file initializes essential extensions for the tracking system
-- Execution order: 01 (runs first)

-- Enable PostGIS extension for geospatial operations
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;

-- Enable TimescaleDB extension for time-series data
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Enable additional useful extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Verify extensions are created successfully
DO $$
BEGIN
    -- Check PostGIS
    IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'postgis') THEN
        RAISE EXCEPTION 'PostGIS extension failed to install';
    END IF;
    
    -- Check TimescaleDB
    IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'timescaledb') THEN
        RAISE EXCEPTION 'TimescaleDB extension failed to install';
    END IF;
    
    RAISE NOTICE 'All extensions installed successfully';
END $$;

-- Display installed extensions
SELECT 
    name as extension_name,
    default_version,
    installed_version,
    CASE 
        WHEN installed_version IS NOT NULL THEN 'INSTALLED'
        ELSE 'NOT INSTALLED'
    END as status
FROM pg_available_extensions 
WHERE name IN ('postgis', 'postgis_topology', 'timescaledb', 'uuid-ossp', 'btree_gist', 'pg_stat_statements')
ORDER BY name; 