package com.mapy.payment.notify;

import com.mapy.payment.entity.OrderEntity;

public interface NotifyClient {

    void notifyMerchant(OrderEntity order);
}
