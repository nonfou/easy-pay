package com.mapy.payment.service.impl;

import com.mapy.payment.entity.OrderEntity;
import com.mapy.payment.repository.OrderRepository;
import com.mapy.payment.service.PriceAllocator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class IncrementalPriceAllocator implements PriceAllocator {

    private final OrderRepository orderRepository;

    public IncrementalPriceAllocator(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public BigDecimal allocate(BigDecimal target, Long aid, Long cid, String type) {
        BigDecimal price = target.setScale(2, RoundingMode.HALF_UP);
        List<OrderEntity> activeOrders = orderRepository.findByAidAndCidAndTypeAndState(aid, cid, type, 0);
        Set<Double> exists = new HashSet<>();
        for (OrderEntity order : activeOrders) {
            exists.add(order.getReallyPrice());
        }
        while (exists.contains(price.doubleValue())) {
            price = price.add(BigDecimal.valueOf(0.01));
        }
        return price;
    }
}
