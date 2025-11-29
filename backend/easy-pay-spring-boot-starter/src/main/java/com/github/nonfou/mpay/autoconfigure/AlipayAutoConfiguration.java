package com.github.nonfou.mpay.autoconfigure;

import com.alipay.api.AlipayClient;
import com.github.nonfou.mpay.controller.PaymentController;
import com.github.nonfou.mpay.payment.config.AlipayConfig;
import com.github.nonfou.mpay.payment.service.AlipayService;
import com.github.nonfou.mpay.payment.service.PaymentCallbackService;
import com.github.nonfou.mpay.websocket.PaymentWebSocketHandler;
import com.github.nonfou.mpay.websocket.WebSocketConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

/**
 * 支付宝自动配置
 * <p>
 * 当配置了 easy-pay.alipay.app-id 时自动启用
 */
@AutoConfiguration
@ConditionalOnClass(AlipayClient.class)
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "easy-pay.alipay", name = "app-id")
@Import({
        AlipayConfig.class,
        AlipayService.class,
        PaymentWebSocketHandler.class,
        WebSocketConfig.class,
        PaymentCallbackService.class,
        PaymentController.class
})
public class AlipayAutoConfiguration {
}
