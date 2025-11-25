package com.github.nonfou.mpay.service;

import com.github.nonfou.mpay.entity.OrderEntity;
import com.github.nonfou.mpay.entity.OrderNotifyLogEntity;
import java.util.List;

public interface NotifyLogService {

    void recordFailure(OrderEntity order, String error, int retries);

    /**
     * 获取待重试的通知记录
     */
    List<OrderNotifyLogEntity> getPendingRetries();

    /**
     * 更新重试结果
     */
    void updateRetryResult(OrderNotifyLogEntity log, boolean success, String error);
}
