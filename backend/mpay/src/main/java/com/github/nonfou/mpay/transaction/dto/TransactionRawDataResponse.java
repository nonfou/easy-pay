package com.github.nonfou.mpay.transaction.dto;

import lombok.Data;

/**
 * 交易原始数据响应
 */
@Data
public class TransactionRawDataResponse {

    /**
     * ID
     */
    private Long id;

    /**
     * 商户订单号
     */
    private String orderId;

    /**
     * 系统交易号
     */
    private String tradeNo;

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
