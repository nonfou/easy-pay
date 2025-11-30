package com.github.nonfou.mpay.transaction.enums;

/**
 * 支付事件类型枚举
 */
public enum PaymentEventType {
    /**
     * 创建支付订单
     */
    CREATE,

    /**
     * 支付回调通知
     */
    NOTIFY,

    /**
     * 主动查询订单状态
     */
    QUERY,

    /**
     * 关闭订单
     */
    CLOSE,

    /**
     * 发起退款
     */
    REFUND_CREATE,

    /**
     * 退款回调通知
     */
    REFUND_NOTIFY,

    /**
     * 退款查询
     */
    REFUND_QUERY
}
