package com.github.nonfou.mpay.payment.dto.alipay;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 支付宝退款请求
 */
@Data
@Accessors(chain = true)
public class AlipayRefundRequest {

    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 支付宝交易号 (与商户订单号二选一)
     */
    private String tradeNo;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 退款原因
     */
    private String refundReason;

    /**
     * 退款请求号 (部分退款时必传)
     */
    private String outRequestNo;
}
