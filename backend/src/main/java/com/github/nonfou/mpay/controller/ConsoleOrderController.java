package com.github.nonfou.mpay.controller;

import com.github.nonfou.mpay.common.response.ApiResponse;
import com.github.nonfou.mpay.dto.order.OrderSummaryDTO;
import com.github.nonfou.mpay.service.OrderQueryService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        return ApiResponse.success(orderQueryService.findActiveOrders(pid, expireMinutes));
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

        LocalDateTime startTime;
        LocalDateTime endTime;

        if (startDate != null && endDate != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            startTime = LocalDate.parse(startDate, formatter).atStartOfDay();
            endTime = LocalDate.parse(endDate, formatter).plusDays(1).atStartOfDay();
        } else {
            // 默认查询今天
            LocalDate today = LocalDate.now();
            startTime = today.atStartOfDay();
            endTime = today.plusDays(1).atStartOfDay();
        }

        return ApiResponse.success(orderQueryService.findSuccessOrders(pid, startTime, endTime));
    }

    /**
     * 查询超时订单
     * GET /api/console/orders/expired
     */
    @GetMapping("/expired")
    public ApiResponse<List<OrderSummaryDTO>> getExpiredOrders(
            @RequestParam(required = false) Long pid,
            @RequestParam(required = false) Integer expireMinutes) {
        return ApiResponse.success(orderQueryService.findExpiredOrders(pid, expireMinutes));
    }
}
