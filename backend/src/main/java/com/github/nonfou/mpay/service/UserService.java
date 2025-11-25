package com.github.nonfou.mpay.service;

import com.github.nonfou.mpay.common.response.PageResponse;
import com.github.nonfou.mpay.dto.user.UserDTO;
import java.util.Optional;

/**
 * 用户服务
 */
public interface UserService {

    /**
     * 分页查询用户列表
     */
    PageResponse<UserDTO> listUsers(Integer role, Integer state, int page, int pageSize);

    /**
     * 根据 PID 获取用户
     */
    Optional<UserDTO> getUserByPid(Long pid);

    /**
     * 更新用户角色
     */
    void updateRole(Long pid, Integer role);

    /**
     * 更新用户状态
     */
    void updateState(Long pid, Integer state);

    /**
     * 检查用户是否为管理员
     */
    boolean isAdmin(Long pid);
}
