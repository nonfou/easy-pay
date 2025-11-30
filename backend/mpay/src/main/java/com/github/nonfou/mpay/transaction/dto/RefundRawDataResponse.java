package com.github.nonfou.mpay.transaction.dto;

import lombok.Data;

/**
 * 退款原始数据响应
 */
@Data
public class RefundRawDataResponse {

    /**
     * ID
     */
    private Long id;

    /**
     * 系统退款单号
     */
    private String refundNo;

    /**
     * 商户订单号
     */
    private String orderId;

    /**
     * 原始请求数据
     */
    private String rawRequest;

    /**
     * 原始响应数据
     */
    private String rawResponse;

    /**
     * 回调通知数据
     */
    private String notifyData;
}
