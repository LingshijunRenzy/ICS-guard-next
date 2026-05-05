-- =============================================================================
-- V6: Permission types table + type_id FK on permissions
-- =============================================================================

-- 1. Create permission_types table
CREATE TABLE IF NOT EXISTS permission_types (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(256)
);

-- 2. Add type_id column to permissions (nullable, no FK constraint for flexibility)
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS type_id BIGINT;
ALTER TABLE permissions ADD CONSTRAINT fk_permissions_type
    FOREIGN KEY (type_id) REFERENCES permission_types(id) ON DELETE SET NULL;
