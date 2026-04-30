-- =============================================================================
-- V1: 初始化表结构
-- =============================================================================
-- 包含: RBAC (用户/角色/权限) + 核心业务 (告警/拓扑/指标/策略) + 审计日志

-- ---------------------------------------------------------------------------
-- RBAC
-- ---------------------------------------------------------------------------

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email         VARCHAR(128),
    display_name  VARCHAR(128),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL UNIQUE,
    description VARCHAR(256)
);

CREATE TABLE permissions (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(128) NOT NULL UNIQUE,
    description VARCHAR(256)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- ---------------------------------------------------------------------------
-- 核心业务: 告警
-- ---------------------------------------------------------------------------

CREATE TABLE alerts (
    id            BIGSERIAL PRIMARY KEY,
    trace_id      VARCHAR(36)  NOT NULL UNIQUE,
    alert_type    VARCHAR(64)  NOT NULL,
    severity      VARCHAR(16)  NOT NULL DEFAULT 'medium',
    source_ip     VARCHAR(45),
    dest_ip       VARCHAR(45),
    protocol      VARCHAR(16),
    description   TEXT,
    raw_payload   JSONB,
    status        VARCHAR(32)  NOT NULL DEFAULT 'new',
    triggered_at  TIMESTAMPTZ  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_alerts_triggered_at ON alerts (triggered_at DESC);
CREATE INDEX idx_alerts_severity     ON alerts (severity);
CREATE INDEX idx_alerts_status       ON alerts (status);

-- ---------------------------------------------------------------------------
-- 核心业务: 拓扑事件
-- ---------------------------------------------------------------------------

CREATE TABLE topology_events (
    id            BIGSERIAL PRIMARY KEY,
    trace_id      VARCHAR(36)  NOT NULL UNIQUE,
    event_type    VARCHAR(64)  NOT NULL,
    device_id     VARCHAR(128),
    device_name   VARCHAR(256),
    device_type   VARCHAR(64),
    ip_address    VARCHAR(45),
    mac_address   VARCHAR(17),
    port          VARCHAR(32),
    status        VARCHAR(32),
    metadata      JSONB,
    occurred_at   TIMESTAMPTZ  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_topology_events_occurred_at ON topology_events (occurred_at DESC);
CREATE INDEX idx_topology_events_device_id   ON topology_events (device_id);
CREATE INDEX idx_topology_events_event_type  ON topology_events (event_type);

-- ---------------------------------------------------------------------------
-- 核心业务: 聚合流量指标
-- ---------------------------------------------------------------------------

CREATE TABLE traffic_metrics (
    id            BIGSERIAL PRIMARY KEY,
    trace_id      VARCHAR(36)  NOT NULL UNIQUE,
    metric_type   VARCHAR(64)  NOT NULL,
    source_ip     VARCHAR(45),
    dest_ip       VARCHAR(45),
    protocol      VARCHAR(16),
    bytes_in      BIGINT       NOT NULL DEFAULT 0,
    bytes_out     BIGINT       NOT NULL DEFAULT 0,
    packet_count  BIGINT       NOT NULL DEFAULT 0,
    flow_duration BIGINT,
    captured_at   TIMESTAMPTZ  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_traffic_metrics_captured_at ON traffic_metrics (captured_at DESC);
CREATE INDEX idx_traffic_metrics_source_ip   ON traffic_metrics (source_ip);
CREATE INDEX idx_traffic_metrics_dest_ip     ON traffic_metrics (dest_ip);

-- ---------------------------------------------------------------------------
-- 核心业务: 安全策略
-- ---------------------------------------------------------------------------

CREATE TABLE policies (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(128) NOT NULL,
    description   TEXT,
    policy_type   VARCHAR(64)  NOT NULL,
    target        VARCHAR(256),
    action        VARCHAR(64)  NOT NULL,
    priority      INT          NOT NULL DEFAULT 0,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    config_json   JSONB,
    created_by    VARCHAR(64),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_policies_enabled     ON policies (enabled);
CREATE INDEX idx_policies_policy_type ON policies (policy_type);

-- ---------------------------------------------------------------------------
-- 审计日志
-- ---------------------------------------------------------------------------

CREATE TABLE audit_logs (
    id            BIGSERIAL PRIMARY KEY,
    trace_id      VARCHAR(36)  NOT NULL UNIQUE,
    user_id       BIGINT,
    username      VARCHAR(64),
    action        VARCHAR(128) NOT NULL,
    resource      VARCHAR(128),
    resource_id   VARCHAR(64),
    detail        JSONB,
    ip_address    VARCHAR(45),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at DESC);
CREATE INDEX idx_audit_logs_user_id    ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_action     ON audit_logs (action);

-- ---------------------------------------------------------------------------
-- 默认数据
-- ---------------------------------------------------------------------------

-- 角色
INSERT INTO roles (name, description) VALUES
    ('ROLE_ADMIN', 'System administrator — full access'),
    ('ROLE_OPERATOR', 'Security operator — manage alerts and policies'),
    ('ROLE_VIEWER', 'Read-only viewer — dashboard and reports');

-- 权限
INSERT INTO permissions (name, description) VALUES
    ('alerts:read', 'View threat alerts'),
    ('alerts:manage', 'Acknowledge, resolve, or escalate alerts'),
    ('topology:read', 'View topology maps and events'),
    ('metrics:read', 'View traffic metrics'),
    ('policies:read', 'View security policies'),
    ('policies:manage', 'Create, update, or delete security policies'),
    ('users:read', 'View users and roles'),
    ('users:manage', 'Create, update, or disable users'),
    ('audit:read', 'View audit logs');

-- 绑定 — admin 拥有所有权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ROLE_ADMIN';

-- 绑定 — operator 可管理告警和策略
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_OPERATOR'
  AND p.name IN ('alerts:read', 'alerts:manage',
                 'topology:read', 'metrics:read',
                 'policies:read', 'policies:manage',
                 'audit:read');

-- 绑定 — viewer 只读
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_VIEWER'
  AND p.name IN ('alerts:read', 'topology:read', 'metrics:read', 'policies:read');

-- 默认 admin 用户 (密码: admin123, 仅开发环境使用)
-- BCrypt hash of 'admin123'
INSERT INTO users (username, password_hash, email, display_name) VALUES
    ('admin', '{bcrypt}$2b$10$iGIZAHD0xvfS8W1RPyal3OYKDK0uPYuO9ivL21u1jbX3xMo5yU5FC',
     'admin@ics-guard.local', 'Administrator');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN';
