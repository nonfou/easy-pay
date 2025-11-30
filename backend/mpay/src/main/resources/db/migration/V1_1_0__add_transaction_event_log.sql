-- ============================================================
-- Easy-Pay 数据库迁移脚本
-- 版本: 1.1.0
-- 说明: 扩展支付交易和退款表，新增事件日志表
-- ============================================================

-- ------------------------------------------------------------
-- 1. 扩展 payment_transaction 表
-- ------------------------------------------------------------
ALTER TABLE payment_transaction
    ADD COLUMN IF NOT EXISTS trade_no VARCHAR(64) UNIQUE COMMENT '系统交易号' AFTER order_id,
    ADD COLUMN IF NOT EXISTS platform_trade_no VARCHAR(64) COMMENT '平台交易号' AFTER trade_no,
    ADD COLUMN IF NOT EXISTS refunded_amount DECIMAL(10,2) DEFAULT 0 COMMENT '已退款金额' AFTER amount,
    ADD COLUMN IF NOT EXISTS subject VARCHAR(256) COMMENT '商品描述' AFTER status,
    ADD COLUMN IF NOT EXISTS notify_data TEXT COMMENT '回调通知数据' AFTER raw_response,
    ADD COLUMN IF NOT EXISTS paid_at DATETIME COMMENT '支付成功时间' AFTER notify_data,
    ADD COLUMN IF NOT EXISTS client_ip VARCHAR(64) COMMENT '客户端IP' AFTER paid_at,
    ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64) COMMENT '商户ID' AFTER client_ip,
    ADD COLUMN IF NOT EXISTS extra_data TEXT COMMENT '扩展数据' AFTER merchant_id;

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_payment_trans_platform_trade ON payment_transaction(platform_trade_no);
CREATE INDEX IF NOT EXISTS idx_payment_trans_created ON payment_transaction(created_at);

-- ------------------------------------------------------------
-- 2. 扩展 refund_record 表
-- ------------------------------------------------------------
ALTER TABLE refund_record
    ADD COLUMN IF NOT EXISTS transaction_id BIGINT COMMENT '关联交易ID' AFTER id,
    ADD COLUMN IF NOT EXISTS platform_trade_no VARCHAR(64) COMMENT '原支付平台交易号' AFTER platform_refund_no,
    ADD COLUMN IF NOT EXISTS raw_request TEXT COMMENT '原始请求数据' AFTER refund_reason,
    ADD COLUMN IF NOT EXISTS notify_data TEXT COMMENT '退款回调数据' AFTER raw_response,
    ADD COLUMN IF NOT EXISTS refunded_at DATETIME COMMENT '退款成功时间' AFTER notify_data,
    ADD COLUMN IF NOT EXISTS operator VARCHAR(64) COMMENT '操作人' AFTER refunded_at,
    ADD COLUMN IF NOT EXISTS extra_data TEXT COMMENT '扩展数据' AFTER operator;

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_refund_transaction ON refund_record(transaction_id);
CREATE INDEX IF NOT EXISTS idx_refund_platform_refund ON refund_record(platform_refund_no);
CREATE INDEX IF NOT EXISTS idx_refund_created ON refund_record(created_at);

-- ------------------------------------------------------------
-- 3. 创建事件日志表 (payment_event_log)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payment_event_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id VARCHAR(64) COMMENT '商户订单号',
    transaction_id BIGINT COMMENT '关联交易ID',
    refund_id BIGINT COMMENT '关联退款ID',
    event_type VARCHAR(32) NOT NULL COMMENT '事件类型: CREATE/NOTIFY/QUERY/CLOSE/REFUND_CREATE/REFUND_NOTIFY/REFUND_QUERY',
    platform VARCHAR(20) COMMENT '支付平台',
    request_data TEXT COMMENT '请求数据',
    response_data TEXT COMMENT '响应数据',
    result_code VARCHAR(32) COMMENT '结果码',
    result_message VARCHAR(512) COMMENT '结果消息',
    success TINYINT(1) COMMENT '是否成功',
    duration_ms BIGINT COMMENT '耗时(毫秒)',
    client_ip VARCHAR(64) COMMENT '客户端IP',
    extra_data TEXT COMMENT '扩展数据',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_event_order (order_id),
    INDEX idx_event_transaction (transaction_id),
    INDEX idx_event_type (event_type),
    INDEX idx_event_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付事件日志表';
