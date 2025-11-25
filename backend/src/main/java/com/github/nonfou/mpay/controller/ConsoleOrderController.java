package com.github.nonfou.mpay.controller;

import com.github.nonfou.mpay.common.error.BusinessException;
import com.github.nonfou.mpay.common.error.ErrorCode;
import com.github.nonfou.mpay.common.response.ApiResponse;
import com.github.nonfou.mpay.dto.order.OrderSummaryDTO;
import com.github.nonfou.mpay.security.SecurityUtils;
import com.github.nonfou.mpay.service.OrderQueryService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单查询接口 - P1 功能（订单作用域查询）
 */
@RestController
@RequestMapping("/api/console/orders")
public class ConsoleOrderController {

    private final OrderQueryService orderQueryService;

    public ConsoleOrderController(OrderQueryService orderQueryService) {
        this.orderQueryService = orderQueryService;
    }

    /**
     * 查询活跃订单（有效期内未支付）
     * GET /api/console/orders/active
     */
    @GetMapping("/active")
    public ApiResponse<List<OrderSummaryDTO>> getActiveOrders(
            @RequestParam(required = false) Long pid,
            @RequestParam(required = false) Integer expireMinutes) {
        // 租户隔离：普通用户只能查询自己的数据
        Long accessiblePid = SecurityUtils.resolveAccessiblePid(pid);
        return ApiResponse.success(orderQueryService.findActiveOrders(accessiblePid, expireMinutes));
    }

    /**
     * 查询成交订单
     * GET /api/console/orders/success
     */
    @GetMapping("/success")
    public ApiResponse<List<OrderSummaryDTO>> getSuccessOrders(
            @RequestParam(required = false) Long pid,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        // 租户隔离：普通用户只能查询自己的数据
        Long accessiblePid = SecurityUtils.resolveAccessiblePid(pid);

        LocalDateTime startTime;
        LocalDateTime endTime;

        if (startDate != null && endDate != null) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                startTime = LocalDate.parse(startDate, formatter).atStartOfDay();
                endTime = LocalDate.parse(endDate, formatter).plusDays(1).atStartOfDay();
            } catch (DateTimeParseException e) {
                throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "日期格式错误，请使用 yyyy-MM-dd 格式");
            }
        } else {
            // 默认查询今天
            LocalDate today = LocalDate.now();
            startTime = today.atStartOfDay();
            endTime = today.plusDays(1).atStartOfDay();
        }

        return ApiResponse.success(orderQueryService.findSuccessOrders(accessiblePid, startTime, endTime));
    }

    /**
     * 查询超时订单
     * GET /api/console/orders/expired
     */
    @GetMapping("/expired")
    public ApiResponse<List<OrderSummaryDTO>> getExpiredOrders(
            @RequestParam(required = false) Long pid,
            @RequestParam(required = false) Integer expireMinutes) {
        // 租户隔离：普通用户只能查询自己的数据
        Long accessiblePid = SecurityUtils.resolveAccessiblePid(pid);
        return ApiResponse.success(orderQueryService.findExpiredOrders(accessiblePid, expireMinutes));
    }
}
