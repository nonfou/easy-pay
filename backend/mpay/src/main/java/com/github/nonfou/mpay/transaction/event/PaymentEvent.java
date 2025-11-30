package com.github.nonfou.mpay.transaction.event;

import com.github.nonfou.mpay.transaction.enums.PaymentEventType;
import com.github.nonfou.mpay.transaction.enums.PaymentPlatform;
import lombok.Builder;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

/**
 * 支付事件 - 用于事件驱动架构
 */
@Getter
public class PaymentEvent extends ApplicationEvent {

    /**
     * 事件类型
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
     * 系统交易号
     */
    private final String tradeNo;

    /**
     * 平台交易号
     */
    private final String platformTradeNo;

    /**
     * 交易类型
     */
    private final String tradeType;

    /**
     * 金额
     */
    private final BigDecimal amount;

    /**
     * 商品描述
     */
    private final String subject;

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
     * 商户ID
     */
    private final String merchantId;

    /**
     * 扩展数据
     */
    private final String extraData;

    @Builder
    public PaymentEvent(Object source,
                        PaymentEventType eventType,
                        PaymentPlatform platform,
                        String orderId,
                        String tradeNo,
                        String platformTradeNo,
                        String tradeType,
                        BigDecimal amount,
                        String subject,
                        String requestData,
                        String responseData,
                        Boolean success,
                        String resultCode,
                        String resultMessage,
                        Long durationMs,
                        String clientIp,
                        String merchantId,
                        String extraData) {
        super(source);
        this.eventType = eventType;
        this.platform = platform;
        this.orderId = orderId;
        this.tradeNo = tradeNo;
        this.platformTradeNo = platformTradeNo;
        this.tradeType = tradeType;
        this.amount = amount;
        this.subject = subject;
        this.requestData = requestData;
        this.responseData = responseData;
        this.success = success;
        this.resultCode = resultCode;
        this.resultMessage = resultMessage;
        this.durationMs = durationMs;
        this.clientIp = clientIp;
        this.merchantId = merchantId;
        this.extraData = extraData;
    }
}
