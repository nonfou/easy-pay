package com.github.nonfou.mpay.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nonfou.mpay.payment.dto.alipay.AlipayCallbackDTO;
import com.github.nonfou.mpay.payment.dto.wxpay.WxPayCallbackDTO;
import com.github.nonfou.mpay.transaction.enums.PaymentEventType;
import com.github.nonfou.mpay.transaction.enums.PaymentPlatform;
import com.github.nonfou.mpay.transaction.event.PaymentEvent;
import com.github.nonfou.mpay.websocket.PaymentWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * 支付回调处理服务（简化版 - 无数据库依赖）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCallbackService {

    private final PaymentWebSocketHandler webSocketHandler;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * 处理支付宝回调
     */
    public boolean handleAlipayCallback(AlipayCallbackDTO callback) {
        String outTradeNo = callback.getOutTradeNo();
        log.info("处理支付宝回调: outTradeNo={}, tradeStatus={}", outTradeNo, callback.getTradeStatus());

        boolean success = callback.isPaySuccess();

        // 发布支付回调事件
        publishPaymentNotifyEvent(
                PaymentPlatform.ALIPAY,
                outTradeNo,
                callback.getTradeNo(),
                callback.getTradeNo(),
                callback.getTotalAmount(),
                callback.getSubject(),
                toJson(callback),
                success,
                callback.getTradeStatus(),
                success ? "支付成功" : "支付未成功"
        );

        // 检查支付状态
        if (!success) {
            log.info("支付宝回调: 支付未成功, outTradeNo={}, tradeStatus={}",
                    outTradeNo, callback.getTradeStatus());
            // 发送支付失败通知
            webSocketHandler.sendPaymentFailed(outTradeNo, callback.getTradeStatus());
            return true;
        }

        log.info("支付宝回调: 订单支付成功, outTradeNo={}, tradeNo={}", outTradeNo, callback.getTradeNo());

        // 发送 WebSocket 通知
        webSocketHandler.sendPaymentSuccess(outTradeNo, callback.getTradeNo());

        return true;
    }

    /**
     * 处理微信支付回调
     */
    public boolean handleWxPayCallback(WxPayCallbackDTO callback) {
        String outTradeNo = callback.getOutTradeNo();
        log.info("处理微信支付回调: outTradeNo={}, resultCode={}", outTradeNo, callback.getResultCode());

        boolean success = callback.isPaySuccess();

        // 发布支付回调事件
        publishPaymentNotifyEvent(
                PaymentPlatform.WXPAY,
                outTradeNo,
                outTradeNo, // 微信没有独立的 tradeNo，使用 outTradeNo
                callback.getTransactionId(),
                callback.getTotalFee(),
                null,
                toJson(callback),
                success,
                callback.getResultCode(),
                success ? "支付成功" : "支付未成功"
        );

        // 检查支付状态
        if (!success) {
            log.info("微信支付回调: 支付未成功, outTradeNo={}, resultCode={}",
                    outTradeNo, callback.getResultCode());
            // 发送支付失败通知
            webSocketHandler.sendPaymentFailed(outTradeNo, callback.getResultCode());
            return true;
        }

        log.info("微信支付回调: 订单支付成功, outTradeNo={}, transactionId={}",
                outTradeNo, callback.getTransactionId());

        // 发送 WebSocket 通知
        webSocketHandler.sendPaymentSuccess(outTradeNo, callback.getTransactionId());

        return true;
    }

    /**
     * 发布支付回调事件
     */
    private void publishPaymentNotifyEvent(PaymentPlatform platform,
                                           String orderId,
                                           String tradeNo,
                                           String platformTradeNo,
                                           java.math.BigDecimal amount,
                                           String subject,
                                           String responseData,
                                           boolean success,
                                           String resultCode,
                                           String resultMessage) {
        PaymentEvent event = PaymentEvent.builder()
                .source(this)
                .eventType(PaymentEventType.NOTIFY)
                .platform(platform)
                .orderId(orderId)
                .tradeNo(tradeNo)
                .platformTradeNo(platformTradeNo)
                .amount(amount)
                .subject(subject)
                .responseData(responseData)
                .success(success)
                .resultCode(resultCode)
                .resultMessage(resultMessage)
                .build();

        eventPublisher.publishEvent(event);
        log.debug("发布支付回调事件: platform={}, orderId={}, success={}", platform, orderId, success);
    }

    /**
     * 转换为 JSON
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }
}
