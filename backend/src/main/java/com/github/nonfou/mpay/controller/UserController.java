package com.github.nonfou.mpay.controller;

import com.github.nonfou.mpay.common.response.ApiResponse;
import com.github.nonfou.mpay.common.response.PageResponse;
import com.github.nonfou.mpay.dto.user.UpdateRoleRequest;
import com.github.nonfou.mpay.dto.user.UserDTO;
import com.github.nonfou.mpay.service.UserService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口 - P2 功能
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 分页查询用户列表
     * GET /api/users
     */
    @GetMapping
    public ApiResponse<PageResponse<UserDTO>> listUsers(
            @RequestParam(required = false) Integer role,
            @RequestParam(required = false) Integer state,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(userService.listUsers(role, state, page, pageSize));
    }

    /**
     * 获取用户信息
     * GET /api/users/{pid}
     */
    @GetMapping("/{pid}")
    public ApiResponse<UserDTO> getUser(@PathVariable Long pid) {
        return userService.getUserByPid(pid)
                .map(ApiResponse::success)
                .orElse(ApiResponse.failure(404, "用户不存在"));
    }

    /**
     * 更新用户角色
     * PUT /api/users/{pid}/role
     */
    @PutMapping("/{pid}/role")
    public ApiResponse<Map<String, Object>> updateRole(
            @PathVariable Long pid,
            @Valid @RequestBody UpdateRoleRequest request) {
        userService.updateRole(pid, request.getRole());
        return ApiResponse.success(Map.of(
                "pid", pid,
                "role", request.getRole()
        ));
    }

    /**
     * 更新用户状态
     * PUT /api/users/{pid}/state
     */
    @PutMapping("/{pid}/state")
    public ApiResponse<Map<String, Object>> updateState(
            @PathVariable Long pid,
            @RequestParam Integer state) {
        userService.updateState(pid, state);
        return ApiResponse.success(Map.of(
                "pid", pid,
                "state", state
        ));
    }

    /**
     * 检查用户是否为管理员
     * GET /api/users/{pid}/is-admin
     */
    @GetMapping("/{pid}/is-admin")
    public ApiResponse<Map<String, Object>> checkAdmin(@PathVariable Long pid) {
        boolean isAdmin = userService.isAdmin(pid);
        return ApiResponse.success(Map.of(
                "pid", pid,
                "isAdmin", isAdmin
        ));
    }
}
