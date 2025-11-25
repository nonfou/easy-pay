package com.github.nonfou.mpay.service.impl;

import com.github.nonfou.mpay.entity.OrderEntity;
import com.github.nonfou.mpay.repository.OrderRepository;
import com.github.nonfou.mpay.service.PriceAllocator;
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
        Set<BigDecimal> exists = new HashSet<>();
        for (OrderEntity order : activeOrders) {
            exists.add(order.getReallyPrice());
        }
        while (exists.contains(price)) {
            price = price.add(BigDecimal.valueOf(0.01));
        }
        return price;
    }
}
