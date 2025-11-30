package com.github.nonfou.mpay.transaction.event;

import com.github.nonfou.mpay.transaction.enums.PaymentEventType;
import com.github.nonfou.mpay.transaction.enums.PaymentPlatform;
import lombok.Builder;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

/**
 * 退款事件 - 用于事件驱动架构
 */
@Getter
public class RefundEvent extends ApplicationEvent {

    /**
     * 事件类型（REFUND_CREATE / REFUND_NOTIFY / REFUND_QUERY）
     */
    private final PaymentEventType eventType;

    /**
     * 支付平台
     */
    private final PaymentPlatform platform;

    /**
     * 商户订单号
     */
    private final String orderId;

    /**
     * 关联的交易ID
     */
    private final Long transactionId;

    /**
     * 系统退款单号
     */
    private final String refundNo;

    /**
     * 平台退款单号
     */
    private final String platformRefundNo;

    /**
     * 原支付平台交易号
     */
    private final String platformTradeNo;

    /**
     * 退款金额
     */
    private final BigDecimal refundAmount;

    /**
     * 退款原因
     */
    private final String refundReason;

    /**
     * 请求数据（JSON）
     */
    private final String requestData;

    /**
     * 响应数据（JSON）
     */
    private final String responseData;

    /**
     * 是否成功
     */
    private final Boolean success;

    /**
     * 结果码
     */
    private final String resultCode;

    /**
     * 结果消息
     */
    private final String resultMessage;

    /**
     * 耗时（毫秒）
     */
    private final Long durationMs;

    /**
     * 客户端IP
     */
    private final String clientIp;

    /**
     * 操作人
     */
    private final String operator;

    /**
     * 扩展数据
     */
    private final String extraData;

    @Builder
    public RefundEvent(Object source,
                       PaymentEventType eventType,
                       PaymentPlatform platform,
                       String orderId,
                       Long transactionId,
                       String refundNo,
                       String platformRefundNo,
                       String platformTradeNo,
                       BigDecimal refundAmount,
                       String refundReason,
                       String requestData,
                       String responseData,
                       Boolean success,
                       String resultCode,
                       String resultMessage,
                       Long durationMs,
                       String clientIp,
                       String operator,
                       String extraData) {
        super(source);
        this.eventType = eventType;
        this.platform = platform;
        this.orderId = orderId;
        this.transactionId = transactionId;
        this.refundNo = refundNo;
        this.platformRefundNo = platformRefundNo;
        this.platformTradeNo = platformTradeNo;
        this.refundAmount = refundAmount;
        this.refundReason = refundReason;
        this.requestData = requestData;
        this.responseData = responseData;
        this.success = success;
        this.resultCode = resultCode;
        this.resultMessage = resultMessage;
        this.durationMs = durationMs;
        this.clientIp = clientIp;
        this.operator = operator;
        this.extraData = extraData;
    }
}
