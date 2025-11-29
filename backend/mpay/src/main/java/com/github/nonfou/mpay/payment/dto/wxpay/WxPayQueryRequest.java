package com.github.nonfou.mpay.payment.dto.wxpay;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 微信订单查询请求
 */
@Data
@Accessors(chain = true)
public class WxPayQueryRequest {

    /**
     * 商户订单号 (与微信支付订单号二选一)
     */
    private String outTradeNo;

    /**
     * 微信支付订单号 (与商户订单号二选一)
     */
    private String transactionId;
}
