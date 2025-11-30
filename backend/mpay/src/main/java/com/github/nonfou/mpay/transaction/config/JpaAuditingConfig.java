package com.github.nonfou.mpay.transaction.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 审计配置
 * 启用 @CreatedDate 和 @LastModifiedDate 自动填充
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
