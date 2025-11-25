package com.github.nonfou.mpay.ratelimit;

import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 基于 Redis 的滑动窗口限流器
 */
@Component
public class RateLimiter {

    private static final String KEY_PREFIX = "mpay:ratelimit:";

    private final StringRedisTemplate redisTemplate;

    public RateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 检查是否允许请求
     *
     * @param key      限流键（如 IP、用户ID 等）
     * @param limit    时间窗口内允许的最大请求数
     * @param windowSeconds 时间窗口（秒）
     * @return true 表示允许请求，false 表示被限流
     */
    public boolean isAllowed(String key, int limit, int windowSeconds) {
        String redisKey = KEY_PREFIX + key;
        Long currentCount = redisTemplate.opsForValue().increment(redisKey);

        if (currentCount == null) {
            return false;
        }

        if (currentCount == 1) {
            // 第一次访问，设置过期时间
            redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
        }

        return currentCount <= limit;
    }

    /**
     * 获取剩余请求次数
     */
    public long getRemainingRequests(String key, int limit) {
        String redisKey = KEY_PREFIX + key;
        String value = redisTemplate.opsForValue().get(redisKey);
        if (value == null) {
            return limit;
        }
        return Math.max(0, limit - Long.parseLong(value));
    }
}
