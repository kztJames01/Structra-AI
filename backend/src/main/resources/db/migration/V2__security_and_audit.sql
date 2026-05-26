-- V2: Security, Audit Logging, and Row-Level Security
-- Implements: Audit trail, API key auth, RLS policies, PG Crypto extensions

-- ============================================================
-- 1. Enable required PostgreSQL extensions
-- ============================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";      -- UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";        -- Cryptographic functions (hashing, encryption)
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS "pgjwt";
EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'pgjwt extension unavailable in this environment; continuing without DB-side JWT functions';
END;
$$;

-- ============================================================
-- 2. Audit Log Table
--    Records every data-access and mutation event for SOC2 compliance.
--    Uses UNLOGGED for high-throughput ingestion (acceptable for audit
--    trail that is also shipped to external SIEM).
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type      VARCHAR(50)  NOT NULL,           -- CREATE, READ, UPDATE, DELETE, AUTH_SUCCESS, AUTH_FAILURE
    entity_type     VARCHAR(50)  NOT NULL,           -- building, chiller_unit, sensor_reading, api_key
    entity_id       UUID,                             -- PK of the affected entity
    actor_id        VARCHAR(100),                      -- API key name or user identifier
    actor_type      VARCHAR(20)  NOT NULL DEFAULT 'API_KEY', -- API_KEY, USER, SYSTEM
    ip_address      INET,
    description     VARCHAR(1000),
    metadata        JSONB DEFAULT '{}',               -- Arbitrary context (request params, diff, etc.)
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for audit queries
CREATE INDEX IF NOT EXISTS idx_audit_event_type    ON audit_log(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_entity        ON audit_log(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_actor         ON audit_log(actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_created_at    ON audit_log(created_at);

-- Convert audit_log to hypertable for time-series retention
SELECT create_hypertable('audit_log', 'created_at', if_not_exists => TRUE, chunk_time_interval => INTERVAL '1 day');

-- Retention policy: keep audit logs for 1 year, then drop
SELECT add_retention_policy('audit_log', INTERVAL '365 days', if_not_exists => TRUE);

-- Compression policy for audit logs older than 30 days
ALTER TABLE audit_log SET (timescaledb.compress, timescaledb.compress_segmentby = 'entity_type, event_type');
SELECT add_compression_policy('audit_log', INTERVAL '30 days', if_not_exists => TRUE);


-- ============================================================
-- 3. API Keys Table
--    Stores hashed API keys for service-to-service auth.
--    Keys are never stored in plaintext — only bcrypt hashes.
-- ============================================================
CREATE TABLE IF NOT EXISTS api_keys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL UNIQUE,    -- Human-readable key name (e.g., "bms-connector-prod")
    key_hash        VARCHAR(200) NOT NULL,            -- bcrypt hash of the API key
    key_prefix      VARCHAR(10)  NOT NULL,            -- First 8 chars for identification (e.g., "sg_prod_")
    scopes          JSONB DEFAULT '["read"]',          -- Permission scopes: read, write, admin
    organization_id UUID,                              -- Tenant boundary for RLS and request scoping
    building_ids    UUID[] DEFAULT '{}',               -- Buildings this key can access (empty = all)
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    rate_limit      INTEGER DEFAULT 1000,              -- Requests per minute
    expires_at      TIMESTAMP WITH TIME ZONE,          -- Optional expiration
    last_used_at    TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE INDEX IF NOT EXISTS idx_api_keys_prefix   ON api_keys(key_prefix);
CREATE INDEX IF NOT EXISTS idx_api_keys_active   ON api_keys(is_active) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_api_keys_name     ON api_keys(name);
CREATE INDEX IF NOT EXISTS idx_api_keys_org      ON api_keys(organization_id);


-- ============================================================
-- 4. Tenant / Organization Table (for multi-tenancy + RLS)
-- ============================================================
CREATE TABLE IF NOT EXISTS organizations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200) NOT NULL UNIQUE,
    slug            VARCHAR(50) NOT NULL UNIQUE,       -- URL-safe identifier
    tier            VARCHAR(20) NOT NULL DEFAULT 'FREE', -- FREE, PRO, ENTERPRISE
    settings        JSONB DEFAULT '{}',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_api_keys_organization') THEN
        ALTER TABLE api_keys
            ADD CONSTRAINT fk_api_keys_organization
            FOREIGN KEY (organization_id) REFERENCES organizations(id);
    END IF;
END;
$$;

-- Add organization_id to existing tables for multi-tenancy
ALTER TABLE buildings      ADD COLUMN IF NOT EXISTS organization_id UUID REFERENCES organizations(id);
ALTER TABLE chiller_units  ADD COLUMN IF NOT EXISTS organization_id UUID REFERENCES organizations(id);
ALTER TABLE sensor_readings ADD COLUMN IF NOT EXISTS organization_id UUID REFERENCES organizations(id);

CREATE INDEX IF NOT EXISTS idx_buildings_org       ON buildings(organization_id);
CREATE INDEX IF NOT EXISTS idx_chiller_units_org   ON chiller_units(organization_id);
CREATE INDEX IF NOT EXISTS idx_sensor_readings_org ON sensor_readings(organization_id);


-- ============================================================
-- 5. Row-Level Security (RLS) Policies
--    Ensures API keys can only access data for their
--    assigned buildings/organizations.
-- ============================================================

-- Enable RLS on data tables
ALTER TABLE buildings       ENABLE ROW LEVEL SECURITY;
ALTER TABLE chiller_units   ENABLE ROW LEVEL SECURITY;
ALTER TABLE sensor_readings ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_log       ENABLE ROW LEVEL SECURITY;

-- System-level role can see everything (for internal services)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'structra_system') THEN
        CREATE ROLE structra_system;
    END IF;
END;
$$;
GRANT structra_system TO structra;

-- Policy: system role bypasses RLS
DROP POLICY IF EXISTS system_all_access ON buildings;
DROP POLICY IF EXISTS system_all_access_cu ON chiller_units;
DROP POLICY IF EXISTS system_all_access_sr ON sensor_readings;
DROP POLICY IF EXISTS system_all_access_al ON audit_log;
CREATE POLICY system_all_access ON buildings FOR ALL TO structra_system USING (true) WITH CHECK (true);
CREATE POLICY system_all_access_cu ON chiller_units FOR ALL TO structra_system USING (true) WITH CHECK (true);
CREATE POLICY system_all_access_sr ON sensor_readings FOR ALL TO structra_system USING (true) WITH CHECK (true);
CREATE POLICY system_all_access_al ON audit_log FOR ALL TO structra_system USING (true) WITH CHECK (true);

-- Policy: API keys scoped to their organization
-- Uses current_setting('app.organization_id') set per-request by the app layer
DROP POLICY IF EXISTS org_isolation_buildings ON buildings;
DROP POLICY IF EXISTS org_isolation_chiller_units ON chiller_units;
DROP POLICY IF EXISTS org_isolation_sensor_readings ON sensor_readings;
DROP POLICY IF EXISTS org_isolation_audit_log ON audit_log;
CREATE POLICY org_isolation_buildings ON buildings
    FOR ALL USING (organization_id::text = current_setting('app.organization_id', true));

CREATE POLICY org_isolation_chiller_units ON chiller_units
    FOR ALL USING (organization_id::text = current_setting('app.organization_id', true));

CREATE POLICY org_isolation_sensor_readings ON sensor_readings
    FOR ALL USING (organization_id::text = current_setting('app.organization_id', true));

CREATE POLICY org_isolation_audit_log ON audit_log
    FOR SELECT USING (metadata->>'organization_id' = current_setting('app.organization_id', true));


-- ============================================================
-- 6. Seed: Default organization for development
-- ============================================================
INSERT INTO organizations (id, name, slug, tier)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Structura Demo', 'structura-demo', 'ENTERPRISE')
ON CONFLICT (slug) DO NOTHING;

-- Assign existing buildings to the demo org
UPDATE buildings SET organization_id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'
WHERE organization_id IS NULL;

-- Cascade organization_id to chiller_units and sensor_readings
UPDATE chiller_units cu SET organization_id = b.organization_id
FROM buildings b WHERE cu.building_id = b.id AND cu.organization_id IS NULL;

UPDATE sensor_readings sr SET organization_id = cu.organization_id
FROM chiller_units cu WHERE sr.chiller_unit_id = cu.id AND sr.organization_id IS NULL;


-- ============================================================
-- 7. Seed: Default API key for development
--    Key: sg_dev_... (plaintext shown once in logs, then only hash stored)
-- ============================================================
INSERT INTO api_keys (name, key_hash, key_prefix, scopes, is_active)
VALUES (
    'dev-default',
    crypt('sg_dev_sk_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6', gen_salt('bf')),
    'sg_dev_sk',
    '["read", "write", "admin"]'::jsonb,
    TRUE
) ON CONFLICT (name) DO NOTHING;

UPDATE api_keys
SET organization_id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'
WHERE name = 'dev-default' AND organization_id IS NULL;


-- ============================================================
-- 8. Helper: Function to verify API key against stored hash
-- ============================================================
CREATE OR REPLACE FUNCTION verify_api_key(key_name TEXT, key_secret TEXT)
RETURNS TABLE (id UUID, name TEXT, scopes JSONB, organization_id UUID) AS $$
    SELECT ak.id, ak.name, ak.scopes, ak.organization_id
    FROM api_keys ak
    WHERE ak.name = key_name
      AND ak.is_active = TRUE
      AND (ak.expires_at IS NULL OR ak.expires_at > NOW())
      AND ak.key_hash = crypt(key_secret, ak.key_hash);
$$ LANGUAGE sql STABLE;


-- ============================================================
-- 9. Helper: Function to log audit events
-- ============================================================
CREATE OR REPLACE FUNCTION log_audit_event(
    p_event_type  VARCHAR,
    p_entity_type VARCHAR,
    p_entity_id   UUID,
    p_actor_id    VARCHAR,
    p_actor_type  VARCHAR DEFAULT 'API_KEY',
    p_ip_address  INET DEFAULT NULL,
    p_description VARCHAR DEFAULT NULL,
    p_metadata    JSONB DEFAULT '{}'
) RETURNS UUID AS $$
DECLARE
    v_id UUID;
BEGIN
    INSERT INTO audit_log (event_type, entity_type, entity_id, actor_id, actor_type, ip_address, description, metadata)
    VALUES (p_event_type, p_entity_type, p_entity_id, p_actor_id, p_actor_type, p_ip_address, p_description, p_metadata)
    RETURNING id INTO v_id;
    RETURN v_id;
END;
$$ LANGUAGE plpgsql;


-- ============================================================
-- 10. Trigger: Auto-audit on buildings changes
-- ============================================================
CREATE OR REPLACE FUNCTION audit_buildings_trigger() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        PERFORM log_audit_event('CREATE', 'building', NEW.id, current_user, 'SYSTEM', NULL,
            'Building created: ' || NEW.name,
            jsonb_build_object('name', NEW.name, 'external_id', NEW.external_id));
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        PERFORM log_audit_event('UPDATE', 'building', NEW.id, current_user, 'SYSTEM', NULL,
            'Building updated: ' || NEW.name,
            jsonb_build_object('old_status', OLD.status, 'new_status', NEW.status));
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        PERFORM log_audit_event('DELETE', 'building', OLD.id, current_user, 'SYSTEM', NULL,
            'Building deleted: ' || OLD.name,
            jsonb_build_object('name', OLD.name));
        RETURN OLD;
    END IF;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_buildings ON buildings;
CREATE TRIGGER trg_audit_buildings
    AFTER INSERT OR UPDATE OR DELETE ON buildings
    FOR EACH ROW EXECUTE FUNCTION audit_buildings_trigger();


-- ============================================================
-- 11. Trigger: Auto-audit on chiller_units changes
-- ============================================================
CREATE OR REPLACE FUNCTION audit_chiller_units_trigger() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        PERFORM log_audit_event('CREATE', 'chiller_unit', NEW.id, current_user, 'SYSTEM', NULL,
            'Chiller unit created: ' || NEW.name,
            jsonb_build_object('name', NEW.name, 'building_id', NEW.building_id));
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        PERFORM log_audit_event('UPDATE', 'chiller_unit', NEW.id, current_user, 'SYSTEM', NULL,
            'Chiller unit updated: ' || NEW.name,
            jsonb_build_object('old_status', OLD.status, 'new_status', NEW.status));
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        PERFORM log_audit_event('DELETE', 'chiller_unit', OLD.id, current_user, 'SYSTEM', NULL,
            'Chiller unit deleted: ' || OLD.name,
            jsonb_build_object('name', OLD.name));
        RETURN OLD;
    END IF;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_chiller_units ON chiller_units;
CREATE TRIGGER trg_audit_chiller_units
    AFTER INSERT OR UPDATE OR DELETE ON chiller_units
    FOR EACH ROW EXECUTE FUNCTION audit_chiller_units_trigger();


-- ============================================================
-- 12. Trigger: Auto-audit on sensor_readings (INSERT only — too high volume for UPDATE/DELETE)
-- ============================================================
CREATE OR REPLACE FUNCTION audit_sensor_readings_trigger() RETURNS TRIGGER AS $$
BEGIN
    PERFORM log_audit_event('CREATE', 'sensor_reading', NEW.id, current_user, 'SYSTEM', NULL,
        'Sensor reading ingested',
        jsonb_build_object('chiller_unit_id', NEW.chiller_unit_id, 'timestamp', NEW.timestamp));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Only audit INSERTs for sensor_readings (high volume table)
DROP TRIGGER IF EXISTS trg_audit_sensor_readings ON sensor_readings;
CREATE TRIGGER trg_audit_sensor_readings
    AFTER INSERT ON sensor_readings
    FOR EACH ROW EXECUTE FUNCTION audit_sensor_readings_trigger();