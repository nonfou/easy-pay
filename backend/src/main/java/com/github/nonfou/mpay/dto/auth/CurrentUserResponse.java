package com.github.nonfou.mpay.dto.auth;

import lombok.Builder;
import lombok.Data;

/**
 * 当前用户信息响应
 */
@Data
@Builder
public class CurrentUserResponse {

    private Long pid;
    private String username;
    private String email;
    private Integer role;
    private String roleName;
}
