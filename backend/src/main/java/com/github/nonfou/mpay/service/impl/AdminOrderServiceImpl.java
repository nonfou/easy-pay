package com.github.nonfou.mpay.service.impl;

import com.github.nonfou.mpay.common.error.BusinessException;
import com.github.nonfou.mpay.common.error.ErrorCode;
import com.github.nonfou.mpay.entity.OrderEntity;
import com.github.nonfou.mpay.notify.NotifyClient;
import com.github.nonfou.mpay.repository.OrderRepository;
import com.github.nonfou.mpay.service.AdminOrderService;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminOrderServiceImpl implements AdminOrderService {

    private static final Logger log = LoggerFactory.getLogger(AdminOrderServiceImpl.class);
    private static final int DEFAULT_EXPIRE_MINUTES = 3;

    private final OrderRepository orderRepository;
    private final NotifyClient notifyClient;

    public AdminOrderServiceImpl(OrderRepository orderRepository, NotifyClient notifyClient) {
        this.orderRepository = orderRepository;
        this.notifyClient = notifyClient;
    }

    @Override
    @Transactional
    public OrderEntity manualSettle(String orderId, String remark) {
        OrderEntity order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在: " + orderId));

        if (order.getState() == 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "订单已支付，无需补单");
        }

        // 更新订单状态
        order.setState(1);
        order.setPayTime(LocalDateTime.now());
        orderRepository.save(order);

        log.info("手动补单成功: orderId={}, remark={}", orderId, remark);

        // 发送商户通知
        try {
            notifyClient.notifyMerchant(order);
            log.info("补单通知发送成功: orderId={}", orderId);
        } catch (Exception e) {
            log.error("补单通知发送失败: orderId={}, error={}", orderId, e.getMessage());
            // 通知失败不影响补单结果
        }

        return order;
    }

    @Override
    public boolean renotify(String orderId) {
        OrderEntity order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在: " + orderId));

        if (order.getState() != 1) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "只有已支付订单可以重新通知");
        }

        try {
            notifyClient.notifyMerchant(order);
            log.info("重新通知发送成功: orderId={}", orderId);
            return true;
        } catch (Exception e) {
            log.error("重新通知发送失败: orderId={}, error={}", orderId, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public int cleanExpiredOrders(Integer expireMinutes) {
        int minutes = expireMinutes != null ? expireMinutes : DEFAULT_EXPIRE_MINUTES;
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(minutes);

        int deletedCount = orderRepository.deleteExpiredOrders(expireTime);
        log.info("清理超时订单完成: 删除数量={}, 超时时间={}分钟", deletedCount, minutes);

        return deletedCount;
    }
}
