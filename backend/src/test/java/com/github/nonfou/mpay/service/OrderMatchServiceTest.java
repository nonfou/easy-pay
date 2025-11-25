package com.github.nonfou.mpay.service;

import com.github.nonfou.mpay.common.error.BusinessException;
import com.github.nonfou.mpay.dto.MatchRequest;
import com.github.nonfou.mpay.entity.OrderEntity;
import com.github.nonfou.mpay.notify.NotifyClient;
import com.github.nonfou.mpay.repository.OrderRepository;
import com.github.nonfou.mpay.service.impl.OrderMatchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderMatchService 单元测试
 * 测试订单匹配功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderMatchService 订单匹配服务测试")
class OrderMatchServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private NotifyClient notifyClient;

    private OrderMatchServiceImpl orderMatchService;

    @BeforeEach
    void setUp() {
        orderMatchService = new OrderMatchServiceImpl(orderRepository, notifyClient);
    }

    @Test
    @DisplayName("匹配成功 - 金额完全匹配")
    void matchPayment_shouldSucceed_whenPriceMatches() {
        // Given
        MatchRequest request = createMatchRequest(100.01);
        List<OrderEntity> candidates = List.of(createPendingOrder("H001", 100.01));

        when(orderRepository.findByPidAndAidAndTypeAndState(anyLong(), anyLong(), anyString(), eq(0)))
                .thenReturn(candidates);
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(i -> i.getArgument(0));

        // When
        orderMatchService.matchPayment(request);

        // Then
        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(captor.capture());

        OrderEntity savedOrder = captor.getValue();
        assertEquals(1, savedOrder.getState());
        assertNotNull(savedOrder.getPayTime());
        assertEquals("PLT123456", savedOrder.getPlatformOrder());

        verify(notifyClient).notifyMerchant(any(OrderEntity.class));
    }

    @Test
    @DisplayName("匹配失败 - 无匹配金额的订单")
    void matchPayment_shouldThrowException_whenNoMatchingOrder() {
        // Given
        MatchRequest request = createMatchRequest(100.01);
        List<OrderEntity> candidates = List.of(createPendingOrder("H001", 100.02)); // 金额不匹配

        when(orderRepository.findByPidAndAidAndTypeAndState(anyLong(), anyLong(), anyString(), eq(0)))
                .thenReturn(candidates);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderMatchService.matchPayment(request));

        assertTrue(exception.getMessage().contains("no matching order"));
        verify(orderRepository, never()).save(any());
        verify(notifyClient, never()).notifyMerchant(any());
    }

    @Test
    @DisplayName("匹配失败 - 无待支付订单")
    void matchPayment_shouldThrowException_whenNoPendingOrders() {
        // Given
        MatchRequest request = createMatchRequest(100.01);

        when(orderRepository.findByPidAndAidAndTypeAndState(anyLong(), anyLong(), anyString(), eq(0)))
                .thenReturn(new ArrayList<>());

        // When & Then
        assertThrows(BusinessException.class, () -> orderMatchService.matchPayment(request));
    }

    @Test
    @DisplayName("多个相同金额订单时应匹配最早创建的订单")
    void matchPayment_shouldMatchOldestOrder_whenMultipleSamePriceOrders() {
        // Given
        MatchRequest request = createMatchRequest(100.01);

        OrderEntity order1 = createPendingOrder("H001", 100.01);
        order1.setCreateTime(LocalDateTime.now().minusMinutes(10)); // 更早的订单

        OrderEntity order2 = createPendingOrder("H002", 100.01);
        order2.setCreateTime(LocalDateTime.now().minusMinutes(5));

        OrderEntity order3 = createPendingOrder("H003", 100.01);
        order3.setCreateTime(LocalDateTime.now());

        List<OrderEntity> candidates = List.of(order2, order1, order3); // 故意打乱顺序

        when(orderRepository.findByPidAndAidAndTypeAndState(anyLong(), anyLong(), anyString(), eq(0)))
                .thenReturn(candidates);
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(i -> i.getArgument(0));

        // When
        orderMatchService.matchPayment(request);

        // Then
        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(captor.capture());

        assertEquals("H001", captor.getValue().getOrderId()); // 应匹配最早的订单
    }

    @Test
    @DisplayName("匹配成功后应设置平台订单号")
    void matchPayment_shouldSetPlatformOrder_whenMatched() {
        // Given
        String platformOrder = "WX202411250001";
        MatchRequest request = new MatchRequest();
        request.setPid(1001L);
        request.setAid(1L);
        request.setPayway("wxpay");
        request.setPrice(new BigDecimal("100.01"));
        request.setPlatformOrder(platformOrder);

        List<OrderEntity> candidates = List.of(createPendingOrder("H001", 100.01));

        when(orderRepository.findByPidAndAidAndTypeAndState(anyLong(), anyLong(), anyString(), eq(0)))
                .thenReturn(candidates);
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(i -> i.getArgument(0));

        // When
        orderMatchService.matchPayment(request);

        // Then
        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(captor.capture());

        assertEquals(platformOrder, captor.getValue().getPlatformOrder());
    }

    @Test
    @DisplayName("金额精度比较 - 浮点数精确匹配")
    void matchPayment_shouldMatchExactPrice_withDoublePrecision() {
        // Given
        MatchRequest request = createMatchRequest(99.99);
        List<OrderEntity> candidates = List.of(
                createPendingOrder("H001", 100.00),
                createPendingOrder("H002", 99.99),
                createPendingOrder("H003", 99.98)
        );

        when(orderRepository.findByPidAndAidAndTypeAndState(anyLong(), anyLong(), anyString(), eq(0)))
                .thenReturn(candidates);
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(i -> i.getArgument(0));

        // When
        orderMatchService.matchPayment(request);

        // Then
        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(captor.capture());

        assertEquals("H002", captor.getValue().getOrderId());
    }

    // ==================== 辅助方法 ====================

    private MatchRequest createMatchRequest(double price) {
        MatchRequest request = new MatchRequest();
        request.setPid(1001L);
        request.setAid(1L);
        request.setPayway("wxpay");
        request.setPrice(new BigDecimal(String.valueOf(price)));
        request.setPlatformOrder("PLT123456");
        return request;
    }

    private OrderEntity createPendingOrder(String orderId, double reallyPrice) {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setOrderId(orderId);
        order.setPid(1001L);
        order.setAid(1L);
        order.setType("wxpay");
        order.setOutTradeNo("OUT" + orderId);
        order.setNotifyUrl("http://merchant.com/notify");
        order.setName("测试商品");
        order.setMoney(new BigDecimal("100.00"));
        order.setReallyPrice(new BigDecimal(String.valueOf(reallyPrice)));
        order.setState(0);
        order.setCreateTime(LocalDateTime.now());
        return order;
    }
}
