-- Extensions đã được tạo trong file 01-init-extensions.sql

-- Tạo bảng cold storage cho flight tracking
DROP TABLE IF EXISTS flight_tracking_archive;
CREATE TABLE flight_tracking_archive (
                                         id BIGSERIAL,
                                         tracking_id BIGINT NOT NULL,
                                         flight_id BIGINT,
                                         callsign VARCHAR(255),
                                         altitude FLOAT,
                                         altitude_type VARCHAR(255),
                                         target_alt FLOAT,
                                         speed FLOAT,
                                         speed_type VARCHAR(255),
                                         vertical_speed FLOAT,
                                         squawk INTEGER,
                                         distance FLOAT,
                                         bearing FLOAT,
                                         unix_time BIGINT,
                                         update_time TIMESTAMP,
                                         longitude DOUBLE PRECISION,
                                         latitude DOUBLE PRECISION,
                                         landing_unix_times BIGINT,
                                         landing_times TIMESTAMP,
                                         created_at TIMESTAMP,
                                         updated_at TIMESTAMP,
                                         location GEOMETRY(Point, 4326)
);

-- Tạo index để tối ưu truy vấn
CREATE INDEX idx_flight_archive_time ON flight_tracking_archive (update_time);
CREATE INDEX idx_flight_archive_flight_id ON flight_tracking_archive (flight_id);
CREATE INDEX idx_flight_archive_location ON flight_tracking_archive USING GIST (location);

-- Tạo partitioning theo thời gian (mỗi tháng một partition)
SELECT create_hypertable('flight_tracking_archive', 'update_time',
                         chunk_time_interval => interval '1 month');

-- Tạo bảng cold storage cho vessel tracking nếu cần
CREATE TABLE vessel_tracking_archive (
                                         id BIGSERIAL,
                                         tracking_id BIGINT NOT NULL,
                                         vessel_id BIGINT,
                                         heading FLOAT,
                                         speed FLOAT,
                                         course FLOAT,
                                         status VARCHAR(255),
                                         timestamp TIMESTAMP,
                                         unix_time BIGINT,
                                         longitude DOUBLE PRECISION,
                                         latitude DOUBLE PRECISION,
                                         created_at TIMESTAMP,
                                         updated_at TIMESTAMP,
                                         location GEOMETRY(Point, 4326)
);

-- Tạo index
CREATE INDEX idx_vessel_archive_time ON vessel_tracking_archive (timestamp);
CREATE INDEX idx_vessel_archive_vessel_id ON vessel_tracking_archive (vessel_id);
CREATE INDEX idx_vessel_archive_location ON vessel_tracking_archive USING GIST (location);

-- Tạo partitioning theo thời gian
SELECT create_hypertable('vessel_tracking_archive', 'timestamp',
                         chunk_time_interval => interval '1 month');