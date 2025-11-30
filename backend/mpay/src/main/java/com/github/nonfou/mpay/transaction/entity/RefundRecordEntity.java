package com.github.nonfou.mpay.transaction.entity;

import com.github.nonfou.mpay.transaction.enums.PaymentPlatform;
import com.github.nonfou.mpay.transaction.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款记录实体
 */
@Entity
@Table(name = "refund_record", indexes = {
        @Index(name = "idx_refund_transaction_id", columnList = "transactionId"),
        @Index(name = "idx_refund_no", columnList = "refundNo"),
        @Index(name = "idx_platform_refund_no", columnList = "platformRefundNo"),
        @Index(name = "idx_refund_status", columnList = "status"),
        @Index(name = "idx_refund_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
public class RefundRecordEntity extends BaseEntity {

    /**
     * 关联的交易ID
     */
    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    /**
     * 商户订单号
     */
    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    /**
     * 系统退款单号
     */
    @Column(name = "refund_no", nullable = false, unique = true, length = 64)
    private String refundNo;

    /**
     * 支付平台
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private PaymentPlatform platform;

    /**
     * 支付平台退款单号
     */
    @Column(name = "platform_refund_no", length = 64)
    private String platformRefundNo;

    /**
     * 原交易平台单号
     */
    @Column(name = "platform_trade_no", length = 64)
    private String platformTradeNo;

    /**
     * 退款金额（单位：元）
     */
    @Column(name = "refund_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount;

    /**
     * 退款原因
     */
    @Column(name = "refund_reason", length = 256)
    private String refundReason;

    /**
     * 退款状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RefundStatus status = RefundStatus.PENDING;

    /**
     * 退款请求原始数据（JSON）
     */
    @Column(name = "raw_request", columnDefinition = "TEXT")
    private String rawRequest;

    /**
     * 退款响应原始数据（JSON）
     */
    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    /**
     * 退款回调数据（JSON）
     */
    @Column(name = "notify_data", columnDefinition = "TEXT")
    private String notifyData;

    /**
     * 退款成功时间
     */
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    /**
     * 操作人
     */
    @Column(name = "operator", length = 64)
    private String operator;

    /**
     * 扩展字段（JSON格式）
     */
    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;
}
