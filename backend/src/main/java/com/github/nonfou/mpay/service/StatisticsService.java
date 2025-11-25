package com.github.nonfou.mpay.service;

import com.github.nonfou.mpay.dto.statistics.OrderTrendDTO;
import com.github.nonfou.mpay.dto.statistics.PaymentTypeStatisticsDTO;
import com.github.nonfou.mpay.dto.statistics.RevenueStatisticsDTO;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 统计分析服务
 */
public interface StatisticsService {

    /**
     * 获取收入统计
     * @param pid 商户ID (可选，为空则查全部)
     * @return 收入统计数据
     */
    RevenueStatisticsDTO getRevenueStatistics(Long pid);

    /**
     * 按支付类型统计
     * @param pid 商户ID (可选)
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 各支付类型统计
     */
    List<PaymentTypeStatisticsDTO> getPaymentTypeStatistics(Long pid, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取订单趋势（最近N天）
     * @param pid 商户ID (可选)
     * @param days 天数
     * @return 每日订单趋势
     */
    List<OrderTrendDTO> getOrderTrend(Long pid, int days);
}
