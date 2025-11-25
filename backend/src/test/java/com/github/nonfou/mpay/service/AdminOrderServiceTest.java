package com.github.nonfou.mpay.service;

import com.github.nonfou.mpay.common.error.BusinessException;
import com.github.nonfou.mpay.entity.OrderEntity;
import com.github.nonfou.mpay.notify.NotifyClient;
import com.github.nonfou.mpay.repository.OrderRepository;
import com.github.nonfou.mpay.service.impl.AdminOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AdminOrderService 单元测试
 * 测试手动补单、重新通知、清理超时订单功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminOrderService 管理员订单��务测试")
class AdminOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private NotifyClient notifyClient;

    private AdminOrderServiceImpl adminOrderService;

    @BeforeEach
    void setUp() {
        adminOrderService = new AdminOrderServiceImpl(orderRepository, notifyClient);
    }

    // ==================== 手动补单测试 ====================

    @Test
    @DisplayName("手动补单成功 - 待支付订单")
    void manualSettle_shouldSucceed_whenOrderIsPending() {
        // Given
        String orderId = "H202411250001";
        OrderEntity order = createPendingOrder(orderId);

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(i -> i.getArgument(0));

        // When
        OrderEntity result = adminOrderService.manualSettle(orderId, "测试补单");

        // Then
        assertEquals(1, result.getState());
        assertNotNull(result.getPayTime());

        verify(orderRepository).save(order);
        verify(notifyClient).notifyMerchant(order);
    }

    @Test
    @DisplayName("手动补单失败 - 订单不存在")
    void manualSettle_shouldThrowException_whenOrderNotFound() {
        // Given
        String orderId = "NOT_EXIST";
        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> adminOrderService.manualSettle(orderId, "测试"));

        assertTrue(exception.getMessage().contains("订单不存在"));
        verify(orderRepository, never()).save(any());
        verify(notifyClient, never()).notifyMerchant(any());
    }

    @Test
    @DisplayName("手动补单失败 - 订单已支付")
    void manualSettle_shouldThrowException_whenOrderAlreadyPaid() {
        // Given
        String orderId = "H202411250001";
        OrderEntity order = createPaidOrder(orderId);

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(order));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> adminOrderService.manualSettle(orderId, "测试"));

        assertTrue(exception.getMessage().contains("已支付"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("手动补单 - 通知失败不影响补单结果")
    void manualSettle_shouldSucceed_evenIfNotifyFails() {
        // Given
        String orderId = "H202411250001";
        OrderEntity order = createPendingOrder(orderId);

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("通知失败")).when(notifyClient).notifyMerchant(any());

        // When
        OrderEntity result = adminOrderService.manualSettle(orderId, "测试补单");

        // Then
        assertEquals(1, result.getState());
        verify(orderRepository).save(order);
    }

    // ==================== 重新通知测试 ====================

    @Test
    @DisplayName("重新通知成功 - 已支付订单")
    void renotify_shouldSucceed_whenOrderIsPaid() {
        // Given
        String orderId = "H202411250001";
        OrderEntity order = createPaidOrder(orderId);

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(order));

        // When
        boolean result = adminOrderService.renotify(orderId);

        // Then
        assertTrue(result);
        verify(notifyClient).notifyMerchant(order);
    }

    @Test
    @DisplayName("重新通知失败 - 订单不存在")
    void renotify_shouldThrowException_whenOrderNotFound() {
        // Given
        String orderId = "NOT_EXIST";
        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(BusinessException.class, () -> adminOrderService.renotify(orderId));
    }

    @Test
    @DisplayName("重新通知失败 - 订单未支付")
    void renotify_shouldThrowException_whenOrderNotPaid() {
        // Given
        String orderId = "H202411250001";
        OrderEntity order = createPendingOrder(orderId);

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(order));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> adminOrderService.renotify(orderId));

        assertTrue(exception.getMessage().contains("只有已支付订单"));
    }

    @Test
    @DisplayName("重新通知 - 通知失败返回false")
    void renotify_shouldReturnFalse_whenNotifyFails() {
        // Given
        String orderId = "H202411250001";
        OrderEntity order = createPaidOrder(orderId);

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(order));
        doThrow(new RuntimeException("通知失败")).when(notifyClient).notifyMerchant(any());

        // When
        boolean result = adminOrderService.renotify(orderId);

        // Then
        assertFalse(result);
    }

    // ==================== 清理超时订单测试 ====================

    @Test
    @DisplayName("清理超时订单 - 使用默认超时时间")
    void cleanExpiredOrders_shouldUseDefaultExpireTime_whenNotSpecified() {
        // Given
        when(orderRepository.deleteExpiredOrders(any(LocalDateTime.class))).thenReturn(5);

        // When
        int result = adminOrderService.cleanExpiredOrders(null);

        // Then
        assertEquals(5, result);

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orderRepository).deleteExpiredOrders(captor.capture());

        // 默认超时时间是3分钟前
        LocalDateTime capturedTime = captor.getValue();
        LocalDateTime expectedTime = LocalDateTime.now().minusMinutes(3);
        assertTrue(Math.abs(java.time.Duration.between(capturedTime, expectedTime).getSeconds()) < 2);
    }

    @Test
    @DisplayName("清理超时订单 - 使用自定义超时时间")
    void cleanExpiredOrders_shouldUseCustomExpireTime_whenSpecified() {
        // Given
        when(orderRepository.deleteExpiredOrders(any(LocalDateTime.class))).thenReturn(10);

        // When
        int result = adminOrderService.cleanExpiredOrders(10);

        // Then
        assertEquals(10, result);

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orderRepository).deleteExpiredOrders(captor.capture());

        // 自定义超时时间是10分钟前
        LocalDateTime capturedTime = captor.getValue();
        LocalDateTime expectedTime = LocalDateTime.now().minusMinutes(10);
        assertTrue(Math.abs(java.time.Duration.between(capturedTime, expectedTime).getSeconds()) < 2);
    }

    @Test
    @DisplayName("清理超时订单 - 无超时订单时返回0")
    void cleanExpiredOrders_shouldReturnZero_whenNoExpiredOrders() {
        // Given
        when(orderRepository.deleteExpiredOrders(any(LocalDateTime.class))).thenReturn(0);

        // When
        int result = adminOrderService.cleanExpiredOrders(5);

        // Then
        assertEquals(0, result);
    }

    // ==================== 辅助方法 ====================

    private OrderEntity createPendingOrder(String orderId) {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setOrderId(orderId);
        order.setPid(1001L);
        order.setType("wxpay");
        order.setOutTradeNo("OUT123");
        order.setNotifyUrl("http://merchant.com/notify");
        order.setName("测试商品");
        order.setMoney(new BigDecimal("100.00"));
        order.setReallyPrice(new BigDecimal("100.01"));
        order.setState(0); // 待支付
        order.setCreateTime(LocalDateTime.now());
        return order;
    }

    private OrderEntity createPaidOrder(String orderId) {
        OrderEntity order = createPendingOrder(orderId);
        order.setState(1); // 已支付
        order.setPayTime(LocalDateTime.now());
        return order;
    }
}
