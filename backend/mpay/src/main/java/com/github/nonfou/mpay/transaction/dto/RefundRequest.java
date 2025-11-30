package com.github.nonfou.mpay.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 退款请求
 */
@Data
public class RefundRequest {

    /**
     * 系统交易号（三选一）
     */
    private String tradeNo;

    /**
     * 商户订单号（三选一）
     */
    private String orderId;

    /**
     * 平台交易号（三选一）
     */
    private String platformTradeNo;

    /**
     * 退款金额
     */
    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于0")
    private BigDecimal refundAmount;

    /**
     * 退款原因
     */
    private String refundReason;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 客户端IP
     */
    private String clientIp;
}
