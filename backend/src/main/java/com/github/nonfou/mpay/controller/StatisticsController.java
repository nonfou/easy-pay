package com.github.nonfou.mpay.controller;

import com.github.nonfou.mpay.common.response.ApiResponse;
import com.github.nonfou.mpay.dto.statistics.OrderTrendDTO;
import com.github.nonfou.mpay.dto.statistics.PaymentTypeStatisticsDTO;
import com.github.nonfou.mpay.dto.statistics.RevenueStatisticsDTO;
import com.github.nonfou.mpay.service.StatisticsService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计分析接口 - P1 功能
 */
@RestController
@RequestMapping("/api/console/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * 获取收入统计
     * GET /api/console/statistics/revenue
     */
    @GetMapping("/revenue")
    public ApiResponse<RevenueStatisticsDTO> getRevenueStatistics(
            @RequestParam(required = false) Long pid) {
        return ApiResponse.success(statisticsService.getRevenueStatistics(pid));
    }

    /**
     * 按支付类型统计
     * GET /api/console/statistics/payment-types
     */
    @GetMapping("/payment-types")
    public ApiResponse<List<PaymentTypeStatisticsDTO>> getPaymentTypeStatistics(
            @RequestParam(required = false) Long pid,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        LocalDateTime startTime;
        LocalDateTime endTime;

        if (startDate != null && endDate != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            startTime = LocalDate.parse(startDate, formatter).atStartOfDay();
            endTime = LocalDate.parse(endDate, formatter).plusDays(1).atStartOfDay();
        } else {
            // 默认查询本月
            LocalDate today = LocalDate.now();
            startTime = today.withDayOfMonth(1).atStartOfDay();
            endTime = today.plusDays(1).atStartOfDay();
        }

        return ApiResponse.success(statisticsService.getPaymentTypeStatistics(pid, startTime, endTime));
    }

    /**
     * 获取订单趋势
     * GET /api/console/statistics/trend
     */
    @GetMapping("/trend")
    public ApiResponse<List<OrderTrendDTO>> getOrderTrend(
            @RequestParam(required = false) Long pid,
            @RequestParam(defaultValue = "7") int days) {
        // 限制最大天数
        if (days > 90) {
            days = 90;
        }
        return ApiResponse.success(statisticsService.getOrderTrend(pid, days));
    }
}
