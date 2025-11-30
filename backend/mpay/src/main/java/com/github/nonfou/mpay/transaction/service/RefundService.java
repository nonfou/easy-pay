package com.github.nonfou.mpay.transaction.service;

import com.alipay.api.AlipayApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.nonfou.mpay.common.error.BusinessException;
import com.github.nonfou.mpay.common.error.ErrorCode;
import com.github.nonfou.mpay.payment.dto.alipay.AlipayRefundRequest;
import com.github.nonfou.mpay.payment.dto.alipay.AlipayRefundResponse;
import com.github.nonfou.mpay.payment.dto.wxpay.WxPayRefundResponse;
import com.github.nonfou.mpay.payment.service.AlipayService;
import com.github.nonfou.mpay.payment.service.WxPayServiceWrapper;
import com.github.nonfou.mpay.transaction.dto.RefundRequest;
import com.github.nonfou.mpay.transaction.dto.RefundResponse;
import com.github.nonfou.mpay.transaction.entity.PaymentTransactionEntity;
import com.github.nonfou.mpay.transaction.entity.RefundRecordEntity;
import com.github.nonfou.mpay.transaction.enums.PaymentEventType;
import com.github.nonfou.mpay.transaction.enums.PaymentPlatform;
import com.github.nonfou.mpay.transaction.enums.RefundStatus;
import com.github.nonfou.mpay.transaction.enums.TransactionStatus;
import com.github.nonfou.mpay.transaction.event.RefundEvent;
import com.github.nonfou.mpay.transaction.repository.PaymentTransactionRepository;
import com.github.nonfou.mpay.transaction.repository.RefundRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 退款服务 - 统一退款入口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final PaymentTransactionRepository transactionRepository;
    private final RefundRecordRepository refundRecordRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    // 可选注入，根据配置决定是否可用
    private final Optional<AlipayService> alipayService;
    private final Optional<WxPayServiceWrapper> wxPayService;

    private static final DateTimeFormatter REFUND_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /**
     * 发起退款
     */
    @Transactional
    public RefundResponse refund(RefundRequest request) {
        long startTime = System.currentTimeMillis();

        // 1. 查找原交易
        PaymentTransactionEntity transaction = findTransaction(request);

        // 2. 验证退款条件
        validateRefund(transaction, request.getRefundAmount());

        // 3. 生成退款单号
        String refundNo = generateRefundNo();

        // 4. 创建退款记录（待处理状态）
        RefundRecordEntity refundRecord = createRefundRecord(transaction, request, refundNo);

        // 5. 调用支付平台退款
        RefundResponse response;
        try {
            response = executeRefund(transaction, refundRecord, request);
            refundRecord.setRawResponse(toJson(response));

            if (response.isSuccess()) {
                refundRecord.setStatus(RefundStatus.SUCCESS);
                refundRecord.setPlatformRefundNo(response.getPlatformRefundNo());
                refundRecord.setRefundedAt(LocalDateTime.now());

                // 更新交易退款金额
                updateTransactionRefundAmount(transaction, request.getRefundAmount());
            } else {
                refundRecord.setStatus(RefundStatus.FAILED);
            }
        } catch (Exception e) {
            log.error("退款失败: refundNo={}", refundNo, e);
            refundRecord.setStatus(RefundStatus.FAILED);
            refundRecord.setRawResponse(e.getMessage());

            response = RefundResponse.builder()
                    .success(false)
                    .refundNo(refundNo)
                    .resultCode("FAIL")
                    .resultMessage(e.getMessage())
                    .build();
        }

        // 6. 保存退款记录
        refundRecordRepository.save(refundRecord);

        // 7. 发布退款事件
        long duration = System.currentTimeMillis() - startTime;
        publishRefundEvent(transaction, refundRecord, request, response, duration);

        return response;
    }

    /**
     * 查找原交易
     */
    private PaymentTransactionEntity findTransaction(RefundRequest request) {
        PaymentTransactionEntity transaction = null;

        if (request.getTradeNo() != null) {
            transaction = transactionRepository.findByTradeNo(request.getTradeNo()).orElse(null);
        }
        if (transaction == null && request.getOrderId() != null) {
            transaction = transactionRepository.findByOrderId(request.getOrderId()).orElse(null);
        }
        if (transaction == null && request.getPlatformTradeNo() != null) {
            transaction = transactionRepository.findByPlatformTradeNo(request.getPlatformTradeNo()).orElse(null);
        }

        if (transaction == null) {
            throw new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND, "未找到原交易记录");
        }

        return transaction;
    }

    /**
     * 验证退款条件
     */
    private void validateRefund(PaymentTransactionEntity transaction, BigDecimal refundAmount) {
        // 检查交易状态
        TransactionStatus status = transaction.getStatus();
        if (status != TransactionStatus.SUCCESS &&
                status != TransactionStatus.PARTIAL_REFUNDED) {
            throw new BusinessException(ErrorCode.INVALID_TRANSACTION_STATUS,
                    "交易状态不允许退款: " + status);
        }

        // 检查退款金额
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REFUND_AMOUNT, "退款金额必须大于0");
        }

        // 检查可退款金额
        BigDecimal refundedAmount = transaction.getRefundedAmount();
        if (refundedAmount == null) {
            refundedAmount = BigDecimal.ZERO;
        }
        BigDecimal availableRefund = transaction.getAmount().subtract(refundedAmount);

        if (refundAmount.compareTo(availableRefund) > 0) {
            throw new BusinessException(ErrorCode.EXCEED_REFUND_AMOUNT,
                    "退款金额超出可退款金额, 可退: " + availableRefund);
        }
    }

    /**
     * 生成退款单号
     */
    private String generateRefundNo() {
        return "R" + LocalDateTime.now().format(REFUND_NO_FORMAT) +
                String.format("%04d", (int) (Math.random() * 10000));
    }

    /**
     * 创建退款记录
     */
    private RefundRecordEntity createRefundRecord(PaymentTransactionEntity transaction,
                                                  RefundRequest request,
                                                  String refundNo) {
        RefundRecordEntity record = new RefundRecordEntity();
        record.setTransactionId(transaction.getId());
        record.setOrderId(transaction.getOrderId());
        record.setRefundNo(refundNo);
        record.setPlatform(transaction.getPlatform());
        record.setPlatformTradeNo(transaction.getPlatformTradeNo());
        record.setRefundAmount(request.getRefundAmount());
        record.setRefundReason(request.getRefundReason());
        record.setStatus(RefundStatus.PENDING);
        record.setRawRequest(toJson(request));
        record.setOperator(request.getOperator());

        return record;
    }

    /**
     * 执行退款
     */
    private RefundResponse executeRefund(PaymentTransactionEntity transaction,
                                         RefundRecordEntity refundRecord,
                                         RefundRequest request) throws AlipayApiException, WxPayException {
        PaymentPlatform platform = transaction.getPlatform();

        if (platform == PaymentPlatform.ALIPAY) {
            return executeAlipayRefund(transaction, refundRecord, request);
        } else if (platform == PaymentPlatform.WXPAY) {
            return executeWxPayRefund(transaction, refundRecord, request);
        } else {
            throw new BusinessException(ErrorCode.UNSUPPORTED_PLATFORM, "不支持的支付平台: " + platform);
        }
    }

    /**
     * 执行支付宝退款
     */
    private RefundResponse executeAlipayRefund(PaymentTransactionEntity transaction,
                                               RefundRecordEntity refundRecord,
                                               RefundRequest request) throws AlipayApiException {
        if (alipayService.isEmpty()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "支付宝服务未配置");
        }

        AlipayRefundRequest alipayRequest = new AlipayRefundRequest();
        alipayRequest.setOutTradeNo(transaction.getOrderId());
        alipayRequest.setTradeNo(transaction.getPlatformTradeNo());
        alipayRequest.setRefundAmount(request.getRefundAmount());
        alipayRequest.setRefundReason(request.getRefundReason());
        alipayRequest.setOutRequestNo(refundRecord.getRefundNo());

        AlipayRefundResponse alipayResponse = alipayService.get().refund(alipayRequest);

        boolean success = "10000".equals(alipayResponse.getCode());

        return RefundResponse.builder()
                .success(success)
                .refundNo(refundRecord.getRefundNo())
                .platformRefundNo(alipayResponse.getTradeNo())
                .refundAmount(request.getRefundAmount())
                .resultCode(alipayResponse.getCode())
                .resultMessage(success ? alipayResponse.getMsg() : alipayResponse.getSubMsg())
                .platform(PaymentPlatform.ALIPAY)
                .build();
    }

    /**
     * 执行微信退款
     */
    private RefundResponse executeWxPayRefund(PaymentTransactionEntity transaction,
                                              RefundRecordEntity refundRecord,
                                              RefundRequest request) throws WxPayException {
        if (wxPayService.isEmpty()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "微信支付服务未配置");
        }

        com.github.nonfou.mpay.payment.dto.wxpay.WxPayRefundRequest wxRequest =
                new com.github.nonfou.mpay.payment.dto.wxpay.WxPayRefundRequest();
        wxRequest.setOutTradeNo(transaction.getOrderId());
        wxRequest.setTransactionId(transaction.getPlatformTradeNo());
        wxRequest.setOutRefundNo(refundRecord.getRefundNo());
        wxRequest.setTotalFee(transaction.getAmount());
        wxRequest.setRefundFee(request.getRefundAmount());
        wxRequest.setRefundDesc(request.getRefundReason());

        WxPayRefundResponse wxResponse = wxPayService.get().refund(wxRequest);

        boolean success = "SUCCESS".equals(wxResponse.getReturnCode()) &&
                "SUCCESS".equals(wxResponse.getResultCode());

        return RefundResponse.builder()
                .success(success)
                .refundNo(refundRecord.getRefundNo())
                .platformRefundNo(wxResponse.getRefundId())
                .refundAmount(request.getRefundAmount())
                .resultCode(success ? "SUCCESS" : wxResponse.getErrCode())
                .resultMessage(success ? "退款成功" : wxResponse.getErrCodeDes())
                .platform(PaymentPlatform.WXPAY)
                .build();
    }

    /**
     * 更新交易退款金额
     */
    private void updateTransactionRefundAmount(PaymentTransactionEntity transaction, BigDecimal refundAmount) {
        BigDecimal currentRefunded = transaction.getRefundedAmount();
        if (currentRefunded == null) {
            currentRefunded = BigDecimal.ZERO;
        }
        BigDecimal newRefunded = currentRefunded.add(refundAmount);
        transaction.setRefundedAmount(newRefunded);

        // 判断是否全部退款
        if (newRefunded.compareTo(transaction.getAmount()) >= 0) {
            transaction.setStatus(TransactionStatus.REFUNDED);
        } else {
            transaction.setStatus(TransactionStatus.PARTIAL_REFUNDED);
        }

        transactionRepository.save(transaction);
    }

    /**
     * 发布退款事件
     */
    private void publishRefundEvent(PaymentTransactionEntity transaction,
                                    RefundRecordEntity refundRecord,
                                    RefundRequest request,
                                    RefundResponse response,
                                    long duration) {
        RefundEvent event = RefundEvent.builder()
                .source(this)
                .eventType(PaymentEventType.REFUND_CREATE)
                .platform(transaction.getPlatform())
                .orderId(transaction.getOrderId())
                .transactionId(transaction.getId())
                .refundNo(refundRecord.getRefundNo())
                .platformRefundNo(response.getPlatformRefundNo())
                .platformTradeNo(transaction.getPlatformTradeNo())
                .refundAmount(request.getRefundAmount())
                .refundReason(request.getRefundReason())
                .requestData(toJson(request))
                .responseData(toJson(response))
                .success(response.isSuccess())
                .resultCode(response.getResultCode())
                .resultMessage(response.getResultMessage())
                .durationMs(duration)
                .clientIp(request.getClientIp())
                .operator(request.getOperator())
                .build();

        eventPublisher.publishEvent(event);
    }

    /**
     * 根据退款单号查询退款记录
     */
    @Transactional(readOnly = true)
    public Optional<RefundRecordEntity> findByRefundNo(String refundNo) {
        return refundRecordRepository.findByRefundNo(refundNo);
    }

    /**
     * 根据订单号查询退款记录列表
     */
    @Transactional(readOnly = true)
    public List<RefundRecordEntity> findByOrderId(String orderId) {
        return refundRecordRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }

    /**
     * 根据交易ID查询退款记录列表
     */
    @Transactional(readOnly = true)
    public List<RefundRecordEntity> findByTransactionId(Long transactionId) {
        return refundRecordRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId);
    }

    /**
     * 分页查询退款记录
     */
    @Transactional(readOnly = true)
    public Page<RefundRecordEntity> findByConditions(String orderId,
                                                     String refundNo,
                                                     PaymentPlatform platform,
                                                     RefundStatus status,
                                                     LocalDateTime startTime,
                                                     LocalDateTime endTime,
                                                     Pageable pageable) {
        return refundRecordRepository.findByConditions(
                orderId, refundNo, platform, status, startTime, endTime, pageable);
    }

    /**
     * 转换为 JSON
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }
}
