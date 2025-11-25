package com.github.nonfou.mpay.dto.auth;

import lombok.Builder;
import lombok.Data;

/**
 * Token 响应
 */
@Data
@Builder
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
}
