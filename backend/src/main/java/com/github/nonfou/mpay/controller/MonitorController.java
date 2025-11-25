package com.github.nonfou.mpay.controller;

import com.github.nonfou.mpay.common.response.ApiResponse;
import com.github.nonfou.mpay.dto.monitor.OrderHeartbeatDTO;
import com.github.nonfou.mpay.dto.monitor.PaymentRecordDTO;
import com.github.nonfou.mpay.service.OrderHeartbeatService;
import com.github.nonfou.mpay.service.PaymentMatchService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private final OrderHeartbeatService orderHeartbeatService;
    private final PaymentMatchService paymentMatchService;

    public MonitorController(OrderHeartbeatService orderHeartbeatService,
            PaymentMatchService paymentMatchService) {
        this.orderHeartbeatService = orderHeartbeatService;
        this.paymentMatchService = paymentMatchService;
    }

    @GetMapping("/orders/active")
    public ApiResponse<List<OrderHeartbeatDTO>> getActiveOrders(@RequestParam(required = false) String pid) {
        return ApiResponse.success(orderHeartbeatService.fetchActiveOrders(pid));
    }

    @PostMapping("/records")
    public ApiResponse<Void> submitPaymentRecord(@RequestBody PaymentRecordDTO record) {
        paymentMatchService.handlePaymentRecord(record);
        return ApiResponse.success();
    }
}
