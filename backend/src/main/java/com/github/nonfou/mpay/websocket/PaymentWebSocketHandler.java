package com.github.nonfou.mpay.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支付结果 WebSocket 处理器
 */
@Slf4j
@Component
public class PaymentWebSocketHandler extends TextWebSocketHandler {

    /**
     * 存储订单ID与WebSocket会话的映射
     */
    private static final Map<String, WebSocketSession> ORDER_SESSIONS = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String orderId = getOrderId(session);
        if (orderId != null) {
            ORDER_SESSIONS.put(orderId, session);
            log.info("WebSocket 连接建立: orderId={}, sessionId={}", orderId, session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String orderId = getOrderId(session);
        if (orderId != null) {
            ORDER_SESSIONS.remove(orderId);
            log.info("WebSocket 连接关闭: orderId={}, sessionId={}, status={}",
                    orderId, session.getId(), status);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 客户端发来的消息，可以用于心跳检测
        log.debug("收到 WebSocket 消息: sessionId={}, payload={}",
                session.getId(), message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String orderId = getOrderId(session);
        log.error("WebSocket 传输错误: orderId={}, sessionId={}, error={}",
                orderId, session.getId(), exception.getMessage());
    }

    /**
     * 向指定订单的客户端发送消息
     *
     * @param orderId 订单号
     * @param message 消息内容
     * @return 是否发送成功
     */
    public boolean sendMessage(String orderId, String message) {
        WebSocketSession session = ORDER_SESSIONS.get(orderId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                log.info("WebSocket 消息发送成功: orderId={}, message={}", orderId, message);
                return true;
            } catch (IOException e) {
                log.error("WebSocket 消息发送失败: orderId={}, error={}", orderId, e.getMessage());
                ORDER_SESSIONS.remove(orderId);
            }
        } else {
            // PC/H5跳转支付场景下，用户离开页面后WebSocket会断开，这是正常情况
            log.debug("WebSocket 会话不存在或已关闭: orderId={}", orderId);
        }
        return false;
    }

    /**
     * 向指定订单的客户端发送支付成功通知
     *
     * @param orderId 订单号
     * @param tradeNo 平台交易号
     * @return 是否发送成功
     */
    public boolean sendPaymentSuccess(String orderId, String tradeNo) {
        String message = String.format(
                "{\"type\":\"PAYMENT_SUCCESS\",\"orderId\":\"%s\",\"tradeNo\":\"%s\"}",
                orderId, tradeNo
        );
        return sendMessage(orderId, message);
    }

    /**
     * 向指定订单的客户端发送支付失败通知
     *
     * @param orderId 订单号
     * @param reason  失败原因
     * @return 是否发送成功
     */
    public boolean sendPaymentFailed(String orderId, String reason) {
        String message = String.format(
                "{\"type\":\"PAYMENT_FAILED\",\"orderId\":\"%s\",\"reason\":\"%s\"}",
                orderId, reason
        );
        return sendMessage(orderId, message);
    }

    /**
     * 检查订单是否有活跃的 WebSocket 连接
     *
     * @param orderId 订单号
     * @return 是否有活跃连接
     */
    public boolean hasActiveConnection(String orderId) {
        WebSocketSession session = ORDER_SESSIONS.get(orderId);
        return session != null && session.isOpen();
    }

    /**
     * 获取当前活跃连接数
     *
     * @return 活跃连接数
     */
    public int getActiveConnectionCount() {
        return (int) ORDER_SESSIONS.values().stream()
                .filter(WebSocketSession::isOpen)
                .count();
    }

    /**
     * 从 WebSocket 会话中提取订单ID
     */
    private String getOrderId(WebSocketSession session) {
        String path = session.getUri() != null ? session.getUri().getPath() : null;
        if (path != null && path.startsWith("/ws/payment/")) {
            return path.substring("/ws/payment/".length());
        }
        return null;
    }
}
