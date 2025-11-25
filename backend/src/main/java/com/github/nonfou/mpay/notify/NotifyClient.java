package com.github.nonfou.mpay.notify;

import com.github.nonfou.mpay.entity.OrderEntity;

public interface NotifyClient {

    /**
     * 通知商户订单支付成功 (包含内部重试)
     */
    void notifyMerchant(OrderEntity order);

    /**
     * 发送单次通知 (不重试，用于定时任务重试场景)
     * @return 是否成功
     */
    boolean sendNotification(OrderEntity order);
}
