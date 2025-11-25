package com.github.nonfou.mpay.controller;

import com.github.nonfou.mpay.common.response.ApiResponse;
import com.github.nonfou.mpay.dto.auth.CurrentUserResponse;
import com.github.nonfou.mpay.dto.auth.LoginRequest;
import com.github.nonfou.mpay.dto.auth.RefreshTokenRequest;
import com.github.nonfou.mpay.dto.auth.TokenResponse;
import com.github.nonfou.mpay.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse token = authService.login(request);
        return ApiResponse.ok(token);
    }

    /**
     * 刷新 Token
     */
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse token = authService.refreshToken(request);
        return ApiResponse.ok(token);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> getCurrentUser() {
        CurrentUserResponse user = authService.getCurrentUser();
        return ApiResponse.ok(user);
    }

    /**
     * 登出 (前端清除 Token 即可，后端无需处理)
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // JWT 是无状态的，登出只需前端清除 Token
        // 如需实现 Token 黑名单，可在此处添加逻辑
        return ApiResponse.ok(null);
    }
}
