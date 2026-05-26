-- Initial schema for Chiller Guard ingestion engine
-- Creates tables for buildings, chiller units, and sensor readings

-- Buildings table
CREATE TABLE IF NOT EXISTS buildings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    address VARCHAR(500),
    external_id VARCHAR(100) NOT NULL UNIQUE,
    bms_type VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Chiller units table
CREATE TABLE IF NOT EXISTS chiller_units (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    external_id VARCHAR(100) NOT NULL UNIQUE,
    model VARCHAR(100),
    manufacturer VARCHAR(100),
    building_id UUID NOT NULL REFERENCES buildings(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'ONLINE',
    installed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Sensor readings table (time-series data)
CREATE TABLE IF NOT EXISTS sensor_readings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chiller_unit_id UUID NOT NULL REFERENCES chiller_units(id) ON DELETE CASCADE,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    inlet_temp DECIMAL(8, 3),
    outlet_temp DECIMAL(8, 3),
    ambient_temp DECIMAL(8, 3),
    suction_pressure DECIMAL(10, 3),
    discharge_pressure DECIMAL(10, 3),
    condenser_pressure DECIMAL(10, 3),
    power_consumption DECIMAL(10, 3),
    compressor_current DECIMAL(10, 3),
    vibration_x DECIMAL(8, 4),
    vibration_y DECIMAL(8, 4),
    vibration_z DECIMAL(8, 4),
    coolant_flow_rate DECIMAL(10, 3),
    operational_mode VARCHAR(50),
    compressor_running BOOLEAN,
    runtime_hours INTEGER,
    data_source VARCHAR(50) NOT NULL,
    bms_point_ids VARCHAR(100),
    is_valid BOOLEAN DEFAULT TRUE,
    validation_errors VARCHAR(500),
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_building_external_id ON buildings(external_id);
CREATE INDEX IF NOT EXISTS idx_building_status ON buildings(status);

CREATE INDEX IF NOT EXISTS idx_unit_external_id ON chiller_units(external_id);
CREATE INDEX IF NOT EXISTS idx_unit_building_id ON chiller_units(building_id);
CREATE INDEX IF NOT EXISTS idx_unit_status ON chiller_units(status);

CREATE INDEX IF NOT EXISTS idx_reading_unit_id ON sensor_readings(chiller_unit_id);
CREATE INDEX IF NOT EXISTS idx_reading_timestamp ON sensor_readings(timestamp);
CREATE INDEX IF NOT EXISTS idx_reading_unit_timestamp ON sensor_readings(chiller_unit_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_reading_source ON sensor_readings(data_source);

-- Convert sensor_readings to TimescaleDB hypertable for time-series optimization
-- This enables automatic partitioning by time and efficient time-series queries
SELECT create_hypertable('sensor_readings', 'timestamp', if_not_exists => TRUE);

-- Add compression policy for old data (optional, adjust retention as needed)
-- ALTER TABLE sensor_readings SET (timescaledb.compress, timescaledb.compress_segmentby = 'chiller_unit_id');
-- SELECT add_compression_policy('sensor_readings', INTERVAL '7 days');
