-- =============================================================================
-- V7: 补齐 permissions type + 新增独立权限点
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. 插入权限类型
-- ---------------------------------------------------------------------------
INSERT INTO permission_types (name, description) VALUES
    ('monitoring', '监控告警 — 仪表盘、告警查看与处理'),
    ('network',    '网络管理 — 拓扑、流量指标、SDN 控制'),
    ('policy',     '策略规则 — 检测规则与安全策略'),
    ('admin',      '系统管理 — 用户、角色、权限管理'),
    ('audit',      '审计日志 — 操作审计与合规');

-- ---------------------------------------------------------------------------
-- 2. 为现有权限补齐 type_id
-- ---------------------------------------------------------------------------
UPDATE permissions SET type_id = (SELECT id FROM permission_types WHERE name = 'monitoring')
    WHERE name IN ('dashboard:read', 'alerts:read', 'alerts:manage', 'alerts:subscribe');
UPDATE permissions SET type_id = (SELECT id FROM permission_types WHERE name = 'network')
    WHERE name IN ('topology:read', 'metrics:read', 'sdn:read', 'sdn:manage');
UPDATE permissions SET type_id = (SELECT id FROM permission_types WHERE name = 'policy')
    WHERE name IN ('policies:read', 'policies:manage');
UPDATE permissions SET type_id = (SELECT id FROM permission_types WHERE name = 'admin')
    WHERE name IN ('users:read', 'users:manage');
UPDATE permissions SET type_id = (SELECT id FROM permission_types WHERE name = 'audit')
    WHERE name = 'audit:read';

-- ---------------------------------------------------------------------------
-- 3. 新增权限点
-- ---------------------------------------------------------------------------
INSERT INTO permissions (name, description, type_id) VALUES
    ('rules:read',       'View detection rules and policies',          (SELECT id FROM permission_types WHERE name = 'policy')),
    ('rules:manage',     'Create, update, enable, or delete rules',    (SELECT id FROM permission_types WHERE name = 'policy')),
    ('roles:read',       'View roles and their permission assignments',(SELECT id FROM permission_types WHERE name = 'admin')),
    ('roles:manage',     'Create, update, or delete roles',            (SELECT id FROM permission_types WHERE name = 'admin')),
    ('permissions:read', 'View permission catalog',                    (SELECT id FROM permission_types WHERE name = 'admin')),
    ('permissions:manage','Create, update, or delete permissions',     (SELECT id FROM permission_types WHERE name = 'admin'))
ON CONFLICT (name) DO UPDATE SET type_id = EXCLUDED.type_id;

-- ---------------------------------------------------------------------------
-- 4. ROLE_ADMIN 获得所有新权限
-- ---------------------------------------------------------------------------
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ROLE_ADMIN'
  AND p.name IN ('rules:read', 'rules:manage', 'roles:read', 'roles:manage',
                 'permissions:read', 'permissions:manage')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 5. ROLE_OPERATOR 获得 rules 管理权限 + roles/permissions 查看权限
-- ---------------------------------------------------------------------------
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROLE_OPERATOR'
  AND p.name IN ('rules:read', 'rules:manage', 'roles:read', 'permissions:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 6. ROLE_VIEWER 获得 rules 和 roles 只读权限
-- ---------------------------------------------------------------------------
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROLE_VIEWER'
  AND p.name IN ('rules:read', 'roles:read', 'permissions:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;
