package com.github.nonfou.mpay.payment.dto.alipay;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付宝回调参数
 */
@Data
public class AlipayCallbackDTO {

    /**
     * 订单创建时间
     */
    @JsonProperty("gmt_create")
    private LocalDateTime gmtCreate;

    /**
     * 支付时间
     */
    @JsonProperty("gmt_payment")
    private LocalDateTime gmtPayment;

    /**
     * 商家支付宝账号
     */
    @JsonProperty("seller_email")
    private String sellerEmail;

    /**
     * 订单标题
     */
    private String subject;

    /**
     * 商户订单号
     */
    @JsonProperty("out_trade_no")
    private String outTradeNo;

    /**
     * 买家支付宝用户号
     */
    @JsonProperty("buyer_id")
    private String buyerId;

    /**
     * 开票金额
     */
    @JsonProperty("invoice_amount")
    private BigDecimal invoiceAmount;

    /**
     * 交易状态: TRADE_SUCCESS(支付成功), TRADE_CLOSED(交易关闭), TRADE_FINISHED(交易完成)
     */
    @JsonProperty("trade_status")
    private String tradeStatus;

    /**
     * 买家支付宝账号
     */
    @JsonProperty("buyer_logon_id")
    private String buyerLogonId;

    /**
     * 支付宝交易号
     */
    @JsonProperty("trade_no")
    private String tradeNo;

    /**
     * 应用ID
     */
    @JsonProperty("app_id")
    private String appId;

    /**
     * 订单总金额
     */
    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    /**
     * 实收金额
     */
    @JsonProperty("receipt_amount")
    private BigDecimal receiptAmount;

    /**
     * 是否支付成功
     */
    public boolean isPaySuccess() {
        return "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
    }
}
