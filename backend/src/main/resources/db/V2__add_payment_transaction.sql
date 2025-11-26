-- V2__add_payment_transaction.sql
-- 支付交易记录表，用于记录与第三方支付平台的交易流水

CREATE TABLE IF NOT EXISTS payment_transaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id VARCHAR(255) NOT NULL COMMENT '系统订单号',
    platform VARCHAR(50) NOT NULL COMMENT '支付平台: alipay/wxpay',
    trade_no VARCHAR(255) COMMENT '平台交易号',
    trade_type VARCHAR(50) COMMENT '交易类型: NATIVE/JSAPI/H5/PC',
    amount DECIMAL(10,2) NOT NULL COMMENT '交易金额',
    status VARCHAR(50) NOT NULL COMMENT '交易状态: PENDING/SUCCESS/FAILED/REFUND',
    raw_request TEXT COMMENT '原始请求数据',
    raw_response TEXT COMMENT '原始响应数据',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_payment_trans_order (order_id),
    INDEX idx_payment_trans_trade_no (trade_no),
    INDEX idx_payment_trans_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付交易记录表';

-- 退款记录表
CREATE TABLE IF NOT EXISTS refund_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id VARCHAR(255) NOT NULL COMMENT '系统订单号',
    refund_no VARCHAR(255) NOT NULL COMMENT '退款单号',
    platform VARCHAR(50) NOT NULL COMMENT '支付平台: alipay/wxpay',
    platform_refund_no VARCHAR(255) COMMENT '平台退款单号',
    refund_amount DECIMAL(10,2) NOT NULL COMMENT '退款金额',
    refund_reason VARCHAR(500) COMMENT '退款原因',
    status VARCHAR(50) NOT NULL COMMENT '退款状态: PENDING/SUCCESS/FAILED',
    raw_response TEXT COMMENT '原始响应数据',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_refund_order (order_id),
    INDEX idx_refund_no (refund_no),
    INDEX idx_refund_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款记录表';
