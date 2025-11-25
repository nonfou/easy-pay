package com.github.nonfou.mpay.controller;

import com.github.nonfou.mpay.common.response.ApiResponse;
import com.github.nonfou.mpay.dto.MatchRequest;
import com.github.nonfou.mpay.service.OrderMatchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/orders")
public class OrderMatchController {

    private final OrderMatchService orderMatchService;

    public OrderMatchController(OrderMatchService orderMatchService) {
        this.orderMatchService = orderMatchService;
    }

    @PostMapping("/match")
    public ApiResponse<Void> match(@RequestBody @Valid MatchRequest request) {
        orderMatchService.matchPayment(request);
        return ApiResponse.success();
    }
}
