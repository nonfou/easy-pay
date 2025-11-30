package com.github.nonfou.mpay.transaction.service;

import com.github.nonfou.mpay.transaction.entity.PaymentEventLogEntity;
import com.github.nonfou.mpay.transaction.enums.PaymentEventType;
import com.github.nonfou.mpay.transaction.enums.PaymentPlatform;
import com.github.nonfou.mpay.transaction.repository.PaymentEventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付事件日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventLogService {

    private final PaymentEventLogRepository eventLogRepository;

    /**
     * 根据订单号查询所有事件日志（按时间正序）
     */
    @Transactional(readOnly = true)
    public List<PaymentEventLogEntity> findByOrderId(String orderId) {
        return eventLogRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    /**
     * 根据交易ID查询所有事件日志
     */
    @Transactional(readOnly = true)
    public List<PaymentEventLogEntity> findByTransactionId(Long transactionId) {
        return eventLogRepository.findByTransactionIdOrderByCreatedAtAsc(transactionId);
    }

    /**
     * 根据退款ID查询所有事件日志
     */
    @Transactional(readOnly = true)
    public List<PaymentEventLogEntity> findByRefundId(Long refundId) {
        return eventLogRepository.findByRefundIdOrderByCreatedAtAsc(refundId);
    }

    /**
     * 分页查询事件日志
     */
    @Transactional(readOnly = true)
    public Page<PaymentEventLogEntity> findByConditions(
            String orderId,
            Long transactionId,
            PaymentEventType eventType,
            PaymentPlatform platform,
            Boolean success,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable) {
        return eventLogRepository.findByConditions(
                orderId, transactionId, eventType, platform, success, startTime, endTime, pageable);
    }

    /**
     * 查询最近的失败事件
     */
    @Transactional(readOnly = true)
    public List<PaymentEventLogEntity> findRecentFailedEvents(int minutes) {
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(minutes);
        return eventLogRepository.findFailedEventsSince(startTime);
    }

    /**
     * 统计某时间段内的事件数量
     */
    @Transactional(readOnly = true)
    public long countByEventTypeAndTimeRange(PaymentEventType eventType, LocalDateTime startTime, LocalDateTime endTime) {
        return eventLogRepository.countByEventTypeAndTimeRange(eventType, startTime, endTime);
    }

    /**
     * 手动创建事件日志
     */
    @Transactional
    public PaymentEventLogEntity createEventLog(PaymentEventLogEntity eventLog) {
        return eventLogRepository.save(eventLog);
    }
}
