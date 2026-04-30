-- =============================================================================
-- V3: 新增 Dashboard、SDN Controller、WebSocket 权限
-- =============================================================================

INSERT INTO permissions (name, description) VALUES
    ('dashboard:read', 'View unified dashboard overview'),
    ('sdn:read', 'View SDN controller health and status'),
    ('sdn:manage', 'Manage SDN flows and push rules'),
    ('alerts:subscribe', 'Subscribe to real-time alert push notifications via WebSocket')
ON CONFLICT (name) DO NOTHING;

-- ROLE_ADMIN: 所有新权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ROLE_ADMIN'
  AND p.name IN ('dashboard:read', 'sdn:read', 'sdn:manage', 'alerts:subscribe')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ROLE_OPERATOR: dashboard, sdn, alerts 订阅
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROLE_OPERATOR'
  AND p.name IN ('dashboard:read', 'sdn:read', 'sdn:manage', 'alerts:subscribe')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ROLE_VIEWER: dashboard 只读
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROLE_VIEWER'
  AND p.name = 'dashboard:read'
ON CONFLICT (role_id, permission_id) DO NOTHING;
