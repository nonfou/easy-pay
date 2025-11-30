package com.github.nonfou.mpay.transaction.dto;

import com.github.nonfou.mpay.transaction.enums.PaymentPlatform;
import com.github.nonfou.mpay.transaction.enums.RefundStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款详情响应
 */
@Data
public class RefundDetailResponse {

    /**
     * ID
     */
    private Long id;

    /**
     * 关联的交易ID
     */
    private Long transactionId;

    /**
     * 商户订单号
     */
    private String orderId;

    /**
     * 系统退款单号
     */
    private String refundNo;

    /**
     * 支付平台
     */
    private PaymentPlatform platform;

    /**
     * 平台退款单号
     */
    private String platformRefundNo;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 退款原因
     */
    private String refundReason;

    /**
     * 退款状态
     */
    private RefundStatus status;

    /**
     * 退款成功时间
     */
    private LocalDateTime refundedAt;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
