package com.github.nonfou.mpay.service.impl;

import com.github.nonfou.mpay.entity.OrderEntity;
import com.github.nonfou.mpay.entity.OrderNotifyLogEntity;
import com.github.nonfou.mpay.repository.OrderNotifyLogRepository;
import com.github.nonfou.mpay.service.NotifyLogService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class NotifyLogServiceImpl implements NotifyLogService {

    private final OrderNotifyLogRepository repository;

    public NotifyLogServiceImpl(OrderNotifyLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void recordFailure(OrderEntity order, String error, int retries) {
        OrderNotifyLogEntity log = new OrderNotifyLogEntity();
        log.setOrderId(order.getOrderId());
        log.setStatus(0);
        log.setRetryCount(retries);
        log.setLastError(error);
        log.setNextRetryTime(LocalDateTime.now().plusMinutes(5));
        log.setCreatedAt(LocalDateTime.now());
        log.setUpdatedAt(LocalDateTime.now());
        repository.save(log);
    }
}
