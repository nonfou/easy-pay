-- P2: 插件表添加远程市场字段
ALTER TABLE plugin_definition ADD COLUMN version VARCHAR(32) NULL;
ALTER TABLE plugin_definition ADD COLUMN author VARCHAR(64) NULL;
ALTER TABLE plugin_definition ADD COLUMN download_url VARCHAR(512) NULL;

-- P2: 用户表添加角色字段
ALTER TABLE user ADD COLUMN role TINYINT NOT NULL DEFAULT 0 COMMENT '0:普通用户, 1:管理员';
