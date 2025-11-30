package com.github.nonfou.mpay.transaction.event;

import com.github.nonfou.mpay.transaction.entity.PaymentEventLogEntity;
import com.github.nonfou.mpay.transaction.entity.PaymentTransactionEntity;
import com.github.nonfou.mpay.transaction.entity.RefundRecordEntity;
import com.github.nonfou.mpay.transaction.enums.PaymentEventType;
import com.github.nonfou.mpay.transaction.enums.RefundStatus;
import com.github.nonfou.mpay.transaction.enums.TransactionStatus;
import com.github.nonfou.mpay.transaction.repository.PaymentEventLogRepository;
import com.github.nonfou.mpay.transaction.repository.PaymentTransactionRepository;
import com.github.nonfou.mpay.transaction.repository.RefundRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付事件监听器 - 异步处理支付/退款事件，持久化数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final PaymentTransactionRepository transactionRepository;
    private final RefundRecordRepository refundRecordRepository;
    private final PaymentEventLogRepository eventLogRepository;

    /**
     * 处理支付事件
     */
    @Async
    @EventListener
    @Transactional
    public void handlePaymentEvent(PaymentEvent event) {
        log.info("处理支付事件: type={}, orderId={}, tradeNo={}",
                event.getEventType(), event.getOrderId(), event.getTradeNo());

        try {
            switch (event.getEventType()) {
                case CREATE -> handlePaymentCreate(event);
                case NOTIFY -> handlePaymentNotify(event);
                case QUERY -> handlePaymentQuery(event);
                case CLOSE -> handlePaymentClose(event);
                default -> log.warn("未知的支付事件类型: {}", event.getEventType());
            }

            // 记录事件日志
            saveEventLog(event);

        } catch (Exception e) {
            log.error("处理支付事件失败: type={}, orderId={}", event.getEventType(), event.getOrderId(), e);
            // 记录失败的事件日志
            saveEventLogWithError(event, e);
        }
    }

    /**
     * 处理退款事件
     */
    @Async
    @EventListener
    @Transactional
    public void handleRefundEvent(RefundEvent event) {
        log.info("处理退款事件: type={}, orderId={}, refundNo={}",
                event.getEventType(), event.getOrderId(), event.getRefundNo());

        try {
            switch (event.getEventType()) {
                case REFUND_CREATE -> handleRefundCreate(event);
                case REFUND_NOTIFY -> handleRefundNotify(event);
                case REFUND_QUERY -> handleRefundQuery(event);
                default -> log.warn("未知的退款事件类型: {}", event.getEventType());
            }

            // 记录事件日志
            saveRefundEventLog(event);

        } catch (Exception e) {
            log.error("处理退款事件失败: type={}, refundNo={}", event.getEventType(), event.getRefundNo(), e);
            // 记录失败的事件日志
            saveRefundEventLogWithError(event, e);
        }
    }

    /**
     * 处理支付创建事件
     */
    private void handlePaymentCreate(PaymentEvent event) {
        // 检查是否已存在
        if (transactionRepository.findByTradeNo(event.getTradeNo()).isPresent()) {
            log.warn("交易记录已存在，跳过创建: tradeNo={}", event.getTradeNo());
            return;
        }

        PaymentTransactionEntity transaction = new PaymentTransactionEntity();
        transaction.setOrderId(event.getOrderId());
        transaction.setTradeNo(event.getTradeNo());
        transaction.setPlatform(event.getPlatform());
        transaction.setTradeType(event.getTradeType());
        transaction.setAmount(event.getAmount());
        transaction.setSubject(event.getSubject());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setRawRequest(event.getRequestData());
        transaction.setRawResponse(event.getResponseData());
        transaction.setClientIp(event.getClientIp());
        transaction.setMerchantId(event.getMerchantId());
        transaction.setExtraData(event.getExtraData());

        transactionRepository.save(transaction);
        log.info("创建支付交易记录: id={}, tradeNo={}", transaction.getId(), transaction.getTradeNo());
    }

    /**
     * 处理支付回调通知事件
     */
    private void handlePaymentNotify(PaymentEvent event) {
        PaymentTransactionEntity transaction = transactionRepository
                .findByTradeNo(event.getTradeNo())
                .orElseGet(() -> {
                    // 如果找不到，尝试通过订单号查找
                    return transactionRepository.findByOrderId(event.getOrderId()).orElse(null);
                });

        if (transaction == null) {
            log.warn("未找到交易记录，创建新记录: orderId={}", event.getOrderId());
            // 创建新记录
            handlePaymentCreate(event);
            transaction = transactionRepository.findByTradeNo(event.getTradeNo()).orElse(null);
            if (transaction == null) {
                return;
            }
        }

        // 更新交易信息
        transaction.setPlatformTradeNo(event.getPlatformTradeNo());
        transaction.setNotifyData(event.getResponseData());

        if (Boolean.TRUE.equals(event.getSuccess())) {
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setPaidAt(LocalDateTime.now());
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
        }

        transactionRepository.save(transaction);
        log.info("更新支付交易状态: id={}, status={}", transaction.getId(), transaction.getStatus());
    }

    /**
     * 处理支付查询事件
     */
    private void handlePaymentQuery(PaymentEvent event) {
        // 查询事件主要用于记录日志，不更新主表状态
        // 除非查询结果显示状态变化
        if (event.getPlatformTradeNo() != null && Boolean.TRUE.equals(event.getSuccess())) {
            transactionRepository.findByTradeNo(event.getTradeNo())
                    .ifPresent(transaction -> {
                        if (transaction.getPlatformTradeNo() == null) {
                            transaction.setPlatformTradeNo(event.getPlatformTradeNo());
                            transactionRepository.save(transaction);
                        }
                    });
        }
    }

    /**
     * 处理关闭订单事件
     */
    private void handlePaymentClose(PaymentEvent event) {
        transactionRepository.findByTradeNo(event.getTradeNo())
                .ifPresent(transaction -> {
                    transaction.setStatus(TransactionStatus.CLOSED);
                    transactionRepository.save(transaction);
                    log.info("关闭交易: id={}, tradeNo={}", transaction.getId(), transaction.getTradeNo());
                });
    }

    /**
     * 处理退款创建事件
     */
    private void handleRefundCreate(RefundEvent event) {
        // 检查是否已存在
        if (refundRecordRepository.findByRefundNo(event.getRefundNo()).isPresent()) {
            log.warn("退款记录已存在，跳过创建: refundNo={}", event.getRefundNo());
            return;
        }

        RefundRecordEntity refund = new RefundRecordEntity();
        refund.setTransactionId(event.getTransactionId());
        refund.setOrderId(event.getOrderId());
        refund.setRefundNo(event.getRefundNo());
        refund.setPlatform(event.getPlatform());
        refund.setPlatformTradeNo(event.getPlatformTradeNo());
        refund.setRefundAmount(event.getRefundAmount());
        refund.setRefundReason(event.getRefundReason());
        refund.setStatus(Boolean.TRUE.equals(event.getSuccess()) ? RefundStatus.SUCCESS : RefundStatus.PENDING);
        refund.setRawRequest(event.getRequestData());
        refund.setRawResponse(event.getResponseData());
        refund.setOperator(event.getOperator());
        refund.setExtraData(event.getExtraData());

        if (Boolean.TRUE.equals(event.getSuccess())) {
            refund.setPlatformRefundNo(event.getPlatformRefundNo());
            refund.setRefundedAt(LocalDateTime.now());
        }

        refundRecordRepository.save(refund);
        log.info("创建退款记录: id={}, refundNo={}", refund.getId(), refund.getRefundNo());

        // 更新交易表的退款金额和状态
        if (event.getTransactionId() != null && Boolean.TRUE.equals(event.getSuccess())) {
            updateTransactionRefundAmount(event.getTransactionId(), event.getRefundAmount());
        }
    }

    /**
     * 处理退款回调通知事件
     */
    private void handleRefundNotify(RefundEvent event) {
        RefundRecordEntity refund = refundRecordRepository
                .findByRefundNo(event.getRefundNo())
                .orElse(null);

        if (refund == null) {
            log.warn("未找到退款记录: refundNo={}", event.getRefundNo());
            return;
        }

        refund.setPlatformRefundNo(event.getPlatformRefundNo());
        refund.setNotifyData(event.getResponseData());

        if (Boolean.TRUE.equals(event.getSuccess())) {
            refund.setStatus(RefundStatus.SUCCESS);
            refund.setRefundedAt(LocalDateTime.now());
            // 更新交易表的退款金额
            updateTransactionRefundAmount(refund.getTransactionId(), refund.getRefundAmount());
        } else {
            refund.setStatus(RefundStatus.FAILED);
        }

        refundRecordRepository.save(refund);
        log.info("更新退款状态: id={}, status={}", refund.getId(), refund.getStatus());
    }

    /**
     * 处理退款查询事件
     */
    private void handleRefundQuery(RefundEvent event) {
        // 查询事件主要用于记录日志
        // 可根据查询结果更新退款状态
    }

    /**
     * 更新交易的退款金额
     */
    private void updateTransactionRefundAmount(Long transactionId, BigDecimal refundAmount) {
        transactionRepository.findById(transactionId).ifPresent(transaction -> {
            BigDecimal currentRefunded = transaction.getRefundedAmount();
            if (currentRefunded == null) {
                currentRefunded = BigDecimal.ZERO;
            }
            BigDecimal newRefunded = currentRefunded.add(refundAmount);
            transaction.setRefundedAmount(newRefunded);

            // 判断是否全部退款
            if (newRefunded.compareTo(transaction.getAmount()) >= 0) {
                transaction.setStatus(TransactionStatus.REFUNDED);
            } else if (newRefunded.compareTo(BigDecimal.ZERO) > 0) {
                transaction.setStatus(TransactionStatus.PARTIAL_REFUNDED);
            }

            transactionRepository.save(transaction);
            log.info("更新交易退款金额: id={}, refundedAmount={}, status={}",
                    transaction.getId(), newRefunded, transaction.getStatus());
        });
    }

    /**
     * 保存支付事件日志
     */
    private void saveEventLog(PaymentEvent event) {
        PaymentEventLogEntity log = new PaymentEventLogEntity();
        log.setOrderId(event.getOrderId());
        log.setEventType(event.getEventType());
        log.setPlatform(event.getPlatform());
        log.setRequestData(event.getRequestData());
        log.setResponseData(event.getResponseData());
        log.setResultCode(event.getResultCode());
        log.setResultMessage(event.getResultMessage());
        log.setSuccess(event.getSuccess());
        log.setDurationMs(event.getDurationMs());
        log.setClientIp(event.getClientIp());
        log.setExtraData(event.getExtraData());

        // 关联交易ID
        if (event.getTradeNo() != null) {
            transactionRepository.findByTradeNo(event.getTradeNo())
                    .ifPresent(t -> log.setTransactionId(t.getId()));
        }

        eventLogRepository.save(log);
    }

    /**
     * 保存支付事件日志（带错误信息）
     */
    private void saveEventLogWithError(PaymentEvent event, Exception e) {
        PaymentEventLogEntity log = new PaymentEventLogEntity();
        log.setOrderId(event.getOrderId());
        log.setEventType(event.getEventType());
        log.setPlatform(event.getPlatform());
        log.setRequestData(event.getRequestData());
        log.setResponseData(event.getResponseData());
        log.setResultCode("ERROR");
        log.setResultMessage(e.getMessage());
        log.setSuccess(false);
        log.setDurationMs(event.getDurationMs());
        log.setClientIp(event.getClientIp());

        eventLogRepository.save(log);
    }

    /**
     * 保存退款事件日志
     */
    private void saveRefundEventLog(RefundEvent event) {
        PaymentEventLogEntity log = new PaymentEventLogEntity();
        log.setOrderId(event.getOrderId());
        log.setTransactionId(event.getTransactionId());
        log.setEventType(event.getEventType());
        log.setPlatform(event.getPlatform());
        log.setRequestData(event.getRequestData());
        log.setResponseData(event.getResponseData());
        log.setResultCode(event.getResultCode());
        log.setResultMessage(event.getResultMessage());
        log.setSuccess(event.getSuccess());
        log.setDurationMs(event.getDurationMs());
        log.setClientIp(event.getClientIp());
        log.setExtraData(event.getExtraData());

        // 关联退款ID
        if (event.getRefundNo() != null) {
            refundRecordRepository.findByRefundNo(event.getRefundNo())
                    .ifPresent(r -> log.setRefundId(r.getId()));
        }

        eventLogRepository.save(log);
    }

    /**
     * 保存退款事件日志（带错误信息）
     */
    private void saveRefundEventLogWithError(RefundEvent event, Exception e) {
        PaymentEventLogEntity log = new PaymentEventLogEntity();
        log.setOrderId(event.getOrderId());
        log.setTransactionId(event.getTransactionId());
        log.setEventType(event.getEventType());
        log.setPlatform(event.getPlatform());
        log.setRequestData(event.getRequestData());
        log.setResponseData(event.getResponseData());
        log.setResultCode("ERROR");
        log.setResultMessage(e.getMessage());
        log.setSuccess(false);
        log.setDurationMs(event.getDurationMs());
        log.setClientIp(event.getClientIp());

        eventLogRepository.save(log);
    }
}
