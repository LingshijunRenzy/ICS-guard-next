-- =============================================================================
-- V5: Permission metadata table + audit columns
-- =============================================================================

-- 1. Add audit columns to permissions
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS created_by VARCHAR(64);

-- 2. Add updated_at trigger (reuse existing trigger function from V2)
DROP TRIGGER IF EXISTS trg_permissions_updated_at ON permissions;
CREATE TRIGGER trg_permissions_updated_at
    BEFORE UPDATE ON permissions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- 3. Create permission_metadata table
CREATE TABLE IF NOT EXISTS permission_metadata (
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    key           VARCHAR(128) NOT NULL,
    value         TEXT NOT NULL,
    PRIMARY KEY (permission_id, key)
);
