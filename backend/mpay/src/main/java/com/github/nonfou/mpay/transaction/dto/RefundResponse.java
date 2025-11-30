package com.github.nonfou.mpay.transaction.dto;

import com.github.nonfou.mpay.transaction.enums.PaymentPlatform;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 退款响应
 */
@Data
@Builder
public class RefundResponse {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 系统退款单号
     */
    private String refundNo;

    /**
     * 平台退款单号
     */
    private String platformRefundNo;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 结果码
     */
    private String resultCode;

    /**
     * 结果消息
     */
    private String resultMessage;

    /**
     * 支付平台
     */
    private PaymentPlatform platform;
}
