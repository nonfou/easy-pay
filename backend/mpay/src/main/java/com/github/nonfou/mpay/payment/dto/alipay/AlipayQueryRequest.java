package com.github.nonfou.mpay.payment.dto.alipay;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 支付宝订单查询请求
 */
@Data
@Accessors(chain = true)
public class AlipayQueryRequest {

    /**
     * 商户订单号 (与支付宝交易号二选一)
     */
    private String outTradeNo;

    /**
     * 支付宝交易号 (与商户订单号二选一)
     */
    private String tradeNo;
}
