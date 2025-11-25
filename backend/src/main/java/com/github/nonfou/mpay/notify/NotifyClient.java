package com.github.nonfou.mpay.notify;

import com.github.nonfou.mpay.entity.OrderEntity;

public interface NotifyClient {

    void notifyMerchant(OrderEntity order);
}
