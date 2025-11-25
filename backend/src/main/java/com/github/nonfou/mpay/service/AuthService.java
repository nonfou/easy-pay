package com.github.nonfou.mpay.service;

import com.github.nonfou.mpay.dto.auth.CurrentUserResponse;
import com.github.nonfou.mpay.dto.auth.LoginRequest;
import com.github.nonfou.mpay.dto.auth.RefreshTokenRequest;
import com.github.nonfou.mpay.dto.auth.TokenResponse;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     */
    TokenResponse login(LoginRequest request);

    /**
     * 刷新 Token
     */
    TokenResponse refreshToken(RefreshTokenRequest request);

    /**
     * 获取当前用户信息
     */
    CurrentUserResponse getCurrentUser();
}
