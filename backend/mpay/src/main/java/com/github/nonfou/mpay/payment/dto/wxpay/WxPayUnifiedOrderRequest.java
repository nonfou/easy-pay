package com.github.nonfou.mpay.payment.dto.wxpay;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 微信支付统一下单请求
 */
@Data
@Accessors(chain = true)
public class WxPayUnifiedOrderRequest {

    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 订单金额 (单位: 元)
     */
    private BigDecimal totalFee;

    /**
     * 商品描述
     */
    private String body;

    /**
     * 客户端 IP
     */
    private String spbillCreateIp;

    /**
     * 交易类型: NATIVE(二维码), JSAPI(公众号), MWEB(H5)
     */
    private String tradeType;

    /**
     * 用户 openid (JSAPI 支付必传)
     */
    private String openid;

    /**
     * 场景信息 (H5 支付必传)
     */
    private String sceneInfo;
}
