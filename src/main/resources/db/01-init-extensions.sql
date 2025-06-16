-- File khởi tạo extensions
-- File này sẽ chạy trước các file khác do tên bắt đầu với 01-

-- Tạo PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;

-- Tạo TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Verify extensions are created
SELECT name, default_version, installed_version 
FROM pg_available_extensions 
WHERE name IN ('postgis', 'timescaledb'); 