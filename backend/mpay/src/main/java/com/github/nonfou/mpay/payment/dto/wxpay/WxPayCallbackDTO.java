package com.github.nonfou.mpay.payment.dto.wxpay;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 微信支付回调参数
 */
@Data
public class WxPayCallbackDTO {

    /**
     * 返回状态码
     */
    private String returnCode;

    /**
     * 业务结果
     */
    private String resultCode;

    /**
     * 公众号 ID
     */
    private String appId;

    /**
     * 商户号
     */
    private String mchId;

    /**
     * 随机字符串
     */
    private String nonceStr;

    /**
     * 签名
     */
    private String sign;

    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 微信支付订单号
     */
    private String transactionId;

    /**
     * 订单金额 (单位: 元)
     */
    private BigDecimal totalFee;

    /**
     * 支付完成时间
     */
    private String timeEnd;

    /**
     * 签名验证结果
     */
    private boolean signValid;

    /**
     * 是否支付成功
     */
    public boolean isPaySuccess() {
        return "SUCCESS".equals(returnCode) && "SUCCESS".equals(resultCode) && signValid;
    }
}
