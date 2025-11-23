package com.mapy.payment.controller;

import com.mapy.common.response.ApiResponse;
import com.mapy.payment.dto.PublicCreateOrderDTO;
import com.mapy.payment.dto.PublicCreateOrderResult;
import com.mapy.payment.service.PublicOrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/orders")
public class PublicOrderController {

    private final PublicOrderService publicOrderService;

    public PublicOrderController(PublicOrderService publicOrderService) {
        this.publicOrderService = publicOrderService;
    }

    @PostMapping
    public ApiResponse<PublicCreateOrderResult> createOrder(@RequestBody @Valid PublicCreateOrderDTO request) {
        PublicCreateOrderResult result = publicOrderService.createOrder(request);
        return ApiResponse.success(result);
    }
}


