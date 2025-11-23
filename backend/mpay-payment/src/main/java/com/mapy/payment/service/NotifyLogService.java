package com.mapy.payment.service;

import com.mapy.payment.entity.OrderEntity;

public interface NotifyLogService {

    void recordFailure(OrderEntity order, String error, int retries);
}
