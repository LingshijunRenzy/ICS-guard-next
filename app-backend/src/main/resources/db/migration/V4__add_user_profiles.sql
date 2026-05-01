-- =============================================================================
-- V4: 用户偏好设置表
-- =============================================================================

CREATE TABLE IF NOT EXISTS user_profiles (
    user_id    BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    language   VARCHAR(10)  NOT NULL DEFAULT 'en',
    timezone   VARCHAR(50)  NOT NULL DEFAULT 'UTC',
    theme      VARCHAR(20)  NOT NULL DEFAULT 'light',
    updated_at TIMESTAMP    NOT NULL DEFAULT now()
);

-- 为已有用户补默认行
INSERT INTO user_profiles (user_id) SELECT id FROM users ON CONFLICT DO NOTHING;
