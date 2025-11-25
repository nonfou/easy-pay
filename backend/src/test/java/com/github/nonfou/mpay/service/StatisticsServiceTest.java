package com.github.nonfou.mpay.service;

import com.github.nonfou.mpay.dto.statistics.OrderTrendDTO;
import com.github.nonfou.mpay.dto.statistics.PaymentTypeStatisticsDTO;
import com.github.nonfou.mpay.dto.statistics.RevenueStatisticsDTO;
import com.github.nonfou.mpay.repository.OrderRepository;
import com.github.nonfou.mpay.service.impl.StatisticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * StatisticsService 单元测试
 * 测试统计分析功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StatisticsService 统计服务测试")
class StatisticsServiceTest {

    @Mock
    private OrderRepository orderRepository;

    private StatisticsServiceImpl statisticsService;

    @BeforeEach
    void setUp() {
        statisticsService = new StatisticsServiceImpl(orderRepository);
    }

    // ==================== 收入统计测试 ====================

    @Test
    @DisplayName("获取收入统计 - 正常数据")
    void getRevenueStatistics_shouldReturnCorrectStats() {
        // Given
        Long pid = 1001L;

        // Mock 今日数据
        when(orderRepository.sumRevenueByTimeRange(eq(pid), any(), any()))
                .thenReturn(1000.0, 800.0, 5000.0, 20000.0); // 今日、昨日、本周、本月
        when(orderRepository.countSuccessOrdersByTimeRange(eq(pid), any(), any()))
                .thenReturn(10L, 8L, 50L, 200L);
        when(orderRepository.countAllOrdersByTimeRange(eq(pid), any(), any()))
                .thenReturn(12L);

        // When
        RevenueStatisticsDTO result = statisticsService.getRevenueStatistics(pid);

        // Then
        assertNotNull(result);
        assertEquals(1000.0, result.getTodayRevenue());
        assertEquals(800.0, result.getYesterdayRevenue());
        assertEquals(5000.0, result.getWeekRevenue());
        assertEquals(20000.0, result.getMonthRevenue());
        assertEquals(10L, result.getTodayOrderCount());
        assertEquals(8L, result.getYesterdayOrderCount());
        assertEquals(50L, result.getWeekOrderCount());
        assertEquals(200L, result.getMonthOrderCount());
        // 成功率: 10/12 = 83.33%
        assertTrue(result.getTodaySuccessRate() > 83 && result.getTodaySuccessRate() < 84);
    }

    @Test
    @DisplayName("获取收入统计 - 无数据时返回0")
    void getRevenueStatistics_shouldReturnZeros_whenNoData() {
        // Given
        Long pid = 1001L;

        when(orderRepository.sumRevenueByTimeRange(eq(pid), any(), any()))
                .thenReturn(null);
        when(orderRepository.countSuccessOrdersByTimeRange(eq(pid), any(), any()))
                .thenReturn(null);
        when(orderRepository.countAllOrdersByTimeRange(eq(pid), any(), any()))
                .thenReturn(0L);

        // When
        RevenueStatisticsDTO result = statisticsService.getRevenueStatistics(pid);

        // Then
        assertNotNull(result);
        assertEquals(0.0, result.getTodayRevenue());
        assertEquals(0L, result.getTodayOrderCount());
        assertEquals(0.0, result.getTodaySuccessRate());
    }

    @Test
    @DisplayName("获取收入统计 - 全部商户(pid=null)")
    void getRevenueStatistics_shouldWorkForAllMerchants_whenPidIsNull() {
        // Given
        when(orderRepository.sumRevenueByTimeRange(isNull(), any(), any()))
                .thenReturn(5000.0);
        when(orderRepository.countSuccessOrdersByTimeRange(isNull(), any(), any()))
                .thenReturn(50L);
        when(orderRepository.countAllOrdersByTimeRange(isNull(), any(), any()))
                .thenReturn(60L);

        // When
        RevenueStatisticsDTO result = statisticsService.getRevenueStatistics(null);

        // Then
        assertNotNull(result);
        verify(orderRepository, atLeastOnce()).sumRevenueByTimeRange(isNull(), any(), any());
    }

    // ==================== 支付类型统计测试 ====================

    @Test
    @DisplayName("获取支付类型统计 - 多种支付类型")
    void getPaymentTypeStatistics_shouldReturnCorrectStats() {
        // Given
        Long pid = 1001L;
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = LocalDateTime.now();

        List<Object[]> mockResults = new ArrayList<>();
        mockResults.add(new Object[]{"wxpay", 100L, 5000.0});
        mockResults.add(new Object[]{"alipay", 80L, 4000.0});
        mockResults.add(new Object[]{"unionpay", 20L, 1000.0});

        when(orderRepository.countByPaymentType(eq(pid), any(), any()))
                .thenReturn(mockResults);

        // When
        List<PaymentTypeStatisticsDTO> result = statisticsService.getPaymentTypeStatistics(pid, startTime, endTime);

        // Then
        assertEquals(3, result.size());

        // 微信支付
        PaymentTypeStatisticsDTO wxpay = result.stream()
                .filter(s -> "wxpay".equals(s.getType()))
                .findFirst().orElse(null);
        assertNotNull(wxpay);
        assertEquals("微信支付", wxpay.getTypeName());
        assertEquals(100L, wxpay.getOrderCount());
        assertEquals(5000.0, wxpay.getTotalAmount());
        assertEquals(50.0, wxpay.getPercentage()); // 5000/10000 = 50%

        // 支付宝
        PaymentTypeStatisticsDTO alipay = result.stream()
                .filter(s -> "alipay".equals(s.getType()))
                .findFirst().orElse(null);
        assertNotNull(alipay);
        assertEquals("支付宝", alipay.getTypeName());
        assertEquals(40.0, alipay.getPercentage()); // 4000/10000 = 40%
    }

