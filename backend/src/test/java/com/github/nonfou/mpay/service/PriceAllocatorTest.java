package com.github.nonfou.mpay.service;

import com.github.nonfou.mpay.entity.OrderEntity;
import com.github.nonfou.mpay.repository.OrderRepository;
import com.github.nonfou.mpay.service.impl.IncrementalPriceAllocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PriceAllocator 单元测试
 * 测试金额碰撞检测算法
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PriceAllocator 金额分配器测试")
class PriceAllocatorTest {

    @Mock
    private OrderRepository orderRepository;

    private IncrementalPriceAllocator priceAllocator;

    @BeforeEach
    void setUp() {
        priceAllocator = new IncrementalPriceAllocator(orderRepository);
    }

    @Test
    @DisplayName("无冲突时应返回原金额")
    void allocate_shouldReturnOriginalPrice_whenNoConflict() {
        // Given
        BigDecimal targetPrice = new BigDecimal("100.00");
        when(orderRepository.findByAidAndCidAndTypeAndState(anyLong(), anyLong(), anyString(), eq(0)))
                .thenReturn(new ArrayList<>());

        // When
        BigDecimal result = priceAllocator.allocate(targetPrice, 1L, 1L, "wxpay");

        // Then
        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    @DisplayName("有冲突时应返回递增后的金额")
    void allocate_shouldReturnIncrementedPrice_whenConflictExists() {
        // Given
        BigDecimal targetPrice = new BigDecimal("100.00");
        List<OrderEntity> existingOrders = new ArrayList<>();

        OrderEntity order1 = new OrderEntity();
        order1.setReallyPrice(new BigDecimal("100.00"));
        existingOrders.add(order1);

        when(orderRepository.findByAidAndCidAndTypeAndState(anyLong(), anyLong(), anyString(), eq(0)))
                .thenReturn(existingOrders);

        // When
        BigDecimal result = priceAllocator.allocate(targetPrice, 1L, 1L, "wxpay");

        // Then
        assertEquals(new BigDecimal("100.01"), result);
    }

    @Test
    @DisplayName("多个冲突时应跳过所有已存在金额")
    void allocate_shouldSkipAllExistingPrices_whenMultipleConflicts() {
        // Given
        BigDecimal targetPrice = new BigDecimal("100.00");
        List<OrderEntity> existingOrders = new ArrayList<>();

        OrderEntity order1 = new OrderEntity();
        order1.setReallyPrice(new BigDecimal("100.00"));
        existingOrders.add(order1);

        OrderEntity order2 = new OrderEntity();
        order2.setReallyPrice(new BigDecimal("100.01"));
        existingOrders.add(order2);

        OrderEntity order3 = new OrderEntity();
        order3.setReallyPrice(new BigDecimal("100.02"));
        existingOrders.add(order3);

        when(orderRepository.findByAidAndCidAndTypeAndState(anyLong(), anyLong(), anyString(), eq(0)))
                .thenReturn(existingOrders);

        // When
        BigDecimal result = priceAllocator.allocate(targetPrice, 1L, 1L, "wxpay");

        // Then
        assertEquals(new BigDecimal("100.03"), result);
    }

    @Test
    @DisplayName("小数精度应保持两位")
    void allocate_shouldMaintainTwoDecimalPlaces() {
        // Given
        BigDecimal targetPrice = new BigDecimal("99.999");
        when(orderRepository.findByAidAndCidAndTypeAndState(anyLong(), anyLong(), anyString(), eq(0)))
                .thenReturn(new ArrayList<>());

        // When
        BigDecimal result = priceAllocator.allocate(targetPrice, 1L, 1L, "alipay");

        // Then
        assertEquals(2, result.scale());
        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    @DisplayName("不同账号/通道间的金额不应冲突")
    void allocate_shouldNotConflict_betweenDifferentAccounts() {
        // Given
        BigDecimal targetPrice = new BigDecimal("100.00");

        // 账号1有100.00的订单
        List<OrderEntity> account1Orders = new ArrayList<>();
        OrderEntity order1 = new OrderEntity();
        order1.setReallyPrice(new BigDecimal("100.00"));
        account1Orders.add(order1);

        when(orderRepository.findByAidAndCidAndTypeAndState(eq(1L), anyLong(), anyString(), eq(0)))
                .thenReturn(account1Orders);
        when(orderRepository.findByAidAndCidAndTypeAndState(eq(2L), anyLong(), anyString(), eq(0)))
                .thenReturn(new ArrayList<>());

        // When
        BigDecimal result1 = priceAllocator.allocate(targetPrice, 1L, 1L, "wxpay");
        BigDecimal result2 = priceAllocator.allocate(targetPrice, 2L, 1L, "wxpay");

        // Then
        assertEquals(new BigDecimal("100.01"), result1); // 账号1有冲突，递增
        assertEquals(new BigDecimal("100.00"), result2); // 账号2无冲突，原价
    }
}
