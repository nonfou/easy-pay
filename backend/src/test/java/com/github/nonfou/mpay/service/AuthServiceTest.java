package com.github.nonfou.mpay.service;

import com.github.nonfou.mpay.common.error.BusinessException;
import com.github.nonfou.mpay.common.error.ErrorCode;
import com.github.nonfou.mpay.dto.auth.CurrentUserResponse;
import com.github.nonfou.mpay.dto.auth.LoginRequest;
import com.github.nonfou.mpay.dto.auth.RefreshTokenRequest;
import com.github.nonfou.mpay.dto.auth.TokenResponse;
import com.github.nonfou.mpay.entity.MerchantEntity;
import com.github.nonfou.mpay.repository.MerchantRepository;
import com.github.nonfou.mpay.security.JwtTokenProvider;
import com.github.nonfou.mpay.security.MerchantUserDetails;
import com.github.nonfou.mpay.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试
 * 测试认证服务
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 认证服务测试")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private MerchantRepository merchantRepository;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(authenticationManager, jwtTokenProvider, merchantRepository);
        ReflectionTestUtils.setField(authService, "accessTokenExpiration", 3600000L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("登录测试")
    class LoginTests {

        @Test
        @DisplayName("登录成功 - 返回有效Token")
        void login_shouldReturnTokens_whenCredentialsValid() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername("admin");
            request.setPassword("password123");

            MerchantEntity merchant = createMerchant(1001L, "admin", 1, 1);
            MerchantUserDetails userDetails = new MerchantUserDetails(merchant);

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(userDetails);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyInt()))
                    .thenReturn("access-token-123");
            when(jwtTokenProvider.generateRefreshToken(anyLong()))
                    .thenReturn("refresh-token-456");

            // When
            TokenResponse response = authService.login(request);

            // Then
            assertNotNull(response);
            assertEquals("access-token-123", response.getAccessToken());
            assertEquals("refresh-token-456", response.getRefreshToken());
            assertEquals("Bearer", response.getTokenType());
            assertEquals(3600L, response.getExpiresIn()); // 3600000ms / 1000

            verify(jwtTokenProvider).generateAccessToken(1001L, "admin", 1);
            verify(jwtTokenProvider).generateRefreshToken(1001L);
        }

        @Test
        @DisplayName("登录失败 - 用户名或密码错误")
        void login_shouldThrowException_whenBadCredentials() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername("admin");
            request.setPassword("wrongpassword");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.login(request));

            assertEquals(ErrorCode.LOGIN_FAILED, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("用户名或密码错误"));

            verify(jwtTokenProvider, never()).generateAccessToken(anyLong(), anyString(), anyInt());
        }

        @Test
        @DisplayName("登录失败 - 账号被禁用")
        void login_shouldThrowException_whenAccountDisabled() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername("admin");
            request.setPassword("password123");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new DisabledException("Account disabled"));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.login(request));

            assertEquals(ErrorCode.ACCOUNT_DISABLED, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("禁用"));

            verify(jwtTokenProvider, never()).generateAccessToken(anyLong(), anyString(), anyInt());
        }

        @Test
        @DisplayName("登录失败 - 其他认证异常")
        void login_shouldThrowException_whenOtherAuthError() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername("admin");
            request.setPassword("password123");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new RuntimeException("Unknown error"));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.login(request));

            assertEquals(ErrorCode.LOGIN_FAILED, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("刷新Token测试")
    class RefreshTokenTests {

        @Test
        @DisplayName("刷新Token成功")
        void refreshToken_shouldReturnNewTokens_whenValidRefreshToken() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("valid-refresh-token");

            MerchantEntity merchant = createMerchant(1001L, "admin", 1, 1);

            when(jwtTokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
            when(jwtTokenProvider.getTokenType("valid-refresh-token")).thenReturn("refresh");
            when(jwtTokenProvider.getPidFromToken("valid-refresh-token")).thenReturn(1001L);
            when(merchantRepository.findByPid(1001L)).thenReturn(Optional.of(merchant));
            when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyInt()))
                    .thenReturn("new-access-token");
            when(jwtTokenProvider.generateRefreshToken(anyLong()))
                    .thenReturn("new-refresh-token");

            // When
            TokenResponse response = authService.refreshToken(request);

            // Then
            assertNotNull(response);
            assertEquals("new-access-token", response.getAccessToken());
            assertEquals("new-refresh-token", response.getRefreshToken());
            assertEquals("Bearer", response.getTokenType());
        }

        @Test
        @DisplayName("刷新Token失败 - 无效的RefreshToken")
        void refreshToken_shouldThrowException_whenInvalidToken() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("invalid-token");

            when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.refreshToken(request));

            assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("Refresh Token"));
        }

        @Test
        @DisplayName("刷新Token失败 - Token类型错误")
        void refreshToken_shouldThrowException_whenWrongTokenType() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("access-token-used-as-refresh");

            when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
            when(jwtTokenProvider.getTokenType(anyString())).thenReturn("access");

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.refreshToken(request));

            assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("Token 类型"));
        }

        @Test
        @DisplayName("刷新Token失败 - 用户不存在")
        void refreshToken_shouldThrowException_whenUserNotFound() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("valid-refresh-token");

            when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
            when(jwtTokenProvider.getTokenType(anyString())).thenReturn("refresh");
            when(jwtTokenProvider.getPidFromToken(anyString())).thenReturn(9999L);
            when(merchantRepository.findByPid(9999L)).thenReturn(Optional.empty());

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.refreshToken(request));

            assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("用户不存在"));
        }

        @Test
        @DisplayName("刷新Token失败 - 账号被禁用")
        void refreshToken_shouldThrowException_whenAccountDisabled() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("valid-refresh-token");

            MerchantEntity merchant = createMerchant(1001L, "admin", 1, 0); // state=0 表示禁用

            when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
            when(jwtTokenProvider.getTokenType(anyString())).thenReturn("refresh");
            when(jwtTokenProvider.getPidFromToken(anyString())).thenReturn(1001L);
            when(merchantRepository.findByPid(1001L)).thenReturn(Optional.of(merchant));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.refreshToken(request));

            assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("禁用"));
        }

        @Test
        @DisplayName("刷新Token失败 - 账号状态为null")
        void refreshToken_shouldThrowException_whenAccountStateIsNull() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("valid-refresh-token");

            MerchantEntity merchant = createMerchant(1001L, "admin", 1, null);

            when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
            when(jwtTokenProvider.getTokenType(anyString())).thenReturn("refresh");
            when(jwtTokenProvider.getPidFromToken(anyString())).thenReturn(1001L);
            when(merchantRepository.findByPid(1001L)).thenReturn(Optional.of(merchant));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.refreshToken(request));

            assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("获取当前用户测试")
    class GetCurrentUserTests {

        @Test
        @DisplayName("获取当前用户成功")
        void getCurrentUser_shouldReturnUserInfo_whenAuthenticated() {
            // Given
            MerchantEntity merchant = createMerchant(1001L, "admin", 1, 1);
            merchant.setEmail("admin@example.com");
            MerchantUserDetails userDetails = new MerchantUserDetails(merchant);

            Authentication authentication = mock(Authentication.class);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(userDetails);

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            when(merchantRepository.findByPid(1001L)).thenReturn(Optional.of(merchant));

            // When
            CurrentUserResponse response = authService.getCurrentUser();

            // Then
            assertNotNull(response);
            assertEquals(1001L, response.getPid());
            assertEquals("admin", response.getUsername());
            assertEquals("admin@example.com", response.getEmail());
            assertEquals(1, response.getRole());
            assertEquals("管理员", response.getRoleName());
        }

        @Test
        @DisplayName("获取当前用户 - 普通用户角色名称")
        void getCurrentUser_shouldReturnNormalUserRoleName_whenNotAdmin() {
            // Given
            MerchantEntity merchant = createMerchant(1001L, "user1", 0, 1); // role=0 普通用户
            MerchantUserDetails userDetails = new MerchantUserDetails(merchant);

            Authentication authentication = mock(Authentication.class);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(userDetails);

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            when(merchantRepository.findByPid(1001L)).thenReturn(Optional.of(merchant));

            // When
            CurrentUserResponse response = authService.getCurrentUser();

            // Then
            assertEquals("普通用户", response.getRoleName());
        }

        @Test
        @DisplayName("获取当前用户失败 - 未登录")
        void getCurrentUser_shouldThrowException_whenNotAuthenticated() {
            // Given
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(null);
            SecurityContextHolder.setContext(securityContext);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.getCurrentUser());

            assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("未登录"));
        }

        @Test
        @DisplayName("获取当前用户失败 - 认证信息无效")
        void getCurrentUser_shouldThrowException_whenInvalidPrincipal() {
            // Given
            Authentication authentication = mock(Authentication.class);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn("anonymous");

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.getCurrentUser());

            assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("无效的认证信息"));
        }

        @Test
        @DisplayName("获取当前用户失败 - 用户不存在")
        void getCurrentUser_shouldThrowException_whenUserNotFound() {
            // Given
            MerchantEntity merchant = createMerchant(1001L, "admin", 1, 1);
            MerchantUserDetails userDetails = new MerchantUserDetails(merchant);

            Authentication authentication = mock(Authentication.class);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(userDetails);

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            when(merchantRepository.findByPid(1001L)).thenReturn(Optional.empty());

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.getCurrentUser());

            assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
        }
    }

    // ==================== 辅助方法 ====================

    private MerchantEntity createMerchant(Long pid, String username, Integer role, Integer state) {
        MerchantEntity merchant = new MerchantEntity();
        merchant.setId(1L);
        merchant.setPid(pid);
        merchant.setUsername(username);
        merchant.setPassword("encoded-password");
        merchant.setRole(role);
        merchant.setState(state);
        return merchant;
    }
}
