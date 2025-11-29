package com.github.nonfou.mpay.payment.dto.alipay;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 支付宝 PC 网站支付请求
 */
@Data
@Accessors(chain = true)
public class AlipayPcPayRequest {

    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 订单金额 (单位: 元)
     */
    private BigDecimal totalAmount;

    /**
     * 订单标题
     */
    private String subject;

    /**
     * 商品描述
     */
    private String body;

    /**
     * 过期时间
     */
    private String timeoutExpress = "30m";
}
