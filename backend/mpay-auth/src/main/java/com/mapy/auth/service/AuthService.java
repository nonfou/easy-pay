package com.mapy.auth.service;

import com.mapy.auth.dto.LoginRequest;
import com.mapy.auth.dto.RefreshTokenRequest;
import com.mapy.auth.dto.TokenPair;

public interface AuthService {

    TokenPair login(LoginRequest request);

    TokenPair refreshToken(RefreshTokenRequest request);

    void logout(String accessToken);
}
