package com.github.nonfou.mpay.scheduler;

import com.github.nonfou.mpay.repository.OrderRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单相关定时任务
 */
@Component
public class OrderScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderScheduler.class);

    private final OrderRepository orderRepository;

    @Value("${mpay.order.expire-minutes:30}")
    private int expireMinutes;

    public OrderScheduler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * 清理过期订单 - 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanExpiredOrders() {
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(expireMinutes);
        long count = orderRepository.countExpiredOrders(expireTime);
        if (count > 0) {
            int deleted = orderRepository.deleteExpiredOrders(expireTime);
            log.info("已清理 {} 条过期订单 (超过 {} 分钟未支付)", deleted, expireMinutes);
        }
    }
}
