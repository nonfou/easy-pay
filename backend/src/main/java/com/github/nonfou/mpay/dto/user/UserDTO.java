package com.github.nonfou.mpay.dto.user;

import lombok.Builder;
import lombok.Data;

/**
 * 用户信息 DTO
 */
@Data
@Builder
public class UserDTO {

    private Long id;
    private Long pid;
    private String username;
    private String email;
    private Integer role;
    private String roleName;
    private Integer state;
    private String stateName;
}
