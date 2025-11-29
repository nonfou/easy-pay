package com.github.nonfou.mpay.payment.dto.alipay;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 支付宝订单查询响应
 */
@Data
@Accessors(chain = true)
public class AlipayQueryResponse {

    /**
     * 响应码 (10000 表示成功)
     */
    private String code;

    /**
     * 响应信息
     */
    private String msg;

    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 支付宝交易号
     */
    private String tradeNo;

    /**
     * 交易状态
     * WAIT_BUYER_PAY: 等待买家付款
     * TRADE_CLOSED: 交易关闭
     * TRADE_SUCCESS: 交易成功
     * TRADE_FINISHED: 交易完成
     */
    private String tradeStatus;

    /**
     * 订单金额
     */
    private BigDecimal totalAmount;

    /**
     * 实收金额
     */
    private BigDecimal receiptAmount;

    /**
     * 买家支付宝用户号
     */
    private String buyerUserId;

    /**
     * 买家支付宝账号
     */
    private String buyerLogonId;

    /**
     * 订单标题
     */
    private String subject;

    /**
     * 错误码
     */
    private String subCode;

    /**
     * 错误信息
     */
    private String subMsg;

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return "10000".equals(code);
    }
}
