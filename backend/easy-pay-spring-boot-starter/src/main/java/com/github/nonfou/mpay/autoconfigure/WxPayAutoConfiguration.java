package com.github.nonfou.mpay.autoconfigure;

import com.github.binarywang.wxpay.service.WxPayService;
import com.github.nonfou.mpay.payment.config.WxPayConfiguration;
import com.github.nonfou.mpay.payment.service.WxPayServiceWrapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

/**
 * 微信支付自动配置
 * <p>
 * 当配置了 easy-pay.wxpay.app-id 时自动启用
 */
@AutoConfiguration
@ConditionalOnClass(WxPayService.class)
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "easy-pay.wxpay", name = "app-id")
@Import({
        WxPayConfiguration.class,
        WxPayServiceWrapper.class
})
public class WxPayAutoConfiguration {
}
