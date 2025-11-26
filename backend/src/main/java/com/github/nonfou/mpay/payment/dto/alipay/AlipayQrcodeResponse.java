package com.github.nonfou.mpay.payment.dto.alipay;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 支付宝二维码支付响应
 */
@Data
public class AlipayQrcodeResponse {

    /**
     * 返回状态码
     */
    private String code;

    /**
     * 返回信息
     */
    private String msg;

    /**
     * 商户订单号
     */
    @JsonProperty("out_trade_no")
    private String outTradeNo;

    /**
     * 二维码内容
     */
    @JsonProperty("qr_code")
    private String qrCode;

    /**
     * 错误码
     */
    @JsonProperty("sub_code")
    private String subCode;

    /**
     * 错误信息
     */
    @JsonProperty("sub_msg")
    private String subMsg;

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return "10000".equals(code);
    }
}
