package com.github.nonfou.mpay.transaction.dto;

import com.github.nonfou.mpay.transaction.enums.PaymentPlatform;
import com.github.nonfou.mpay.transaction.enums.TransactionStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易详情响应
 */
@Data
public class TransactionDetailResponse {

    /**
     * ID
     */
    private Long id;

    /**
     * 商户订单号
     */
    private String orderId;

    /**
     * 支付平台
     */
    private PaymentPlatform platform;

    /**
     * 系统交易号
     */
    private String tradeNo;

    /**
     * 平台交易号
     */
    private String platformTradeNo;

    /**
     * 交易类型
     */
    private String tradeType;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 已退款金额
     */
    private BigDecimal refundedAmount;

    /**
     * 交易状态
     */
    private TransactionStatus status;

    /**
     * 商品描述
     */
    private String subject;

    /**
     * 商户ID
     */
    private String merchantId;

    /**
     * 支付成功时间
     */
    private LocalDateTime paidAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
