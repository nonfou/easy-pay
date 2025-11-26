package com.github.nonfou.mpay.payment.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 微信支付配置属性
 */
@Data
@ConfigurationProperties(prefix = "mpay.wxpay")
public class WxPayProperties {

    /**
     * 公众号/小程序 appId
     */
    private String appId;

    /**
     * 商户号
     */
    private String mchId;

    /**
     * 支付API安全密钥
     */
    private String mchKey;

    /**
     * 交易类型: NATIVE(二维码), JSAPI(公众号), MWEB(H5)
     */
    private String tradeType = "NATIVE";

    /**
     * 支付结果回调地址
     */
    private String payNotifyUrl;

    /**
     * 退款回调地址
     */
    private String refundNotifyUrl;

    /**
     * 退款证书路径
     */
    private String certPath;
}
