package com.github.nonfou.mpay.service.impl;

import com.github.nonfou.mpay.entity.OrderEntity;
import com.github.nonfou.mpay.entity.OrderNotifyLogEntity;
import com.github.nonfou.mpay.repository.OrderNotifyLogRepository;
import com.github.nonfou.mpay.service.NotifyLogService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NotifyLogServiceImpl implements NotifyLogService {

    private static final int[] RETRY_INTERVALS = {5, 15, 30, 60, 120, 360, 720, 1440};

    private final OrderNotifyLogRepository repository;

    @Value("${mpay.notify.max-retries:8}")
    private int maxRetries;

    public NotifyLogServiceImpl(OrderNotifyLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void recordFailure(OrderEntity order, String error, int retries) {
        OrderNotifyLogEntity log = repository.findByOrderId(order.getOrderId())
                .orElseGet(() -> {
                    OrderNotifyLogEntity newLog = new OrderNotifyLogEntity();
                    newLog.setOrderId(order.getOrderId());
                    newLog.setRetryCount(0);
                    newLog.setCreatedAt(LocalDateTime.now());
                    return newLog;
                });
        log.setStatus(0);
        log.setRetryCount(retries);
        log.setLastError(error);
        log.setNextRetryTime(calculateNextRetryTime(retries));
        log.setUpdatedAt(LocalDateTime.now());
        repository.save(log);
    }

    @Override
    public List<OrderNotifyLogEntity> getPendingRetries() {
        return repository.findPendingRetries(LocalDateTime.now(), maxRetries);
    }

    @Override
    public void updateRetryResult(OrderNotifyLogEntity log, boolean success, String error) {
        if (success) {
            log.setStatus(1);
            log.setLastError(null);
            log.setNextRetryTime(null);
        } else {
            int newRetryCount = log.getRetryCount() + 1;
            log.setRetryCount(newRetryCount);
            log.setLastError(error);
            if (newRetryCount >= maxRetries) {
                log.setStatus(2); // 标记为最终失败
                log.setNextRetryTime(null);
            } else {
                log.setNextRetryTime(calculateNextRetryTime(newRetryCount));
            }
        }
        log.setUpdatedAt(LocalDateTime.now());
        repository.save(log);
    }

    private LocalDateTime calculateNextRetryTime(int retryCount) {
        int index = Math.min(retryCount, RETRY_INTERVALS.length - 1);
        int minutes = RETRY_INTERVALS[index];
        return LocalDateTime.now().plusMinutes(minutes);
    }
}
