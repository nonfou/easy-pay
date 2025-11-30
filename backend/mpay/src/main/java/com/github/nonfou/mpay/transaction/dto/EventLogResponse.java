package com.github.nonfou.mpay.transaction.dto;

import com.github.nonfou.mpay.transaction.enums.PaymentEventType;
import com.github.nonfou.mpay.transaction.enums.PaymentPlatform;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 事件日志响应
 */
@Data
public class EventLogResponse {

    /**
     * ID
     */
    private Long id;

    /**
     * 商户订单号
     */
    private String orderId;

    /**
     * 交易ID
     */
    private Long transactionId;

    /**
     * 退款ID
     */
    private Long refundId;

    /**
     * 事件类型
     */
    private PaymentEventType eventType;

    /**
     * 支付平台
     */
    private PaymentPlatform platform;

    /**
     * 请求数据
     */
    private String requestData;

    /**
     * 响应数据
     */
    private String responseData;

    /**
     * 结果码
     */
    private String resultCode;

    /**
     * 结果消息
     */
    private String resultMessage;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
