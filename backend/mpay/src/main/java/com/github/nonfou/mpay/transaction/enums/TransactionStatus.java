package com.github.nonfou.mpay.transaction.enums;

/**
 * 交易状态枚举
 */
public enum TransactionStatus {
    /**
     * 待支付
     */
    PENDING,

    /**
     * 支付成功
     */
    SUCCESS,

    /**
     * 支付失败
     */
    FAILED,

    /**
     * 已关闭
     */
    CLOSED,

    /**
     * 已退款
     */
    REFUNDED,

    /**
     * 部分退款
     */
    PARTIAL_REFUNDED
}
