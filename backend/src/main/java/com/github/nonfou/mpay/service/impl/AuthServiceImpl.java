package com.github.nonfou.mpay.service.impl;

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
import com.github.nonfou.mpay.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final MerchantRepository merchantRepository;

    @Value("${mpay.jwt.access-token-expiration:3600000}")
    private long accessTokenExpiration;

    @Override
    public TokenResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            MerchantUserDetails userDetails = (MerchantUserDetails) authentication.getPrincipal();

            String accessToken = jwtTokenProvider.generateAccessToken(
                    userDetails.getPid(),
                    userDetails.getUsername(),
                    userDetails.getRole()
            );
            String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails.getPid());

            return TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(accessTokenExpiration / 1000)
                    .build();

        } catch (BadCredentialsException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        } catch (DisabledException e) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
    }

    @Override
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无效的 Refresh Token");
        }

        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无效的 Token 类型");
        }

        Long pid = jwtTokenProvider.getPidFromToken(refreshToken);
        MerchantEntity merchant = merchantRepository.findByPid(pid)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        if (merchant.getState() == null || merchant.getState() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                merchant.getPid(),
                merchant.getUsername(),
                merchant.getRole()
        );
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(merchant.getPid());

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000)
                .build();
    }

    @Override
    public CurrentUserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }

        if (!(authentication.getPrincipal() instanceof MerchantUserDetails userDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无效的认证信息");
        }

        MerchantEntity merchant = merchantRepository.findByPid(userDetails.getPid())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        return CurrentUserResponse.builder()
                .pid(merchant.getPid())
                .username(merchant.getUsername())
                .email(merchant.getEmail())
                .role(merchant.getRole())
                .roleName(merchant.isAdmin() ? "管理员" : "普通用户")
                .build();
    }
}
