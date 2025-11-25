package com.github.nonfou.mpay.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * API 限流拦截器
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    /** 公共 API 限流：每分钟 60 次 */
    private static final int PUBLIC_API_LIMIT = 60;
    private static final int PUBLIC_API_WINDOW = 60;

    /** 认证 API 限流：每分钟 10 次（防暴力破解） */
    private static final int AUTH_API_LIMIT = 10;
    private static final int AUTH_API_WINDOW = 60;

    private final RateLimiter rateLimiter;

    public RateLimitInterceptor(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        String path = request.getRequestURI();

        int limit;
        int window;
        String keyPrefix;

        // 根据 API 类型设置不同的限流策略
        if (path.startsWith("/api/auth/")) {
            // 认证接口使用更严格的限流
            limit = AUTH_API_LIMIT;
            window = AUTH_API_WINDOW;
            keyPrefix = "auth:";
        } else if (path.startsWith("/api/public/")) {
            // 公共接口
            limit = PUBLIC_API_LIMIT;
            window = PUBLIC_API_WINDOW;
            keyPrefix = "public:";
        } else {
            // 其他接口不限流（已有 JWT 认证保护）
            return true;
        }

        String key = keyPrefix + clientIp;
        if (!rateLimiter.isAllowed(key, limit, window)) {
            log.warn("请求被限流: ip={}, path={}", clientIp, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
            return false;
        }

        // 添加限流相关响应头
        long remaining = rateLimiter.getRemainingRequests(key, limit);
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多个代理，取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
