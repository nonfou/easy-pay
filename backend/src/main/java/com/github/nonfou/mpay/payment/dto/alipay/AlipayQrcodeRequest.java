package com.github.nonfou.mpay.payment.dto.alipay;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 支付宝二维码支付请求
 */
@Data
@Accessors(chain = true)
public class AlipayQrcodeRequest {

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
     * 过期时间，默认 30 分钟
     */
    private String timeoutExpress = "30m";
}