    @Test
    @DisplayName("获取支付类型统计 - 无数据返回空列表")
    void getPaymentTypeStatistics_shouldReturnEmptyList_whenNoData() {
        // Given
        when(orderRepository.countByPaymentType(any(), any(), any()))
                .thenReturn(new ArrayList<>());

        // When
        List<PaymentTypeStatisticsDTO> result = statisticsService.getPaymentTypeStatistics(
                1001L, LocalDateTime.now().minusDays(7), LocalDateTime.now());

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("获取支付类型统计 - 未知类型显示原始名称")
    void getPaymentTypeStatistics_shouldShowOriginalName_forUnknownType() {
        // Given
        List<Object[]> mockResults = new ArrayList<>();
        mockResults.add(new Object[]{"unknown_pay", 10L, 500.0});

        when(orderRepository.countByPaymentType(any(), any(), any()))
                .thenReturn(mockResults);

        // When
        List<PaymentTypeStatisticsDTO> result = statisticsService.getPaymentTypeStatistics(
                1001L, LocalDateTime.now().minusDays(7), LocalDateTime.now());

        // Then
        assertEquals(1, result.size());
        assertEquals("unknown_pay", result.get(0).getTypeName()); // 未知类型显示原始名称
    }

    // ==================== 订单趋势测试 ====================

    @Test
    @DisplayName("获取订单趋势 - 7天数据")
    void getOrderTrend_shouldReturnSevenDaysData() {
        // Given
        Long pid = 1001L;
        int days = 7;

        List<Object[]> mockResults = new ArrayList<>();
        LocalDate today = LocalDate.now();
        mockResults.add(new Object[]{today, 10L, 8L, 800.0});
        mockResults.add(new Object[]{today.minusDays(1), 12L, 10L, 1000.0});

        when(orderRepository.getOrderTrendByDate(eq(pid), any()))
                .thenReturn(mockResults);

        // When
        List<OrderTrendDTO> result = statisticsService.getOrderTrend(pid, days);

        // Then
        assertEquals(7, result.size()); // 应该有7天的数据

        // 检查今天的数据
        OrderTrendDTO todayTrend = result.stream()
                .filter(t -> t.getDate().equals(today))
                .findFirst().orElse(null);
        assertNotNull(todayTrend);
        assertEquals(10L, todayTrend.getOrderCount());
        assertEquals(8L, todayTrend.getSuccessCount());
        assertEquals(800.0, todayTrend.getTotalAmount());
    }

    @Test
    @DisplayName("获取订单趋势 - 无数据日期填充0")
    void getOrderTrend_shouldFillZeros_forMissingDates() {
        // Given
        when(orderRepository.getOrderTrendByDate(any(), any()))
                .thenReturn(new ArrayList<>()); // 无数据

        // When
        List<OrderTrendDTO> result = statisticsService.getOrderTrend(1001L, 7);

        // Then
        assertEquals(7, result.size());

        // 所有日期应该填充0
        for (OrderTrendDTO trend : result) {
            assertEquals(0L, trend.getOrderCount());
            assertEquals(0L, trend.getSuccessCount());
            assertEquals(0.0, trend.getTotalAmount());
        }
    }

    @Test
    @DisplayName("获取订单趋势 - 日期按升序排列")
    void getOrderTrend_shouldReturnDatesInAscendingOrder() {
        // Given
        when(orderRepository.getOrderTrendByDate(any(), any()))
                .thenReturn(new ArrayList<>());

        // When
        List<OrderTrendDTO> result = statisticsService.getOrderTrend(1001L, 7);

        // Then
        for (int i = 1; i < result.size(); i++) {
            assertTrue(result.get(i).getDate().isAfter(result.get(i - 1).getDate()));
        }
    }

    @Test
    @DisplayName("获取订单趋势 - 自定义天数")
    void getOrderTrend_shouldSupportCustomDays() {
        // Given
        when(orderRepository.getOrderTrendByDate(any(), any()))
                .thenReturn(new ArrayList<>());

        // When
        List<OrderTrendDTO> result30 = statisticsService.getOrderTrend(1001L, 30);
        List<OrderTrendDTO> result14 = statisticsService.getOrderTrend(1001L, 14);

        // Then
        assertEquals(30, result30.size());
        assertEquals(14, result14.size());
    }
}
