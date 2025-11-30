package com.github.nonfou.mpay.transaction.controller;

import com.github.nonfou.mpay.common.response.ApiResponse;
import com.github.nonfou.mpay.common.response.PageResponse;
import com.github.nonfou.mpay.transaction.dto.*;
import com.github.nonfou.mpay.transaction.entity.PaymentEventLogEntity;
import com.github.nonfou.mpay.transaction.entity.PaymentTransactionEntity;
import com.github.nonfou.mpay.transaction.service.PaymentEventLogService;
import com.github.nonfou.mpay.transaction.service.PaymentTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 交易记录管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/transaction")
@RequiredArgsConstructor
public class TransactionController {

    private final PaymentTransactionService transactionService;
    private final PaymentEventLogService eventLogService;

    /**
     * 分页查询交易记录
     */
    @GetMapping("/list")
    public ApiResponse<PageResponse<TransactionDetailResponse>> list(TransactionQueryRequest request) {
        PageRequest pageable = PageRequest.of(request.getPage(), request.getSize());

        Page<PaymentTransactionEntity> page = transactionService.findByConditions(
                request.getOrderId(),
                request.getTradeNo(),
                request.getPlatform(),
                request.getStatus(),
                request.getMerchantId(),
                request.getStartTime(),
                request.getEndTime(),
                pageable
        );

        Page<TransactionDetailResponse> responsePage = page.map(this::toDetailResponse);

        return ApiResponse.success(PageResponse.of(responsePage));
    }

    /**
     * 根据订单号查询交易详情
     */
    @GetMapping("/order/{orderId}")
    public ApiResponse<TransactionDetailResponse> getByOrderId(@PathVariable String orderId) {
        return transactionService.findByOrderId(orderId)
                .map(this::toDetailResponse)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "交易记录不存在"));
    }

    /**
     * 根据交易号查询交易详情
     */
    @GetMapping("/trade/{tradeNo}")
    public ApiResponse<TransactionDetailResponse> getByTradeNo(@PathVariable String tradeNo) {
        return transactionService.findByTradeNo(tradeNo)
                .map(this::toDetailResponse)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "交易记录不存在"));
    }

    /**
     * 根据ID查询交易详情
     */
    @GetMapping("/{id}")
    public ApiResponse<TransactionDetailResponse> getById(@PathVariable Long id) {
        return transactionService.findById(id)
                .map(this::toDetailResponse)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "交易记录不存在"));
    }

    /**
     * 获取交易的全部事件日志（流程追溯）
     */
    @GetMapping("/{id}/events")
    public ApiResponse<List<EventLogResponse>> getTransactionEvents(@PathVariable Long id) {
        List<PaymentEventLogEntity> events = eventLogService.findByTransactionId(id);
        List<EventLogResponse> responses = events.stream()
                .map(this::toEventLogResponse)
                .toList();
        return ApiResponse.success(responses);
    }

    /**
     * 根据订单号获取全部事件日志
     */
    @GetMapping("/order/{orderId}/events")
    public ApiResponse<List<EventLogResponse>> getOrderEvents(@PathVariable String orderId) {
        List<PaymentEventLogEntity> events = eventLogService.findByOrderId(orderId);
        List<EventLogResponse> responses = events.stream()
                .map(this::toEventLogResponse)
                .toList();
        return ApiResponse.success(responses);
    }

    /**
     * 获取交易的原始请求/响应数据
     */
    @GetMapping("/{id}/raw")
    public ApiResponse<TransactionRawDataResponse> getRawData(@PathVariable Long id) {
        return transactionService.findById(id)
                .map(t -> {
                    TransactionRawDataResponse response = new TransactionRawDataResponse();
                    response.setId(t.getId());
                    response.setOrderId(t.getOrderId());
                    response.setTradeNo(t.getTradeNo());
                    response.setRawRequest(t.getRawRequest());
                    response.setRawResponse(t.getRawResponse());
                    response.setNotifyData(t.getNotifyData());
                    return response;
                })
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "交易记录不存在"));
    }

    /**
     * 转换为详情响应
     */
    private TransactionDetailResponse toDetailResponse(PaymentTransactionEntity entity) {
        TransactionDetailResponse response = new TransactionDetailResponse();
        response.setId(entity.getId());
        response.setOrderId(entity.getOrderId());
        response.setPlatform(entity.getPlatform());
        response.setTradeNo(entity.getTradeNo());
        response.setPlatformTradeNo(entity.getPlatformTradeNo());
        response.setTradeType(entity.getTradeType());
        response.setAmount(entity.getAmount());
        response.setRefundedAmount(entity.getRefundedAmount());
        response.setStatus(entity.getStatus());
        response.setSubject(entity.getSubject());
        response.setMerchantId(entity.getMerchantId());
        response.setPaidAt(entity.getPaidAt());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    /**
     * 转换为事件日志响应
     */
    private EventLogResponse toEventLogResponse(PaymentEventLogEntity entity) {
        EventLogResponse response = new EventLogResponse();
        response.setId(entity.getId());
        response.setOrderId(entity.getOrderId());
        response.setTransactionId(entity.getTransactionId());
        response.setRefundId(entity.getRefundId());
        response.setEventType(entity.getEventType());
        response.setPlatform(entity.getPlatform());
        response.setRequestData(entity.getRequestData());
        response.setResponseData(entity.getResponseData());
        response.setResultCode(entity.getResultCode());
        response.setResultMessage(entity.getResultMessage());
        response.setSuccess(entity.getSuccess());
        response.setDurationMs(entity.getDurationMs());
        response.setClientIp(entity.getClientIp());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}
