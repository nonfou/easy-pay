package com.github.nonfou.mpay.payment.dto.wxpay;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 微信支付退款请求
 */
@Data
@Accessors(chain = true)
public class WxPayRefundRequest {

    /**
     * 商户订单号 (与微信订单号二选一)
     */
    private String outTradeNo;

    /**
     * 微信订单号
     */
    private String transactionId;

    /**
     * 商户退款单号
     */
    private String outRefundNo;

    /**
     * 订单金额 (单位: 元)
     */
    private BigDecimal totalFee;

    /**
     * 退款金额 (单位: 元)
     */
    private BigDecimal refundFee;

    /**
     * 退款原因
     */
    private String refundDesc;
}
