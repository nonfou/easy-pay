-- ============================================================
-- Easy-Pay 数据库初始化脚本
-- 合并自: V1~V7 迁移脚本
-- 生成时间: 2025-11-25
-- 与 Java Entity 完全对应
-- ============================================================

-- ------------------------------------------------------------
-- 用户表 (MerchantEntity -> user)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pid BIGINT NOT NULL UNIQUE COMMENT '商户ID',
    username VARCHAR(255) COMMENT '登录用户名',
    password VARCHAR(255) COMMENT '登录密码(BCrypt加密)',
    email VARCHAR(255) COMMENT '邮箱',
    secret_key VARCHAR(64) NOT NULL COMMENT 'API密钥',
    role INT NOT NULL DEFAULT 0 COMMENT '角色: 0-普通用户, 1-管理员',
    state INT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    UNIQUE INDEX idx_user_username (username)
);

-- ------------------------------------------------------------
-- 支付账号表 (PayAccountEntity -> pay_account)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pay_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pid BIGINT NOT NULL COMMENT '所属商户ID',
    platform VARCHAR(255) NOT NULL COMMENT '平台类型',
    account VARCHAR(255) NOT NULL COMMENT '账号',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    state INT NOT NULL COMMENT '状态: 0-禁用, 1-启用',
    pattern INT NOT NULL COMMENT '模式',
    params TEXT COMMENT '扩展参数(JSON)',
    created_at DATETIME COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    INDEX idx_pay_account_pid (pid)
);

-- ------------------------------------------------------------
-- 支付通道表 (PayChannelEntity -> pay_channel)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pay_channel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_id BIGINT NOT NULL COMMENT '所属账号ID',
    channel VARCHAR(255) NOT NULL COMMENT '通道标识',
    qrcode VARCHAR(255) COMMENT '收款码URL',
    last_time DATETIME COMMENT '最后使用时间',
    state INT NOT NULL COMMENT '状态: 0-禁用, 1-启用',
    type VARCHAR(255) COMMENT '支付类型',
    CONSTRAINT fk_channel_account FOREIGN KEY (account_id) REFERENCES pay_account(id) ON DELETE CASCADE,
    INDEX idx_pay_channel_account (account_id)
);

-- ------------------------------------------------------------
-- 订单表 (OrderEntity -> orders)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id VARCHAR(255) NOT NULL UNIQUE COMMENT '系统订单号',
    pid BIGINT NOT NULL COMMENT '商户ID',
    type VARCHAR(255) NOT NULL COMMENT '支付类型: wxpay/alipay',
    out_trade_no VARCHAR(255) NOT NULL COMMENT '商户订单号',
    notify_url VARCHAR(255) NOT NULL COMMENT '异步通知URL',
    return_url VARCHAR(255) COMMENT '同步跳转URL',
    name VARCHAR(255) NOT NULL COMMENT '商品名称',
    money DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    really_price DECIMAL(10,2) NOT NULL COMMENT '实际支付金额(防碰撞)',
    clientip VARCHAR(255) COMMENT '客户端IP',
    device VARCHAR(255) COMMENT '设备类型',
    param TEXT COMMENT '透传参数',
    state INT NOT NULL DEFAULT 0 COMMENT '状态: 0-待支付, 1-已支付, 2-已关闭',
    patt INT COMMENT '支付模式',
    create_time DATETIME COMMENT '创建时间',
    close_time DATETIME COMMENT '关闭时间',
    pay_time DATETIME COMMENT '支付时间',
    aid BIGINT COMMENT '支付账号ID',
    cid BIGINT COMMENT '支付通道ID',
    platform_order VARCHAR(255) COMMENT '平台订单号',
    INDEX idx_orders_pid (pid),
    INDEX idx_orders_state (state),
    INDEX idx_orders_create_time (create_time),
    INDEX idx_orders_pid_state (pid, state)
);

-- ------------------------------------------------------------
-- 订单通知日志表 (OrderNotifyLogEntity -> order_notify_log)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_notify_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id VARCHAR(255) NOT NULL COMMENT '订单号',
    status INT NOT NULL COMMENT '通知状态: 0-待通知, 1-成功, 2-失败',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    last_error VARCHAR(512) COMMENT '最后错误信息',
    next_retry_time DATETIME COMMENT '下次重试时间',
    created_at DATETIME COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    INDEX idx_notify_log_order (order_id),
    INDEX idx_notify_log_status (status)
);

-- ------------------------------------------------------------
-- 插件定义表 (PluginEntity -> plugin_definition)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS plugin_definition (
    platform VARCHAR(64) PRIMARY KEY COMMENT '平台标识',
    name VARCHAR(255) NOT NULL COMMENT '插件名称',
    class_name VARCHAR(255) NOT NULL COMMENT '类名',
    price VARCHAR(255) COMMENT '价格说明',
    describe_text TEXT COMMENT '描述',
    website VARCHAR(255) COMMENT '官网',
    state INT NOT NULL COMMENT '状态: 0-禁用, 1-启用',
    install TINYINT(1) NOT NULL COMMENT '安装状态: 0-未安装, 1-已安装',
    query TEXT COMMENT '查询配置',
    version VARCHAR(255) COMMENT '版本号',
    author VARCHAR(255) COMMENT '作者',
    download_url VARCHAR(255) COMMENT '下载地址'
);

-- ============================================================
-- 初始数据
-- ============================================================

-- 创建默认管理员账号 (用户名: admin, 密码: admin123)
-- BCrypt 哈希使用 Python bcrypt 库生成: bcrypt.hashpw(b'admin123', bcrypt.gensalt())
INSERT INTO user (pid, username, password, email, secret_key, role, state)
SELECT 1000, 'admin', '$2b$12$wHpHuFjuOlH4NIICMqL9Hu3O6CV/ofbBnZaoFA0nIlXbOSMU1fPAe', 'admin@example.com', 'default_secret_key_please_change', 1, 1
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM user WHERE username = 'admin');
