package com.github.nonfou.mpay.controller;

import com.github.nonfou.mpay.common.response.ApiResponse;
import com.github.nonfou.mpay.dto.order.ManualSettleRequest;
import com.github.nonfou.mpay.entity.OrderEntity;
import com.github.nonfou.mpay.service.AdminOrderService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台订单管理接口 - P0 功能
 */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    /**
     * 手动补单
     * POST /api/admin/orders/{orderId}/settle
     */
    @PostMapping("/{orderId}/settle")
    public ApiResponse<Map<String, Object>> manualSettle(
            @PathVariable String orderId,
            @Valid @RequestBody ManualSettleRequest request) {
        OrderEntity order = adminOrderService.manualSettle(orderId, request.getRemark());
        return ApiResponse.success(Map.of(
                "orderId", order.getOrderId(),
                "state", order.getState(),
                "payTime", order.getPayTime()
        ));
    }

    /**
     * 重新发送通知
     * POST /api/admin/orders/{orderId}/renotify
     */
    @PostMapping("/{orderId}/renotify")
    public ApiResponse<Map<String, Object>> renotify(@PathVariable String orderId) {
        boolean success = adminOrderService.renotify(orderId);
        return ApiResponse.success(Map.of(
                "orderId", orderId,
                "success", success
        ));
    }

    /**
     * 批量清理超时订单
     * DELETE /api/admin/orders/expired
     */
    @DeleteMapping("/expired")
    public ApiResponse<Map<String, Object>> cleanExpiredOrders(
            @RequestParam(required = false) Integer expireMinutes) {
        int deletedCount = adminOrderService.cleanExpiredOrders(expireMinutes);
        return ApiResponse.success(Map.of(
                "deletedCount", deletedCount
        ));
    }
}
