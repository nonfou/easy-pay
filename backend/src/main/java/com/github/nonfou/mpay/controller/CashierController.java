package com.github.nonfou.mpay.controller;

import com.github.nonfou.mpay.common.error.BusinessException;
import com.github.nonfou.mpay.common.error.ErrorCode;
import com.github.nonfou.mpay.common.response.ApiResponse;
import com.github.nonfou.mpay.dto.cashier.CashierOrderDTO;
import com.github.nonfou.mpay.dto.cashier.CashierOrderStateDTO;
import com.github.nonfou.mpay.service.CashierService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 收银台公共接口（无需认证）
 */
@RestController
@RequestMapping("/api/cashier")
public class CashierController {

    private final CashierService cashierService;

    public CashierController(CashierService cashierService) {
        this.cashierService = cashierService;
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/orders/{orderId}")
    public ApiResponse<CashierOrderDTO> getOrderDetail(@PathVariable String orderId) {
        return cashierService.getOrderDetail(orderId)
                .map(ApiResponse::success)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
    }

    /**
     * 获取订单状态（用于轮询）
     */
    @GetMapping("/orders/{orderId}/state")
    public ApiResponse<CashierOrderStateDTO> getOrderState(@PathVariable String orderId) {
        return cashierService.getOrderState(orderId)
                .map(ApiResponse::success)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
    }
}
