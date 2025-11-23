package com.mapy.payment.event;

import com.mapy.payment.entity.OrderEntity;

public interface OrderEventPublisher {

    void publish(OrderEntity order);
}
