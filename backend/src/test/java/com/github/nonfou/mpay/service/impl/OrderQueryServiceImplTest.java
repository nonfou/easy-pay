package com.github.nonfou.mpay.service.impl;

import com.github.nonfou.mpay.dto.order.OrderSummaryDTO;
import com.github.nonfou.mpay.entity.OrderEntity;
import com.github.nonfou.mpay.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * OrderQueryService 单元测试
 * 测试订单查询服务
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderQueryService 订单查询服务测试")
class OrderQueryServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    private OrderQueryServiceImpl orderQueryService;

    private static final Long TEST_PID = 1001L;

    @BeforeEach
    void setUp() {
        orderQueryService = new OrderQueryServiceImpl(orderRepository);
    }

    @Nested
    @DisplayName("查询活跃订单测试")
    class FindActiveOrdersTests {

        @Test
        @DisplayName("查询活跃订单成功 - 有结果")
        void findActiveOrders_shouldReturnOrders_whenOrdersExist() {
            // Given
            OrderEntity order = createOrder(1L, "H001", 0);
            when(orderRepository.findActiveOrders(eq(TEST_PID), any(LocalDateTime.class)))
                    .thenReturn(List.of(order));

            // When
            List<OrderSummaryDTO> result = orderQueryService.findActiveOrders(TEST_PID, null);

            // Then
            assertEquals(1, result.size());
            assertEquals("H001", result.get(0).getOrderId());
            assertEquals(0, result.get(0).getState());
            assertEquals("待支付", result.get(0).getStateName());
        }

        @Test
        @DisplayName("查询活跃订单 - 空结果")
        void findActiveOrders_shouldReturnEmpty_whenNoOrders() {
            // Given
            when(orderRepository.findActiveOrders(eq(TEST_PID), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            List<OrderSummaryDTO> result = orderQueryService.findActiveOrders(TEST_PID, null);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("查询活跃订单 - 使用自定义过期时间")
        void findActiveOrders_shouldUseCustomExpireMinutes() {
            // Given
            Integer customExpireMinutes = 10;
            when(orderRepository.findActiveOrders(eq(TEST_PID), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            orderQueryService.findActiveOrders(TEST_PID, customExpireMinutes);

            // Then
            verify(orderRepository).findActiveOrders(eq(TEST_PID), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("查询活跃订单 - 使用默认过期时间（3分钟）")
        void findActiveOrders_shouldUseDefaultExpireMinutes_whenNull() {
            // Given
            when(orderRepository.findActiveOrders(eq(TEST_PID), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            orderQueryService.findActiveOrders(TEST_PID, null);

            // Then
            verify(orderRepository).findActiveOrders(eq(TEST_PID), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("查询活跃订单 - PID 为 null 时查询所有")
        void findActiveOrders_shouldQueryAll_whenPidIsNull() {
            // Given
            when(orderRepository.findActiveOrders(eq(null), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            List<OrderSummaryDTO> result = orderQueryService.findActiveOrders(null, null);

            // Then
            assertTrue(result.isEmpty());
            verify(orderRepository).findActiveOrders(eq(null), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("查询成功订单测试")
    class FindSuccessOrdersTests {

        @Test
        @DisplayName("查询成功订单 - 有结果")
        void findSuccessOrders_shouldReturnOrders_whenOrdersExist() {
            // Given
            OrderEntity order = createOrder(1L, "H001", 1);
            order.setPayTime(LocalDateTime.now().minusHours(1));
            LocalDateTime startTime = LocalDateTime.now().minusDays(1);
            LocalDateTime endTime = LocalDateTime.now();

            when(orderRepository.findSuccessOrders(TEST_PID, startTime, endTime))
                    .thenReturn(List.of(order));

            // When
            List<OrderSummaryDTO> result = orderQueryService.findSuccessOrders(TEST_PID, startTime, endTime);

            // Then
            assertEquals(1, result.size());
            assertEquals("H001", result.get(0).getOrderId());
            assertEquals(1, result.get(0).getState());
            assertEquals("已支付", result.get(0).getStateName());
        }

        @Test
        @DisplayName("查询成功订单 - 空结果")
        void findSuccessOrders_shouldReturnEmpty_whenNoOrders() {
            // Given
            LocalDateTime startTime = LocalDateTime.now().minusDays(1);
            LocalDateTime endTime = LocalDateTime.now();

            when(orderRepository.findSuccessOrders(TEST_PID, startTime, endTime))
                    .thenReturn(Collections.emptyList());

            // When
            List<OrderSummaryDTO> result = orderQueryService.findSuccessOrders(TEST_PID, startTime, endTime);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("查询成功订单 - 多个订单")
        void findSuccessOrders_shouldReturnMultipleOrders() {
            // Given
            OrderEntity order1 = createOrder(1L, "H001", 1);
            OrderEntity order2 = createOrder(2L, "H002", 1);
            LocalDateTime startTime = LocalDateTime.now().minusDays(1);
            LocalDateTime endTime = LocalDateTime.now();

            when(orderRepository.findSuccessOrders(TEST_PID, startTime, endTime))
                    .thenReturn(List.of(order1, order2));

            // When
            List<OrderSummaryDTO> result = orderQueryService.findSuccessOrders(TEST_PID, startTime, endTime);

            // Then
            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("查询超时订单测试")
    class FindExpiredOrdersTests {

        @Test
        @DisplayName("查询超时订单 - 有结果")
        void findExpiredOrders_shouldReturnOrders_whenOrdersExist() {
            // Given
            OrderEntity order = createOrder(1L, "H001", 0);
            order.setCreateTime(LocalDateTime.now().minusMinutes(10)); // 超过默认3分钟

            when(orderRepository.findExpiredOrders(eq(TEST_PID), any(LocalDateTime.class)))
                    .thenReturn(List.of(order));

            // When
            List<OrderSummaryDTO> result = orderQueryService.findExpiredOrders(TEST_PID, null);

            // Then
            assertEquals(1, result.size());
            assertEquals("H001", result.get(0).getOrderId());
        }

        @Test
        @DisplayName("查询超时订单 - 空结果")
        void findExpiredOrders_shouldReturnEmpty_whenNoOrders() {
            // Given
            when(orderRepository.findExpiredOrders(eq(TEST_PID), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            List<OrderSummaryDTO> result = orderQueryService.findExpiredOrders(TEST_PID, null);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("查询超时订单 - 使用自定义过期时间")
        void findExpiredOrders_shouldUseCustomExpireMinutes() {
            // Given
            Integer customExpireMinutes = 5;
            when(orderRepository.findExpiredOrders(eq(TEST_PID), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            orderQueryService.findExpiredOrders(TEST_PID, customExpireMinutes);

            // Then
            verify(orderRepository).findExpiredOrders(eq(TEST_PID), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("DTO 转换测试")
    class DTOConversionTests {

        @Test
        @DisplayName("转换 DTO - 所有字段正确映射")
        void toSummaryDTO_shouldMapAllFields() {
            // Given
            OrderEntity order = new OrderEntity();
            order.setId(1L);
            order.setOrderId("H202411250001");
            order.setOutTradeNo("OUT123");
            order.setPid(TEST_PID);
            order.setType("wxpay");
            order.setName("测试商品");
            order.setMoney(new BigDecimal("100.00"));
            order.setReallyPrice(new BigDecimal("100.01"));
            order.setState(1);
            order.setCreateTime(LocalDateTime.now());
            order.setPayTime(LocalDateTime.now());

            when(orderRepository.findSuccessOrders(eq(TEST_PID), any(), any()))
                    .thenReturn(List.of(order));

            // When
            List<OrderSummaryDTO> result = orderQueryService.findSuccessOrders(
                    TEST_PID, LocalDateTime.now().minusDays(1), LocalDateTime.now());

            // Then
            OrderSummaryDTO dto = result.get(0);
            assertEquals(1L, dto.getId());
            assertEquals("H202411250001", dto.getOrderId());
            assertEquals("OUT123", dto.getOutTradeNo());
            assertEquals(TEST_PID, dto.getPid());
            assertEquals("wxpay", dto.getType());
            assertEquals("测试商品", dto.getName());
            assertEquals(new BigDecimal("100.00"), dto.getMoney());
            assertEquals(new BigDecimal("100.01"), dto.getReallyPrice());
            assertEquals(1, dto.getState());
            assertEquals("已支付", dto.getStateName());
        }

        @Test
        @DisplayName("转换 DTO - 状态名称映射正确")
        void toSummaryDTO_shouldMapStateNames() {
            // Given
            OrderEntity order0 = createOrder(1L, "H001", 0);
            OrderEntity order1 = createOrder(2L, "H002", 1);
            OrderEntity order2 = createOrder(3L, "H003", 2);

            when(orderRepository.findActiveOrders(any(), any())).thenReturn(List.of(order0));
            when(orderRepository.findSuccessOrders(any(), any(), any())).thenReturn(List.of(order1));
            when(orderRepository.findExpiredOrders(any(), any())).thenReturn(List.of(order2));

            // When
            List<OrderSummaryDTO> activeOrders = orderQueryService.findActiveOrders(null, null);
            List<OrderSummaryDTO> successOrders = orderQueryService.findSuccessOrders(null, LocalDateTime.now().minusDays(1), LocalDateTime.now());
            List<OrderSummaryDTO> expiredOrders = orderQueryService.findExpiredOrders(null, null);

            // Then
            assertEquals("待支付", activeOrders.get(0).getStateName());
            assertEquals("已支付", successOrders.get(0).getStateName());
            assertEquals("已关闭", expiredOrders.get(0).getStateName());
        }

        @Test
        @DisplayName("转换 DTO - 未知状态返回'未知'")
        void toSummaryDTO_shouldReturnUnknown_forUnknownState() {
            // Given
            OrderEntity order = createOrder(1L, "H001", 99); // 未知状态

            when(orderRepository.findActiveOrders(any(), any())).thenReturn(List.of(order));

            // When
            List<OrderSummaryDTO> result = orderQueryService.findActiveOrders(null, null);

            // Then
            assertEquals("未知", result.get(0).getStateName());
        }

        @Test
        @DisplayName("转换 DTO - 超时标记正确")
        void toSummaryDTO_shouldMarkExpiredCorrectly() {
            // Given - 创建一个超过3分钟的待支付订单
            OrderEntity expiredOrder = createOrder(1L, "H001", 0);
            expiredOrder.setCreateTime(LocalDateTime.now().minusMinutes(10)); // 超过默认3分钟

            when(orderRepository.findExpiredOrders(any(), any())).thenReturn(List.of(expiredOrder));

            // When
            List<OrderSummaryDTO> result = orderQueryService.findExpiredOrders(null, null);

            // Then
            assertTrue(result.get(0).getExpired());
        }

        @Test
        @DisplayName("转换 DTO - 非超时订单标记为false")
        void toSummaryDTO_shouldNotMarkAsExpired_whenNotExpired() {
            // Given - 创建一个新的待支付订单
            OrderEntity newOrder = createOrder(1L, "H001", 0);
            newOrder.setCreateTime(LocalDateTime.now()); // 刚创建

            when(orderRepository.findActiveOrders(any(), any())).thenReturn(List.of(newOrder));

            // When
            List<OrderSummaryDTO> result = orderQueryService.findActiveOrders(null, null);

            // Then
            assertFalse(result.get(0).getExpired());
        }
    }

    // ==================== 辅助方法 ====================

    private OrderEntity createOrder(Long id, String orderId, Integer state) {
        OrderEntity order = new OrderEntity();
        order.setId(id);
        order.setOrderId(orderId);
        order.setOutTradeNo("OUT" + id);
        order.setPid(TEST_PID);
        order.setType("wxpay");
        order.setName("测试商品");
        order.setMoney(new BigDecimal("100.00"));
        order.setReallyPrice(new BigDecimal("100.01"));
        order.setState(state);
        order.setCreateTime(LocalDateTime.now());
        order.setNotifyUrl("http://example.com/notify");
        return order;
    }
}
