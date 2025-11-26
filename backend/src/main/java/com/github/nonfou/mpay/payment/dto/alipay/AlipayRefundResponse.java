package com.github.nonfou.mpay.payment.dto.alipay;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付宝退款响应
 */
@Data
public class AlipayRefundResponse {

    /**
     * 返回状态码
     */
    private String code;

    /**
     * 返回信息
     */
    private String msg;

    /**
     * 支付宝交易号
     */
    @JsonProperty("trade_no")
    private String tradeNo;

    /**
     * 商户订单号
     */
    @JsonProperty("out_trade_no")
    private String outTradeNo;

    /**
     * 买家支付宝用户号
     */
    @JsonProperty("buyer_user_id")
    private String buyerUserId;

    /**
     * 退款金额
     */
    @JsonProperty("refund_fee")
    private BigDecimal refundFee;

    /**
     * 本次退款请求对应的退款金额
     */
    @JsonProperty("send_back_fee")
    private BigDecimal sendBackFee;

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
