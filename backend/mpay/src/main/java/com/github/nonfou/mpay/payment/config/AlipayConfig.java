package com.github.nonfou.mpay.payment.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.github.nonfou.mpay.payment.properties.AlipayProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝配置类
 */
@Configuration
@EnableConfigurationProperties(AlipayProperties.class)
@ConditionalOnProperty(prefix = "easy-pay.alipay", name = "app-id")
public class AlipayConfig {

    @Bean
    public AlipayClient alipayClient(AlipayProperties properties) {
        return new DefaultAlipayClient(
                properties.getGatewayUrl(),
                properties.getAppId(),
                properties.getPrivateKey(),
                "JSON",
                properties.getCharset(),
                properties.getPublicKey(),
                properties.getSignType()
        );
    }
}
