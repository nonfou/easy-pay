package com.mapy.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    /**
     * 设备/客户端信息，可用于风控。
     */
    private String clientId;
}
