package com.github.nonfou.mpay.payment.dto.wxpay;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 微信订单查询响应
 */
@Data
@Accessors(chain = true)
public class WxPayQueryResponse {

    /**
     * 返回状态码 (SUCCESS/FAIL)
     */
    private String returnCode;

    /**
     * 返回信息
     */
    private String returnMsg;

    /**
     * 业务结果 (SUCCESS/FAIL)
     */
    private String resultCode;

    /**
     * 错误代码
     */
    private String errCode;

    /**
     * 错误代码描述
     */
    private String errCodeDes;

    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 微��支付订单号
     */
    private String transactionId;

    /**
     * 交易类型
     */
    private String tradeType;

    /**
     * 交易状态
     * SUCCESS: 支付成功
     * REFUND: 转入退款
     * NOTPAY: 未支付
     * CLOSED: 已关闭
     * REVOKED: 已撤销
     * USERPAYING: 用户支付中
     * PAYERROR: 支付失败
     */
    private String tradeState;

    /**
     * 交易状态描述
     */
    private String tradeStateDesc;

    /**
     * 订单金额 (单位: 分)
     */
    private Integer totalFee;

    /**
     * 实收金额 (单位: 分)
     */
    private Integer cashFee;

    /**
     * 用户标识
     */
    private String openid;

    /**
     * 商品描述
     */
    private String body;

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(returnCode) && "SUCCESS".equals(resultCode);
    }
}
