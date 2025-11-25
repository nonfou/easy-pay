package com.github.nonfou.mpay.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户角色更新请求
 */
@Data
public class UpdateRoleRequest {

    @NotNull(message = "角色不能为空")
    private Integer role;
}
