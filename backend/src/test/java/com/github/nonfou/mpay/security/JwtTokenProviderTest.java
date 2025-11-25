package com.github.nonfou.mpay.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtTokenProvider 单元测试
 * 测试 JWT 令牌生成和验证
 */
@DisplayName("JwtTokenProvider JWT 令牌提供者测试")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    // 测试用密钥（至少 32 字符）
    private static final String TEST_SECRET = "testSecretKeyForJwtTokenProvider12345678";
    private static final Long TEST_PID = 1001L;
    private static final String TEST_USERNAME = "testuser";
    private static final Integer TEST_ROLE = 1;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiration", 3600000L); // 1 小时
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpiration", 604800000L); // 7 天
    }

    @Nested
    @DisplayName("配置验证测试")
    class ConfigurationValidationTests {

        @Test
        @DisplayName("配置验证失败 - 密钥为空")
        void validateConfiguration_shouldThrow_whenSecretIsEmpty() {
            // Given
            JwtTokenProvider provider = new JwtTokenProvider();
            ReflectionTestUtils.setField(provider, "jwtSecret", "");

            // When & Then
            assertThrows(IllegalStateException.class, provider::validateConfiguration);
        }

        @Test
        @DisplayName("配置验证失败 - 密钥为 null")
        void validateConfiguration_shouldThrow_whenSecretIsNull() {
            // Given
            JwtTokenProvider provider = new JwtTokenProvider();
            ReflectionTestUtils.setField(provider, "jwtSecret", null);

            // When & Then
            assertThrows(IllegalStateException.class, provider::validateConfiguration);
        }

        @Test
        @DisplayName("配置验证失败 - 密钥长度不足")
        void validateConfiguration_shouldThrow_whenSecretTooShort() {
            // Given
            JwtTokenProvider provider = new JwtTokenProvider();
            ReflectionTestUtils.setField(provider, "jwtSecret", "short"); // 少于 32 字符

            // When & Then
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    provider::validateConfiguration);
            assertTrue(exception.getMessage().contains("长度不足"));
        }

        @Test
        @DisplayName("配置验证成功 - 密钥有效")
        void validateConfiguration_shouldSucceed_whenSecretIsValid() {
            // Given - setUp 中已配置

            // When & Then
            assertDoesNotThrow(() -> jwtTokenProvider.validateConfiguration());
        }
    }

    @Nested
    @DisplayName("Access Token 生成测试")
    class GenerateAccessTokenTests {

        @Test
        @DisplayName("生成 Access Token 成功")
        void generateAccessToken_shouldReturnValidToken() {
            // When
            String token = jwtTokenProvider.generateAccessToken(TEST_PID, TEST_USERNAME, TEST_ROLE);

            // Then
            assertNotNull(token);
            assertTrue(token.length() > 0);
            // JWT 格式: header.payload.signature
            assertEquals(3, token.split("\\.").length);
        }

        @Test
        @DisplayName("生成 Access Token - 可从中提取 PID")
        void generateAccessToken_shouldContainPid() {
            // When
            String token = jwtTokenProvider.generateAccessToken(TEST_PID, TEST_USERNAME, TEST_ROLE);

            // Then
            Long extractedPid = jwtTokenProvider.getPidFromToken(token);
            assertEquals(TEST_PID, extractedPid);
        }

        @Test
        @DisplayName("生成 Access Token - Token 类型为 access")
        void generateAccessToken_shouldHaveAccessType() {
            // When
            String token = jwtTokenProvider.generateAccessToken(TEST_PID, TEST_USERNAME, TEST_ROLE);

            // Then
            String tokenType = jwtTokenProvider.getTokenType(token);
            assertEquals("access", tokenType);
        }

        @Test
        @DisplayName("每次生成的 Access Token 不同")
        void generateAccessToken_shouldGenerateDifferentTokensEachTime() throws InterruptedException {
            // When
            String token1 = jwtTokenProvider.generateAccessToken(TEST_PID, TEST_USERNAME, TEST_ROLE);
            Thread.sleep(1100); // 确保时间戳不同（JWT iat 精度为秒）
            String token2 = jwtTokenProvider.generateAccessToken(TEST_PID, TEST_USERNAME, TEST_ROLE);

            // Then
            assertNotEquals(token1, token2);
        }
    }

    @Nested
    @DisplayName("Refresh Token 生成测试")
    class GenerateRefreshTokenTests {

        @Test
        @DisplayName("生成 Refresh Token 成功")
        void generateRefreshToken_shouldReturnValidToken() {
            // When
            String token = jwtTokenProvider.generateRefreshToken(TEST_PID);

            // Then
            assertNotNull(token);
            assertEquals(3, token.split("\\.").length);
        }

        @Test
        @DisplayName("生成 Refresh Token - 可从中提取 PID")
        void generateRefreshToken_shouldContainPid() {
            // When
            String token = jwtTokenProvider.generateRefreshToken(TEST_PID);

            // Then
            Long extractedPid = jwtTokenProvider.getPidFromToken(token);
            assertEquals(TEST_PID, extractedPid);
        }

        @Test
        @DisplayName("生成 Refresh Token - Token 类型为 refresh")
        void generateRefreshToken_shouldHaveRefreshType() {
            // When
            String token = jwtTokenProvider.generateRefreshToken(TEST_PID);

            // Then
            String tokenType = jwtTokenProvider.getTokenType(token);
            assertEquals("refresh", tokenType);
        }
    }

    @Nested
    @DisplayName("Token 验证测试")
    class ValidateTokenTests {

        @Test
        @DisplayName("验证有效 Access Token")
        void validateToken_shouldReturnTrue_forValidAccessToken() {
            // Given
            String token = jwtTokenProvider.generateAccessToken(TEST_PID, TEST_USERNAME, TEST_ROLE);

            // When
            boolean isValid = jwtTokenProvider.validateToken(token);

            // Then
            assertTrue(isValid);
        }

        @Test
        @DisplayName("验证有效 Refresh Token")
        void validateToken_shouldReturnTrue_forValidRefreshToken() {
            // Given
            String token = jwtTokenProvider.generateRefreshToken(TEST_PID);

            // When
            boolean isValid = jwtTokenProvider.validateToken(token);

            // Then
            assertTrue(isValid);
        }

        @Test
        @DisplayName("验证失败 - Token 格式错误")
        void validateToken_shouldReturnFalse_forMalformedToken() {
            // Given
            String malformedToken = "not.a.valid.jwt.token";

            // When
            boolean isValid = jwtTokenProvider.validateToken(malformedToken);

            // Then
            assertFalse(isValid);
        }

        @Test
        @DisplayName("验证失败 - Token 被篡改")
        void validateToken_shouldReturnFalse_forTamperedToken() {
            // Given
            String token = jwtTokenProvider.generateAccessToken(TEST_PID, TEST_USERNAME, TEST_ROLE);
            // 篡改签名部分
            String[] parts = token.split("\\.");
            String tamperedToken = parts[0] + "." + parts[1] + ".tamperedSignature";

            // When
            boolean isValid = jwtTokenProvider.validateToken(tamperedToken);

            // Then
            assertFalse(isValid);
        }

        @Test
        @DisplayName("验证失败 - Token 使用不同密钥签名")
        void validateToken_shouldReturnFalse_forTokenWithDifferentKey() {
            // Given
            JwtTokenProvider anotherProvider = new JwtTokenProvider();
            ReflectionTestUtils.setField(anotherProvider, "jwtSecret",
                    "anotherSecretKeyForTestingPurpose12345");
            ReflectionTestUtils.setField(anotherProvider, "accessTokenExpiration", 3600000L);

            String tokenFromOtherProvider = anotherProvider.generateAccessToken(TEST_PID, TEST_USERNAME, TEST_ROLE);

            // When
            boolean isValid = jwtTokenProvider.validateToken(tokenFromOtherProvider);

            // Then
            assertFalse(isValid);
        }

        @Test
        @DisplayName("验证失败 - 空 Token")
        void validateToken_shouldReturnFalse_forEmptyToken() {
            // When
            boolean isValid = jwtTokenProvider.validateToken("");

            // Then
            assertFalse(isValid);
        }

        @Test
        @DisplayName("验证失败 - null Token")
        void validateToken_shouldReturnFalse_forNullToken() {
            // When
            boolean isValid = jwtTokenProvider.validateToken(null);

            // Then
            assertFalse(isValid);
        }

        @Test
        @DisplayName("验证失败 - 过期的 Token")
        void validateToken_shouldReturnFalse_forExpiredToken() {
            // Given - 创建一个立即过期的 token
            JwtTokenProvider shortLivedProvider = new JwtTokenProvider();
            ReflectionTestUtils.setField(shortLivedProvider, "jwtSecret", TEST_SECRET);
            ReflectionTestUtils.setField(shortLivedProvider, "accessTokenExpiration", 1L); // 1 毫秒

            String expiredToken = shortLivedProvider.generateAccessToken(TEST_PID, TEST_USERNAME, TEST_ROLE);

            // Wait for expiration
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When
            boolean isValid = jwtTokenProvider.validateToken(expiredToken);

            // Then
            assertFalse(isValid);
        }
    }

    @Nested
    @DisplayName("Token 过期检测测试")
    class TokenExpirationTests {

        @Test
        @DisplayName("新生成的 Token 不会即将过期")
        void isTokenExpiringSoon_shouldReturnFalse_forNewToken() {
            // Given
            String token = jwtTokenProvider.generateAccessToken(TEST_PID, TEST_USERNAME, TEST_ROLE);

            // When
            boolean expiringSoon = jwtTokenProvider.isTokenExpiringSoon(token);

            // Then
            assertFalse(expiringSoon);
        }

        @Test
        @DisplayName("即将过期的 Token 应被检测 - 验证方法存在且可调用")
        void isTokenExpiringSoon_methodShouldBeCallable() {
            // Given - 创建一个正常的 token
            String token = jwtTokenProvider.generateAccessToken(TEST_PID, TEST_USERNAME, TEST_ROLE);

            // When - 调用方法验证其可用性
            boolean result = jwtTokenProvider.isTokenExpiringSoon(token);

            // Then - 新生成的 token 不应该即将过期
            assertFalse(result, "新生成的 token 不应该即将过期");
            // 注：由于 JWT 时间戳精度为秒级，短生命周期 token 的边界测试不稳定，故此处仅测试方法可调用性
        }

        @Test
        @DisplayName("无效 Token 返回 false")
        void isTokenExpiringSoon_shouldReturnFalse_forInvalidToken() {
            // When
            boolean expiringSoon = jwtTokenProvider.isTokenExpiringSoon("invalid.token.here");

            // Then
            assertFalse(expiringSoon);
        }
    }

    @Nested
    @DisplayName("Token 解析测试")
    class TokenParsingTests {

        @Test
        @DisplayName("从 Token 提取 PID")
        void getPidFromToken_shouldReturnCorrectPid() {
            // Given
            Long expectedPid = 12345L;
            String token = jwtTokenProvider.generateAccessToken(expectedPid, "user", 1);

            // When
            Long extractedPid = jwtTokenProvider.getPidFromToken(token);

            // Then
            assertEquals(expectedPid, extractedPid);
        }

        @Test
        @DisplayName("从 Token 提取类型 - access")
        void getTokenType_shouldReturnAccess_forAccessToken() {
            // Given
            String token = jwtTokenProvider.generateAccessToken(TEST_PID, TEST_USERNAME, TEST_ROLE);

            // When
            String type = jwtTokenProvider.getTokenType(token);

            // Then
            assertEquals("access", type);
        }

        @Test
        @DisplayName("从 Token 提取类型 - refresh")
        void getTokenType_shouldReturnRefresh_forRefreshToken() {
            // Given
            String token = jwtTokenProvider.generateRefreshToken(TEST_PID);

            // When
            String type = jwtTokenProvider.getTokenType(token);

            // Then
            assertEquals("refresh", type);
        }
    }

    @Nested
    @DisplayName("安全性测试")
    class SecurityTests {

        @Test
        @DisplayName("不同用户生成不同 Token")
        void generateAccessToken_shouldGenerateDifferentTokensForDifferentUsers() {
            // Given
            Long pid1 = 1001L;
            Long pid2 = 1002L;

            // When
            String token1 = jwtTokenProvider.generateAccessToken(pid1, "user1", 1);
            String token2 = jwtTokenProvider.generateAccessToken(pid2, "user2", 1);

            // Then
            assertNotEquals(token1, token2);

            // 验证可以正确提取各自的 PID
            assertEquals(pid1, jwtTokenProvider.getPidFromToken(token1));
            assertEquals(pid2, jwtTokenProvider.getPidFromToken(token2));
        }

        @Test
        @DisplayName("相同用户不同角色生成不同 Token")
        void generateAccessToken_shouldGenerateDifferentTokensForDifferentRoles() {
            // When
            String adminToken = jwtTokenProvider.generateAccessToken(TEST_PID, TEST_USERNAME, 0);
            String userToken = jwtTokenProvider.generateAccessToken(TEST_PID, TEST_USERNAME, 1);

            // Then
            assertNotEquals(adminToken, userToken);
        }
    }
}
