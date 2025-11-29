package com.github.nonfou.mpay.payment.config;

import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;
import com.github.nonfou.mpay.payment.properties.WxPayProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信支付配置类
 */
@Configuration
@EnableConfigurationProperties(WxPayProperties.class)
@ConditionalOnProperty(prefix = "easy-pay.wxpay", name = "app-id")
public class WxPayConfiguration {

    @Bean
    public WxPayService wxPayService(WxPayProperties properties) {
        WxPayConfig config = new WxPayConfig();
        config.setAppId(properties.getAppId());
        config.setMchId(properties.getMchId());
        config.setMchKey(properties.getMchKey());
        config.setKeyPath(properties.getCertPath());
        config.setTradeType(properties.getTradeType());
        config.setNotifyUrl(properties.getPayNotifyUrl());

        WxPayService wxPayService = new WxPayServiceImpl();
        wxPayService.setConfig(config);
        return wxPayService;
    }
}
