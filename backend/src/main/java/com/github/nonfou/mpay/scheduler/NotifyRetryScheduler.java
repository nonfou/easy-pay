package com.github.nonfou.mpay.scheduler;

import com.github.nonfou.mpay.entity.OrderEntity;
import com.github.nonfou.mpay.entity.OrderNotifyLogEntity;
import com.github.nonfou.mpay.notify.NotifyClient;
import com.github.nonfou.mpay.repository.OrderRepository;
import com.github.nonfou.mpay.service.NotifyLogService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 通知重试定时任务
 */
@Component
public class NotifyRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotifyRetryScheduler.class);

    private final NotifyLogService notifyLogService;
    private final OrderRepository orderRepository;
    private final NotifyClient notifyClient;

    public NotifyRetryScheduler(NotifyLogService notifyLogService,
            OrderRepository orderRepository,
            NotifyClient notifyClient) {
        this.notifyLogService = notifyLogService;
        this.orderRepository = orderRepository;
        this.notifyClient = notifyClient;
    }

    /**
     * 重试失败的通知 - 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000)
    public void retryFailedNotifications() {
        List<OrderNotifyLogEntity> pendingLogs = notifyLogService.getPendingRetries();
        if (pendingLogs.isEmpty()) {
            return;
        }
        log.info("开始重试 {} 条失败通知", pendingLogs.size());
        for (OrderNotifyLogEntity notifyLog : pendingLogs) {
            retryNotification(notifyLog);
        }
    }

    private void retryNotification(OrderNotifyLogEntity notifyLog) {
        try {
            OrderEntity order = orderRepository.findByOrderId(notifyLog.getOrderId())
                    .orElse(null);
            if (order == null) {
                log.warn("订单 {} 不存在，跳过重试", notifyLog.getOrderId());
                notifyLogService.updateRetryResult(notifyLog, false, "订单不存在");
                return;
            }
            log.info("重试通知订单 {} (第 {} 次)", order.getOrderId(), notifyLog.getRetryCount() + 1);
            boolean success = notifyClient.sendNotification(order);
            notifyLogService.updateRetryResult(notifyLog, success, success ? null : "通知失败");
            if (success) {
                log.info("订单 {} 通知重试成功", order.getOrderId());
            }
        } catch (Exception e) {
            log.error("订单 {} 通知重试异常: {}", notifyLog.getOrderId(), e.getMessage());
            notifyLogService.updateRetryResult(notifyLog, false, e.getMessage());
        }
    }
}
