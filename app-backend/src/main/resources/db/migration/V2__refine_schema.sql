-- =============================================================================
-- V2: 重构与改进
-- =============================================================================
--  1. policies → rules 重命名
--  2. 新增 rule_metadata (key-value)
--  3. alerts 加 CHECK 约束
--  4. audit_logs.trace_id 去 UNIQUE
--  5. 复合索引优化
--  6. updated_at 触发器
--  7. 表/列注释
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. 重命名 policies → rules
-- ---------------------------------------------------------------------------
ALTER TABLE policies RENAME TO rules;
ALTER INDEX policies_pkey RENAME TO rules_pkey;
ALTER INDEX idx_policies_enabled RENAME TO idx_rules_enabled;
ALTER INDEX idx_policies_policy_type RENAME TO idx_rules_rule_type;

ALTER TABLE rules RENAME COLUMN policy_type TO rule_type;
ALTER TABLE rules RENAME COLUMN config_json TO rule_config;

-- 原 policies.enabled 默认值已经随表重命名，无需额外处理

-- ---------------------------------------------------------------------------
-- 2. rule_metadata — 规则的扩展键值对
-- ---------------------------------------------------------------------------
CREATE TABLE rule_metadata (
    rule_id BIGINT NOT NULL REFERENCES rules(id) ON DELETE CASCADE,
    key     VARCHAR(128) NOT NULL,
    value   TEXT         NOT NULL,
    PRIMARY KEY (rule_id, key)
);

COMMENT ON TABLE rule_metadata IS '规则扩展元数据 — 任意键值对，与 rules 表 join 使用';

-- ---------------------------------------------------------------------------
-- 3. alerts 加 CHECK 约束
-- ---------------------------------------------------------------------------
ALTER TABLE alerts
    ADD CONSTRAINT ck_alerts_severity
        CHECK (severity IN ('low', 'medium', 'high', 'critical'));

ALTER TABLE alerts
    ADD CONSTRAINT ck_alerts_status
        CHECK (status IN ('new', 'acknowledged', 'resolved', 'escalated', 'false_positive'));

-- ---------------------------------------------------------------------------
-- 4. audit_logs.trace_id 去掉 UNIQUE，改为普通索引
-- ---------------------------------------------------------------------------
ALTER TABLE audit_logs DROP CONSTRAINT audit_logs_trace_id_key;
CREATE INDEX idx_audit_logs_trace_id ON audit_logs (trace_id);

-- ---------------------------------------------------------------------------
-- 5. 补充复合索引
-- ---------------------------------------------------------------------------

-- alerts: 支持 (源,目的) 组合查询
CREATE INDEX idx_alerts_source_dest ON alerts (source_ip, dest_ip);

-- topology_events: (设备 + 事件类型 + 时间) 常用组合
CREATE INDEX idx_topology_events_device_type_time
    ON topology_events (device_id, event_type, occurred_at DESC);

-- traffic_metrics: (源,目的,时间) 时序查询
CREATE INDEX idx_traffic_metrics_source_dest_time
    ON traffic_metrics (source_ip, dest_ip, captured_at DESC);

-- rules: 按类型 + 优先级排序
CREATE INDEX idx_rules_type_priority ON rules (rule_type, priority DESC);

-- ---------------------------------------------------------------------------
-- 6. updated_at 自动触发器
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_rules_updated_at
    BEFORE UPDATE ON rules
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ---------------------------------------------------------------------------
-- 7. 表/列注释
-- ---------------------------------------------------------------------------

COMMENT ON TABLE users            IS '系统用户';
COMMENT ON COLUMN users.username  IS '登录名';
COMMENT ON COLUMN users.enabled   IS '是否启用（禁用的用户无法登录）';

COMMENT ON TABLE roles             IS '角色';
COMMENT ON COLUMN roles.name       IS 'Spring Security 角色名，须以 ROLE_ 开头';

COMMENT ON TABLE permissions       IS '权限点';
COMMENT ON COLUMN permissions.name IS '权限标识，格式 resource:action，如 alerts:read';

COMMENT ON TABLE alerts                  IS '威胁告警（从 ics.threat.alerts topic 消费）';
COMMENT ON COLUMN alerts.trace_id        IS '全局唯一追踪 ID（Kafka 消息幂等键）';
COMMENT ON COLUMN alerts.severity        IS '严重等级: low | medium | high | critical';
COMMENT ON COLUMN alerts.status          IS '处理状态: new | acknowledged | resolved | escalated | false_positive';
COMMENT ON COLUMN alerts.raw_payload     IS '原始告警 JSON（Engine 发送的完整消息体）';

COMMENT ON TABLE topology_events             IS '拓扑变更事件（从 ics.topology.events topic 消费）';
COMMENT ON COLUMN topology_events.event_type IS '事件类型: device_online | device_offline | topology_change';
COMMENT ON COLUMN topology_events.status     IS '设备状态: online | offline | degraded';

COMMENT ON TABLE traffic_metrics               IS '聚合流量指标（从 ics.traffic.metrics topic 消费）';
COMMENT ON COLUMN traffic_metrics.flow_duration IS '流持续时间（毫秒）';

COMMENT ON TABLE rules                  IS '安全策略规则';
COMMENT ON COLUMN rules.rule_type       IS '规则类型: flow_block | rate_limit | traffic_mirror | alert_suppress';
COMMENT ON COLUMN rules.action          IS '执行动作: block | allow | mirror | log';
COMMENT ON COLUMN rules.rule_config     IS '规则配置 JSON（结构化参数，如阈值、匹配条件）';

COMMENT ON TABLE rule_metadata IS '规则扩展元数据（key-value 对，如标签、分类、关联资产）';

COMMENT ON TABLE audit_logs          IS '操作审计日志';
COMMENT ON COLUMN audit_logs.action  IS '操作类型: login | logout | create_rule | delete_rule | ack_alert | ...';
COMMENT ON COLUMN audit_logs.detail  IS '操作详情 JSON（变更前后值、额外上下文）';
