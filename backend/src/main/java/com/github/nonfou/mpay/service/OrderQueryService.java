package com.github.nonfou.mpay.service;

import com.github.nonfou.mpay.dto.order.OrderSummaryDTO;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单查询服务 - P1 功能
 */
public interface OrderQueryService {

    /**
     * 查询活跃订单（有效期内未支付）
     * @param pid 商户ID (可选)
     * @param expireMinutes 有效期（分钟），默认3分钟
     * @return 活跃订单列表
     */
    List<OrderSummaryDTO> findActiveOrders(Long pid, Integer expireMinutes);

    /**
     * 查询成交订单
     * @param pid 商户ID (可选)
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 成交订单列表
     */
    List<OrderSummaryDTO> findSuccessOrders(Long pid, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询超时订单
     * @param pid 商户ID (可选)
     * @param expireMinutes 有效期（分钟），默认3分钟
     * @return 超时订单列表
     */
    List<OrderSummaryDTO> findExpiredOrders(Long pid, Integer expireMinutes);
}
