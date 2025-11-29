package com.github.nonfou.mpay.payment.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 支付宝配置属性
 */
@Data
@ConfigurationProperties(prefix = "easy-pay.alipay")
public class AlipayProperties {

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 商户私钥
     */
    private String privateKey;

    /**
     * 支付宝公钥
     */
    private String publicKey;

    /**
     * 异步回调地址
     */
    private String notifyUrl;

    /**
     * 同步回调页面
     */
    private String returnUrl;

    /**
     * 签名类型
     */
    private String signType = "RSA2";

    /**
     * 字符编码
     */
    private String charset = "utf-8";

    /**
     * 网关地址
     */
    private String gatewayUrl = "https://openapi.alipay.com/gateway.do";
}
