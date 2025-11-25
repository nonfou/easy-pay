package com.github.nonfou.mpay.security;

import com.github.nonfou.mpay.common.error.BusinessException;
import com.github.nonfou.mpay.common.error.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具类
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // 工具类，不允许实例化
    }

    /**
     * 获取当前登录用户信息
     *
     * @return 当前用户详情
     * @throws BusinessException 如果未登录或认证信息无效
     */
    public static MerchantUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }

        if (!(authentication.getPrincipal() instanceof MerchantUserDetails userDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无效的认证信息");
        }

        return userDetails;
    }

    /**
     * 获取当前用户的 PID
     *
     * @return 当前用户的商户 ID
     */
    public static Long getCurrentUserPid() {
        return getCurrentUser().getPid();
    }

    /**
     * 判断当前用户是否为管理员
     *
     * @return true 如果是管理员
     */
    public static boolean isAdmin() {
        return getCurrentUser().isAdmin();
    }

    /**
     * 校验数据访问权限
     * 普通用户只能访问自己的数据，管理员可以访问所有数据
     *
     * @param requestedPid 请求访问的 PID
     * @return 实际应该使用的 PID（管理员可以使用请求的 PID，普通用户强制使用自己的 PID）
     */
    public static Long resolveAccessiblePid(Long requestedPid) {
        MerchantUserDetails currentUser = getCurrentUser();

        // 管理员可以查询任意商户的数据
        if (currentUser.isAdmin()) {
            return requestedPid; // 如果请求的 PID 为 null，管理员可以查询所有数据
        }

        // 普通用户只能查询自己的数据，忽略请求参数中的 PID
        return currentUser.getPid();
    }
}
