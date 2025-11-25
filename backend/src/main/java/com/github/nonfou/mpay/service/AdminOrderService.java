package com.github.nonfou.mpay.service;

import com.github.nonfou.mpay.entity.OrderEntity;

/**
 * 后台订单管理服务 - P0 功能
 */
public interface AdminOrderService {

    /**
     * 手动补单 - 将订单状态更新为已支付并发送通知
     * @param orderId 订单号
     * @param remark 补单原因
     * @return 更新后的订单
     */
    OrderEntity manualSettle(String orderId, String remark);

    /**
     * 重新发送通知 - 对已支付订单重新发送商户通知
     * @param orderId 订单号
     * @return 是否发送成功
     */
    boolean renotify(String orderId);

    /**
     * 批量清理超时订单
     * @param expireMinutes 超时时间（分钟），默认为3分钟
     * @return 删除的订单数量
     */
    int cleanExpiredOrders(Integer expireMinutes);
}
