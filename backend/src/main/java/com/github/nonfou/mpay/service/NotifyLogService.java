package com.github.nonfou.mpay.service;

import com.github.nonfou.mpay.entity.OrderEntity;

public interface NotifyLogService {

    void recordFailure(OrderEntity order, String error, int retries);
}
