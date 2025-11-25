-- V6: 用户认证相关字段
-- 添加密码字段 (BCrypt 加密)
ALTER TABLE user ADD COLUMN password VARCHAR(255) NULL COMMENT '登录密码(BCrypt加密)';

-- 为 username 添加唯一索引
ALTER TABLE user ADD UNIQUE INDEX idx_user_username (username);

-- 创建默认管理员账号 (密码: admin123)
-- BCrypt 加密后的 admin123
INSERT INTO user (pid, secret_key, username, password, email, role, state)
SELECT 1000, 'default_secret_key_please_change', 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@example.com', 1, 1
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM user WHERE username = 'admin');
