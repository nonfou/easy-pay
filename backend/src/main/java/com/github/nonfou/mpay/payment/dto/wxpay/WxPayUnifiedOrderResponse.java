package com.github.nonfou.mpay.payment.dto.wxpay;

import lombok.Data;

/**
 * 微信支付统一下单响应
 */
@Data
public class WxPayUnifiedOrderResponse {

    /**
     * 返回状态码: SUCCESS/FAIL
     */
    private String returnCode;

    /**
     * 返回信息
     */
    private String returnMsg;

    /**
     * 业务结果: SUCCESS/FAIL
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
     * 交易类型
     */
    private String tradeType;

    /**
     * 预支付交易会话标识
     */
    private String prepayId;

    /**
     * 二维码链接 (trade_type=NATIVE 时返回)
     */
    private String codeUrl;

    /**
     * H5 支付跳转链接 (trade_type=MWEB 时返回)
     */
    private String mwebUrl;

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(returnCode) && "SUCCESS".equals(resultCode);
    }
}
