package com.github.nonfou.mpay.service.impl;

import com.github.nonfou.mpay.dto.order.OrderSummaryDTO;
import com.github.nonfou.mpay.entity.OrderEntity;
import com.github.nonfou.mpay.repository.OrderRepository;
import com.github.nonfou.mpay.service.OrderQueryService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OrderQueryServiceImpl implements OrderQueryService {

    private static final int DEFAULT_EXPIRE_MINUTES = 3;

    private static final java.util.Map<Integer, String> STATE_NAMES = java.util.Map.of(
            0, "待支付",
            1, "已支付",
            2, "已关闭"
    );

    private final OrderRepository orderRepository;

    public OrderQueryServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public List<OrderSummaryDTO> findActiveOrders(Long pid, Integer expireMinutes) {
        int minutes = expireMinutes != null ? expireMinutes : DEFAULT_EXPIRE_MINUTES;
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(minutes);

        List<OrderEntity> orders = orderRepository.findActiveOrders(pid, expireTime);
        return orders.stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderSummaryDTO> findSuccessOrders(Long pid, LocalDateTime startTime, LocalDateTime endTime) {
        List<OrderEntity> orders = orderRepository.findSuccessOrders(pid, startTime, endTime);
        return orders.stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderSummaryDTO> findExpiredOrders(Long pid, Integer expireMinutes) {
        int minutes = expireMinutes != null ? expireMinutes : DEFAULT_EXPIRE_MINUTES;
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(minutes);

        List<OrderEntity> orders = orderRepository.findExpiredOrders(pid, expireTime);
        return orders.stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    private OrderSummaryDTO toSummaryDTO(OrderEntity entity) {
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(DEFAULT_EXPIRE_MINUTES);
        boolean isExpired = entity.getState() == 0 && entity.getCreateTime().isBefore(expireTime);

        return OrderSummaryDTO.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .outTradeNo(entity.getOutTradeNo())
                .pid(entity.getPid())
                .type(entity.getType())
                .name(entity.getName())
                .money(entity.getMoney())
                .reallyPrice(entity.getReallyPrice())
                .state(entity.getState())
                .stateName(STATE_NAMES.getOrDefault(entity.getState(), "未知"))
                .createTime(entity.getCreateTime())
                .payTime(entity.getPayTime())
                .closeTime(entity.getCloseTime())
                .expired(isExpired)
                .build();
    }
}
