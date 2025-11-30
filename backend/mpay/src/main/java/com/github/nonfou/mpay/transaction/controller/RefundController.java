package com.github.nonfou.mpay.transaction.controller;

import com.github.nonfou.mpay.common.response.ApiResponse;
import com.github.nonfou.mpay.common.response.PageResponse;
import com.github.nonfou.mpay.transaction.dto.*;
import com.github.nonfou.mpay.transaction.entity.PaymentEventLogEntity;
import com.github.nonfou.mpay.transaction.entity.RefundRecordEntity;
import com.github.nonfou.mpay.transaction.service.PaymentEventLogService;
import com.github.nonfou.mpay.transaction.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 退款管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/refund")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;
    private final PaymentEventLogService eventLogService;

    /**
     * 发起退款
     */
    @PostMapping
    public ApiResponse<RefundResponse> refund(@Valid @RequestBody RefundRequest request) {
        log.info("发起退款请求: orderId={}, amount={}", request.getOrderId(), request.getRefundAmount());
        RefundResponse response = refundService.refund(request);
        if (response.isSuccess()) {
            return ApiResponse.success(response);
        } else {
            return ApiResponse.error(2006, response.getResultMessage(), response);
        }
    }

    /**
     * 分页查询退款记录
     */
    @GetMapping("/list")
    public ApiResponse<PageResponse<RefundDetailResponse>> list(RefundQueryRequest request) {
        PageRequest pageable = PageRequest.of(request.getPage(), request.getSize());

        Page<RefundRecordEntity> page = refundService.findByConditions(
                request.getOrderId(),
                request.getRefundNo(),
                request.getPlatform(),
                request.getStatus(),
                request.getStartTime(),
                request.getEndTime(),
                pageable
        );

        Page<RefundDetailResponse> responsePage = page.map(this::toDetailResponse);

        return ApiResponse.success(PageResponse.of(responsePage));
    }

    /**
     * 根据退款单号查询退款详情
     */
    @GetMapping("/{refundNo}")
    public ApiResponse<RefundDetailResponse> getByRefundNo(@PathVariable String refundNo) {
        return refundService.findByRefundNo(refundNo)
                .map(this::toDetailResponse)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "退款记录不存在"));
    }

    /**
     * 根据订单号查询退款记录列表
     */
    @GetMapping("/order/{orderId}")
    public ApiResponse<List<RefundDetailResponse>> getByOrderId(@PathVariable String orderId) {
        List<RefundRecordEntity> refunds = refundService.findByOrderId(orderId);
        List<RefundDetailResponse> responses = refunds.stream()
                .map(this::toDetailResponse)
                .toList();
        return ApiResponse.success(responses);
    }

    /**
     * 获取退款的全部事件日志
     */
    @GetMapping("/{refundNo}/events")
    public ApiResponse<List<EventLogResponse>> getRefundEvents(@PathVariable String refundNo) {
        return refundService.findByRefundNo(refundNo)
                .map(refund -> {
                    List<PaymentEventLogEntity> events = eventLogService.findByRefundId(refund.getId());
                    return events.stream()
                            .map(this::toEventLogResponse)
                            .toList();
                })
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "退款记录不存在"));
    }

    /**
     * 获取退款的原始数据
     */
    @GetMapping("/{refundNo}/raw")
    public ApiResponse<RefundRawDataResponse> getRawData(@PathVariable String refundNo) {
        return refundService.findByRefundNo(refundNo)
                .map(r -> {
                    RefundRawDataResponse response = new RefundRawDataResponse();
                    response.setId(r.getId());
                    response.setRefundNo(r.getRefundNo());
                    response.setOrderId(r.getOrderId());
                    response.setRawRequest(r.getRawRequest());
                    response.setRawResponse(r.getRawResponse());
                    response.setNotifyData(r.getNotifyData());
                    return response;
                })
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "退款记录不存在"));
    }

    /**
     * 转换为详情响应
     */
    private RefundDetailResponse toDetailResponse(RefundRecordEntity entity) {
        RefundDetailResponse response = new RefundDetailResponse();
        response.setId(entity.getId());
        response.setTransactionId(entity.getTransactionId());
        response.setOrderId(entity.getOrderId());
        response.setRefundNo(entity.getRefundNo());
        response.setPlatform(entity.getPlatform());
        response.setPlatformRefundNo(entity.getPlatformRefundNo());
        response.setRefundAmount(entity.getRefundAmount());
        response.setRefundReason(entity.getRefundReason());
        response.setStatus(entity.getStatus());
        response.setRefundedAt(entity.getRefundedAt());
        response.setOperator(entity.getOperator());
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
