package com.github.nonfou.mpay.payment.dto.wxpay;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 微信支付退款响应
 */
@Data
public class WxPayRefundResponse {

    /**
     * 返回状态码
     */
    private String returnCode;

    /**
     * 返回信息
     */
    private String returnMsg;

    /**
     * 业务结果
     */
    private String resultCode;

    /**
     * 错误代码
     */
    private String errCode;

    /**
     * 错误描述
     */
    private String errCodeDes;

    /**
     * 微信订单号
     */
    private String transactionId;

    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 商户退款单号
     */
    private String outRefundNo;

    /**
     * 微信退款单号
     */
    private String refundId;

    /**
     * 退款金额 (单位: 元)
     */
    private BigDecimal refundFee;

    /**
     * 订单金额 (单位: 元)
     */
    private BigDecimal totalFee;

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(returnCode) && "SUCCESS".equals(resultCode);
    }
}
