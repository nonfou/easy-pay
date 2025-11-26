package com.github.nonfou.mpay.payment.service;

import com.github.nonfou.mpay.payment.dto.alipay.AlipayCallbackDTO;
import com.github.nonfou.mpay.payment.dto.wxpay.WxPayCallbackDTO;
import com.github.nonfou.mpay.websocket.PaymentWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 支付回调处理服务（简化版 - 无数据库依赖）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCallbackService {

    private final PaymentWebSocketHandler webSocketHandler;

    /**
     * 处理支付宝回调
     */
    public boolean handleAlipayCallback(AlipayCallbackDTO callback) {
        String outTradeNo = callback.getOutTradeNo();
        log.info("处理支付宝回调: outTradeNo={}, tradeStatus={}", outTradeNo, callback.getTradeStatus());

        // 检查支付状态
        if (!callback.isPaySuccess()) {
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

        // 检查支付状态
        if (!callback.isPaySuccess()) {
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
}
