package com.github.nonfou.mpay.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Easy Pay 主自动配置类
 * <p>
 * 自动导入支付宝和微信支付的配置
 */
@AutoConfiguration
@Import({
        AlipayAutoConfiguration.class,
        WxPayAutoConfiguration.class
})
public class EasyPayAutoConfiguration {
}
