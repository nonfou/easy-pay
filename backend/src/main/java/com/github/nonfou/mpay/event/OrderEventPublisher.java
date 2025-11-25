package com.github.nonfou.mpay.event;

import com.github.nonfou.mpay.entity.OrderEntity;

public interface OrderEventPublisher {

    void publish(OrderEntity order);
}
