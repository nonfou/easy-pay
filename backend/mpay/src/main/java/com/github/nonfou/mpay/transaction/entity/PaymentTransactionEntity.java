package com.github.nonfou.mpay.transaction.entity;

import com.github.nonfou.mpay.transaction.enums.PaymentPlatform;
import com.github.nonfou.mpay.transaction.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 支付交易记录实体
 */
@Entity
@Table(name = "payment_transaction", indexes = {
        @Index(name = "idx_order_id", columnList = "orderId"),
        @Index(name = "idx_trade_no", columnList = "tradeNo"),
        @Index(name = "idx_platform_trade_no", columnList = "platformTradeNo"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
public class PaymentTransactionEntity extends BaseEntity {

    /**
     * 商户订单号
     */
    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    /**
     * 支付平台
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private PaymentPlatform platform;

    /**
     * 系统交易号
     */
    @Column(name = "trade_no", nullable = false, unique = true, length = 64)
    private String tradeNo;

    /**
     * 支付平台交易号（支付宝/微信返回）
     */
    @Column(name = "platform_trade_no", length = 64)
    private String platformTradeNo;

    /**
     * 交易类型（如：JSAPI, NATIVE, APP, H5）
     */
    @Column(name = "trade_type", length = 32)
    private String tradeType;

    /**
     * 订单金额（单位：元）
     */
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * 已退款金额（单位：元）
     */
    @Column(name = "refunded_amount", precision = 10, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    /**
     * 交易状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    /**
     * 订单标题/商品描述
     */
    @Column(name = "subject", length = 256)
    private String subject;

    /**
     * 创建支付时的原始请求数据（JSON）
     */
    @Column(name = "raw_request", columnDefinition = "TEXT")
    private String rawRequest;

    /**
     * 创建支付时的原始响应数据（JSON）
     */
    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    /**
     * 回调通知原始数据（JSON）
     */
    @Column(name = "notify_data", columnDefinition = "TEXT")
    private String notifyData;

    /**
     * 支付成功时间
     */
    @Column(name = "paid_at")
    private java.time.LocalDateTime paidAt;

    /**
     * 客户端IP
     */
    @Column(name = "client_ip", length = 64)
    private String clientIp;

    /**
     * 商户ID（如有多商户场景）
     */
    @Column(name = "merchant_id", length = 64)
    private String merchantId;

    /**
     * 扩展字段（JSON格式，存储额外信息）
     */
    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;
}
