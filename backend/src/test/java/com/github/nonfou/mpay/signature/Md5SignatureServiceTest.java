package com.github.nonfou.mpay.signature;

import com.github.nonfou.mpay.dto.PublicCreateOrderDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Md5SignatureService 单元测试
 * 测试 MD5 签名验证服务，包括防重放攻击
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Md5SignatureService 签名验证服务测试")
class Md5SignatureServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private Md5SignatureService signatureService;

    private static final String SECRET = "testSecretKey12345678";
    private static final Long PID = 1001L;

    @BeforeEach
    void setUp() {
        signatureService = new Md5SignatureService(redisTemplate);
    }

    @Nested
    @DisplayName("签名验证基础测试")
    class BasicVerifyTests {

        @Test
        @DisplayName("验证成功 - 正确签名")
        void verify_shouldReturnTrue_whenSignatureIsCorrect() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            String signString = SignatureUtils.buildSignString(request) + SECRET;
            String correctSign = SignatureUtils.md5(signString);
            request.setSign(correctSign);

            // When
            boolean result = signatureService.verify(request, SECRET);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("验证失败 - 签名错误")
        void verify_shouldReturnFalse_whenSignatureIsIncorrect() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            request.setSign("incorrectSignature123456789012");

            // When
            boolean result = signatureService.verify(request, SECRET);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("验证失败 - 签名为 null")
        void verify_shouldReturnFalse_whenSignatureIsNull() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            request.setSign(null);

            // When
            boolean result = signatureService.verify(request, SECRET);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("验证成功 - 签名大小写不敏感")
        void verify_shouldReturnTrue_whenSignatureCaseDiffers() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            String signString = SignatureUtils.buildSignString(request) + SECRET;
            String correctSign = SignatureUtils.md5(signString).toUpperCase();
            request.setSign(correctSign);

            // When
            boolean result = signatureService.verify(request, SECRET);

            // Then
            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("时间戳防重放测试")
    class TimestampValidationTests {

        @Test
        @DisplayName("验证成功 - 时间戳在有效期内")
        void verify_shouldReturnTrue_whenTimestampIsValid() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            request.setTimestamp(Instant.now().getEpochSecond());
            String signString = SignatureUtils.buildSignString(request) + SECRET;
            request.setSign(SignatureUtils.md5(signString));

            // When
            boolean result = signatureService.verify(request, SECRET);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("验证失败 - 时间戳过期（超过 5 分钟）")
        void verify_shouldReturnFalse_whenTimestampExpired() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            // 设置 6 分钟前的时间戳
            request.setTimestamp(Instant.now().getEpochSecond() - 360);
            String signString = SignatureUtils.buildSignString(request) + SECRET;
            request.setSign(SignatureUtils.md5(signString));

            // When
            boolean result = signatureService.verify(request, SECRET);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("验证失败 - 时间戳来自未来（超过 5 分钟）")
        void verify_shouldReturnFalse_whenTimestampFromFuture() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            // 设置 6 分钟后的时间戳
            request.setTimestamp(Instant.now().getEpochSecond() + 360);
            String signString = SignatureUtils.buildSignString(request) + SECRET;
            request.setSign(SignatureUtils.md5(signString));

            // When
            boolean result = signatureService.verify(request, SECRET);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("验证成功 - 无时间戳时跳过验证（兼容旧版本）")
        void verify_shouldReturnTrue_whenTimestampIsNull() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            request.setTimestamp(null);
            String signString = SignatureUtils.buildSignString(request) + SECRET;
            request.setSign(SignatureUtils.md5(signString));

            // When
            boolean result = signatureService.verify(request, SECRET);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("验证成功 - 时间戳在边界值（刚好 5 分钟）")
        void verify_shouldReturnTrue_whenTimestampAtBoundary() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            // 设置刚好 5 分钟前
            request.setTimestamp(Instant.now().getEpochSecond() - 300);
            String signString = SignatureUtils.buildSignString(request) + SECRET;
            request.setSign(SignatureUtils.md5(signString));

            // When
            boolean result = signatureService.verify(request, SECRET);

            // Then
            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Nonce 防重放测试")
    class NonceValidationTests {

        @Test
        @DisplayName("验证成功 - nonce 首次使用")
        void verify_shouldReturnTrue_whenNonceIsNew() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            request.setNonce("unique-nonce-123");
            request.setTimestamp(Instant.now().getEpochSecond());
            String signString = SignatureUtils.buildSignString(request) + SECRET;
            request.setSign(SignatureUtils.md5(signString));

            when(redisTemplate.hasKey("mpay:nonce:" + PID + ":unique-nonce-123")).thenReturn(false);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            // When
            boolean result = signatureService.verify(request, SECRET);

            // Then
            assertTrue(result);
            verify(valueOperations).set(eq("mpay:nonce:" + PID + ":unique-nonce-123"), eq("1"), eq(600L), any());
        }

        @Test
        @DisplayName("验证失败 - nonce 已被使用（重放攻击）")
        void verify_shouldReturnFalse_whenNonceIsReused() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            request.setNonce("used-nonce-456");
            request.setTimestamp(Instant.now().getEpochSecond());
            String signString = SignatureUtils.buildSignString(request) + SECRET;
            request.setSign(SignatureUtils.md5(signString));

            when(redisTemplate.hasKey("mpay:nonce:" + PID + ":used-nonce-456")).thenReturn(true);

            // When
            boolean result = signatureService.verify(request, SECRET);

            // Then
            assertFalse(result);
            verify(redisTemplate, never()).opsForValue();
        }

        @Test
        @DisplayName("验证成功 - 无 nonce 时跳过检查")
        void verify_shouldSkipNonceCheck_whenNonceIsNull() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            request.setNonce(null);
            request.setTimestamp(Instant.now().getEpochSecond());
            String signString = SignatureUtils.buildSignString(request) + SECRET;
            request.setSign(SignatureUtils.md5(signString));

            // When
            boolean result = signatureService.verify(request, SECRET);

            // Then
            assertTrue(result);
            verify(redisTemplate, never()).hasKey(anyString());
        }

        @Test
        @DisplayName("签名失败时不记录 nonce")
        void verify_shouldNotMarkNonce_whenSignatureFails() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            request.setNonce("some-nonce");
            request.setTimestamp(Instant.now().getEpochSecond());
            request.setSign("wrongSignature12345678901234");

            when(redisTemplate.hasKey(anyString())).thenReturn(false);

            // When
            boolean result = signatureService.verify(request, SECRET);

            // Then
            assertFalse(result);
            verify(redisTemplate, never()).opsForValue();
        }
    }

    @Nested
    @DisplayName("组合场景测试")
    class CombinedScenarioTests {

        @Test
        @DisplayName("完整防重放验证 - timestamp + nonce")
        void verify_shouldValidateBothTimestampAndNonce() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            request.setTimestamp(Instant.now().getEpochSecond());
            request.setNonce("full-protection-nonce");
            String signString = SignatureUtils.buildSignString(request) + SECRET;
            request.setSign(SignatureUtils.md5(signString));

            when(redisTemplate.hasKey(anyString())).thenReturn(false);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            // When
            boolean result = signatureService.verify(request, SECRET);

            // Then
            assertTrue(result);
            verify(redisTemplate).hasKey(contains("full-protection-nonce"));
            verify(valueOperations).set(anyString(), eq("1"), eq(600L), any());
        }

        @Test
        @DisplayName("时间戳过期时不检查 nonce")
        void verify_shouldNotCheckNonce_whenTimestampExpired() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            request.setTimestamp(Instant.now().getEpochSecond() - 400); // 过期
            request.setNonce("should-not-be-checked");
            String signString = SignatureUtils.buildSignString(request) + SECRET;
            request.setSign(SignatureUtils.md5(signString));

            // When
            boolean result = signatureService.verify(request, SECRET);

            // Then
            assertFalse(result);
            verify(redisTemplate, never()).hasKey(anyString());
        }

        @Test
        @DisplayName("参数篡改检测 - 修改金额")
        void verify_shouldDetectTampering_whenAmountChanged() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            String signString = SignatureUtils.buildSignString(request) + SECRET;
            String correctSign = SignatureUtils.md5(signString);
            request.setSign(correctSign);

            // 篡改金额
            request.setMoney(new BigDecimal("200.00"));

            // When
            boolean result = signatureService.verify(request, SECRET);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("参数篡改检测 - 修改订单号")
        void verify_shouldDetectTampering_whenOrderNoChanged() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            String signString = SignatureUtils.buildSignString(request) + SECRET;
            String correctSign = SignatureUtils.md5(signString);
            request.setSign(correctSign);

            // 篡改订单号
            request.setOutTradeNo("TAMPERED_ORDER");

            // When
            boolean result = signatureService.verify(request, SECRET);

            // Then
            assertFalse(result);
        }
    }

    // ==================== 辅助方法 ====================

    private PublicCreateOrderDTO createValidRequest() {
        PublicCreateOrderDTO request = new PublicCreateOrderDTO();
        request.setPid(PID);
        request.setType("wxpay");
        request.setOutTradeNo("ORDER123456");
        request.setName("测试商品");
        request.setMoney(new BigDecimal("99.99"));
        request.setNotifyUrl("http://example.com/notify");
        request.setClientIp("192.168.1.1");
        request.setDevice("mobile");
        return request;
    }
}
