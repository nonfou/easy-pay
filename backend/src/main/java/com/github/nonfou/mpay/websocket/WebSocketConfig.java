package com.github.nonfou.mpay.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final PaymentWebSocketHandler paymentWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册支付结果通知的 WebSocket 端点
        // 路径格式: /ws/payment/{orderId}
        registry.addHandler(paymentWebSocketHandler, "/ws/payment/*")
                .setAllowedOrigins("*"); // 生产环境应该限制具体域名
    }
}
