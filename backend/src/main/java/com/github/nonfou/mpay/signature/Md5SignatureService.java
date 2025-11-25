package com.github.nonfou.mpay.signature;

import com.github.nonfou.mpay.dto.PublicCreateOrderDTO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class Md5SignatureService implements SignatureService {

    private static final Logger log = LoggerFactory.getLogger(Md5SignatureService.class);

    /** 签名有效期：5分钟 */
    private static final long TIMESTAMP_TOLERANCE_SECONDS = 300;

    /** Nonce 缓存过期时间：10分钟（大于 TIMESTAMP_TOLERANCE 以确保重放检测有效） */
    private static final long NONCE_EXPIRE_SECONDS = 600;

    private static final String NONCE_KEY_PREFIX = "mpay:nonce:";

    private final StringRedisTemplate redisTemplate;

    public Md5SignatureService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean verify(PublicCreateOrderDTO request, String secret) {
        // 1. 验证时间戳防重放
        if (!validateTimestamp(request.getTimestamp())) {
            log.warn("签名验证失败-时间戳过期: pid={}, timestamp={}", request.getPid(), request.getTimestamp());
            return false;
        }

        // 2. 验证 nonce 防重放（如果提供）
        if (request.getNonce() != null && !validateNonce(request.getPid(), request.getNonce())) {
            log.warn("签名验证失败-nonce重复: pid={}, nonce={}", request.getPid(), request.getNonce());
            return false;
        }

        // 3. 构建签名并验证
        String signString = SignatureUtils.buildSignString(request) + secret;
        String expected = SignatureUtils.md5(signString);

        // 使用常量时间比较防止时序攻击
        boolean match = constantTimeEquals(expected, request.getSign());
        if (!match) {
            log.warn("签名验证失败-签名不匹配: pid={}", request.getPid());
        } else if (request.getNonce() != null) {
            // 签名验证通过后，记录 nonce 防止重放
            markNonceUsed(request.getPid(), request.getNonce());
        }
        return match;
    }

    /**
     * 验证时间戳是否在有效范围内
     */
    private boolean validateTimestamp(Long timestamp) {
        if (timestamp == null) {
            // 兼容旧版本：如果没有 timestamp 则跳过验证
            log.debug("请求未包含 timestamp，跳过时间戳验证");
            return true;
        }
        long now = Instant.now().getEpochSecond();
        long diff = Math.abs(now - timestamp);
        return diff <= TIMESTAMP_TOLERANCE_SECONDS;
    }

    /**
     * 验证 nonce 是否已被使用
     */
    private boolean validateNonce(Long pid, String nonce) {
        String key = NONCE_KEY_PREFIX + pid + ":" + nonce;
        return !Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 标记 nonce 已使用
     */
    private void markNonceUsed(Long pid, String nonce) {
        String key = NONCE_KEY_PREFIX + pid + ":" + nonce;
        redisTemplate.opsForValue().set(key, "1", NONCE_EXPIRE_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 常量时间字符串比较，防止时序攻击
     */
    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        byte[] expectedBytes = expected.toLowerCase().getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }
}
