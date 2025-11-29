package com.github.nonfou.mpay.payment.dto.alipay;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 支付宝 H5 支付请求
 */
@Data
@Accessors(chain = true)
public class AlipayH5PayRequest {

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

    /**
     * 用户付款中途退出返回商户网站的地址
     */
    private String quitUrl;
}
