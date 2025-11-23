package com.mapy.auth.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TokenPair {
    String accessToken;
    String refreshToken;
    long expiresIn;
}
