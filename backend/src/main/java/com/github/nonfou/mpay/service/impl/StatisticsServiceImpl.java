package com.github.nonfou.mpay.service.impl;

import com.github.nonfou.mpay.dto.statistics.OrderTrendDTO;
import com.github.nonfou.mpay.dto.statistics.PaymentTypeStatisticsDTO;
import com.github.nonfou.mpay.dto.statistics.RevenueStatisticsDTO;
import com.github.nonfou.mpay.repository.OrderRepository;
import com.github.nonfou.mpay.service.StatisticsService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final Map<String, String> TYPE_NAMES = Map.of(
            "alipay", "支付宝",
            "wxpay", "微信支付",
            "unionpay", "银联支付",
            "qqpay", "QQ钱包"
    );

    private final OrderRepository orderRepository;

    public StatisticsServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public RevenueStatisticsDTO getRevenueStatistics(Long pid) {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();

        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();
        LocalDateTime yesterdayEnd = todayStart;

        LocalDateTime weekStart = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime monthStart = today.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();

        // 今日统计
        Double todayRevenue = orderRepository.sumRevenueByTimeRange(pid, todayStart, todayEnd);
        Long todaySuccessCount = orderRepository.countSuccessOrdersByTimeRange(pid, todayStart, todayEnd);
        Long todayAllCount = orderRepository.countAllOrdersByTimeRange(pid, todayStart, todayEnd);

        // 昨日统计
        Double yesterdayRevenue = orderRepository.sumRevenueByTimeRange(pid, yesterdayStart, yesterdayEnd);
        Long yesterdaySuccessCount = orderRepository.countSuccessOrdersByTimeRange(pid, yesterdayStart, yesterdayEnd);

        // 本周统计
        Double weekRevenue = orderRepository.sumRevenueByTimeRange(pid, weekStart, todayEnd);
        Long weekSuccessCount = orderRepository.countSuccessOrdersByTimeRange(pid, weekStart, todayEnd);

        // 本月统计
        Double monthRevenue = orderRepository.sumRevenueByTimeRange(pid, monthStart, todayEnd);
        Long monthSuccessCount = orderRepository.countSuccessOrdersByTimeRange(pid, monthStart, todayEnd);

        // 计算成功率
        double successRate = todayAllCount > 0 ? (double) todaySuccessCount / todayAllCount * 100 : 0;

        return RevenueStatisticsDTO.builder()
                .todayRevenue(todayRevenue != null ? todayRevenue : 0.0)
                .yesterdayRevenue(yesterdayRevenue != null ? yesterdayRevenue : 0.0)
                .weekRevenue(weekRevenue != null ? weekRevenue : 0.0)
                .monthRevenue(monthRevenue != null ? monthRevenue : 0.0)
                .todayOrderCount(todaySuccessCount != null ? todaySuccessCount : 0L)
                .yesterdayOrderCount(yesterdaySuccessCount != null ? yesterdaySuccessCount : 0L)
                .weekOrderCount(weekSuccessCount != null ? weekSuccessCount : 0L)
                .monthOrderCount(monthSuccessCount != null ? monthSuccessCount : 0L)
                .todaySuccessRate(Math.round(successRate * 100.0) / 100.0)
                .build();
    }

    @Override
    public List<PaymentTypeStatisticsDTO> getPaymentTypeStatistics(Long pid, LocalDateTime startTime,
            LocalDateTime endTime) {
        List<Object[]> results = orderRepository.countByPaymentType(pid, startTime, endTime);

        // 计算总金额用于百分比
        double totalAmount = results.stream()
                .mapToDouble(row -> ((Number) row[2]).doubleValue())
                .sum();

        List<PaymentTypeStatisticsDTO> statistics = new ArrayList<>();
        for (Object[] row : results) {
            String type = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            Double amount = ((Number) row[2]).doubleValue();
            double percentage = totalAmount > 0 ? amount / totalAmount * 100 : 0;

            statistics.add(PaymentTypeStatisticsDTO.builder()
                    .type(type)
                    .typeName(TYPE_NAMES.getOrDefault(type, type))
                    .orderCount(count)
                    .totalAmount(amount)
                    .percentage(Math.round(percentage * 100.0) / 100.0)
                    .build());
        }

        return statistics;
    }

    @Override
    public List<OrderTrendDTO> getOrderTrend(Long pid, int days) {
        LocalDateTime startTime = LocalDate.now().minusDays(days - 1).atStartOfDay();
        List<Object[]> results = orderRepository.getOrderTrendByDate(pid, startTime);

        // 构建日期映射
        Map<LocalDate, OrderTrendDTO> trendMap = new HashMap<>();
        for (Object[] row : results) {
            LocalDate date = (LocalDate) row[0];
            Long orderCount = ((Number) row[1]).longValue();
            Long successCount = ((Number) row[2]).longValue();
            Double totalAmount = ((Number) row[3]).doubleValue();

            trendMap.put(date, OrderTrendDTO.builder()
                    .date(date)
                    .orderCount(orderCount)
                    .successCount(successCount)
                    .totalAmount(totalAmount)
                    .build());
        }

        // 填充所有日期（包括无数据的日期）
        List<OrderTrendDTO> trends = new ArrayList<>();
        LocalDate date = LocalDate.now().minusDays(days - 1);
        for (int i = 0; i < days; i++) {
            OrderTrendDTO trend = trendMap.getOrDefault(date, OrderTrendDTO.builder()
                    .date(date)
                    .orderCount(0L)
                    .successCount(0L)
                    .totalAmount(0.0)
                    .build());
            trends.add(trend);
            date = date.plusDays(1);
        }

        return trends;
    }
}
