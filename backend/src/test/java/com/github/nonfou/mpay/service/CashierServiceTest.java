package com.github.nonfou.mpay.service;

import com.github.nonfou.mpay.dto.cashier.CashierOrderDTO;
import com.github.nonfou.mpay.dto.cashier.CashierOrderStateDTO;
import com.github.nonfou.mpay.entity.OrderEntity;
import com.github.nonfou.mpay.entity.PayChannelEntity;
import com.github.nonfou.mpay.repository.OrderRepository;
import com.github.nonfou.mpay.repository.PayChannelRepository;
import com.github.nonfou.mpay.service.impl.CashierServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CashierService 单元测试
 * 测试收银台服务功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CashierService 收银台服务测试")
class CashierServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PayChannelRepository payChannelRepository;

    private CashierServiceImpl cashierService;

    @BeforeEach
    void setUp() {
        cashierService = new CashierServiceImpl(orderRepository, payChannelRepository);
    }

    // ==================== 获取订单详情测试 ====================

    @Test
    @DisplayName("获取订单详情成功")
    void getOrderDetail_shouldReturnOrder_whenOrderExists() {
        // Given
        String orderId = "H202411250001";
        OrderEntity order = createOrder(orderId, 0);
        PayChannelEntity channel = createChannel();

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(order));
        when(payChannelRepository.findById(order.getCid())).thenReturn(Optional.of(channel));

        // When
        Optional<CashierOrderDTO> result = cashierService.getOrderDetail(orderId);

        // Then
        assertTrue(result.isPresent());
        CashierOrderDTO dto = result.get();
        assertEquals(orderId, dto.getOrderId());
        assertEquals("wxpay", dto.getType());
        assertEquals("测试商品", dto.getName());
        assertEquals(new BigDecimal("100.00"), dto.getMoney());
        assertEquals(new BigDecimal("100.01"), dto.getReallyPrice());
        assertEquals("http://example.com/qr.png", dto.getQrcodeUrl());
        assertEquals(0, dto.getState());
    }

    @Test
    @DisplayName("获取订单详情 - 订单不存在")
    void getOrderDetail_shouldReturnEmpty_whenOrderNotFound() {
        // Given
        String orderId = "NOT_EXIST";
        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        // When
        Optional<CashierOrderDTO> result = cashierService.getOrderDetail(orderId);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("获取订单详情 - 通道不存在时二维码为空")
    void getOrderDetail_shouldHaveNullQrcode_whenChannelNotFound() {
        // Given
        String orderId = "H202411250001";
        OrderEntity order = createOrder(orderId, 0);

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(order));
        when(payChannelRepository.findById(order.getCid())).thenReturn(Optional.empty());

        // When
        Optional<CashierOrderDTO> result = cashierService.getOrderDetail(orderId);

        // Then
        assertTrue(result.isPresent());
        assertNull(result.get().getQrcodeUrl());
    }

    @Test
    @DisplayName("获取订单详情 - 订单无通道ID时二维码为空")
    void getOrderDetail_shouldHaveNullQrcode_whenNoCid() {
        // Given
        String orderId = "H202411250001";
        OrderEntity order = createOrder(orderId, 0);
        order.setCid(null);

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(order));

        // When
        Optional<CashierOrderDTO> result = cashierService.getOrderDetail(orderId);

        // Then
        assertTrue(result.isPresent());
        assertNull(result.get().getQrcodeUrl());
        verify(payChannelRepository, never()).findById(any());
    }

    // ==================== 获取订单状态测试 ====================

    @Test
    @DisplayName("获取订单状态 - 待支付状态有倒计时")
    void getOrderState_shouldHaveExpireIn_whenPending() {
        // Given
        String orderId = "H202411250001";
        OrderEntity order = createOrder(orderId, 0);
        order.setCloseTime(LocalDateTime.now().plusMinutes(2)); // 2分钟后关闭

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(order));

        // When
        Optional<CashierOrderStateDTO> result = cashierService.getOrderState(orderId);

        // Then
        assertTrue(result.isPresent());
        CashierOrderStateDTO dto = result.get();
        assertEquals(orderId, dto.getOrderId());
        assertEquals(0, dto.getState());
        assertTrue(dto.getExpireIn() > 0 && dto.getExpireIn() <= 120);
    }

    @Test
    @DisplayName("获取订单状态 - 已支付状态倒计时为0")
    void getOrderState_shouldHaveZeroExpireIn_whenPaid() {
        // Given
        String orderId = "H202411250001";
        OrderEntity order = createOrder(orderId, 1);

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(order));

        // When
        Optional<CashierOrderStateDTO> result = cashierService.getOrderState(orderId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(1, result.get().getState());
        assertEquals(0, result.get().getExpireIn());
    }

    @Test
    @DisplayName("获取订单状态 - 已过期订单倒计时为0")
    void getOrderState_shouldHaveZeroExpireIn_whenExpired() {
        // Given
        String orderId = "H202411250001";
        OrderEntity order = createOrder(orderId, 0);
        order.setCloseTime(LocalDateTime.now().minusMinutes(1)); // 已过期

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(order));

        // When
        Optional<CashierOrderStateDTO> result = cashierService.getOrderState(orderId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(0, result.get().getExpireIn());
    }

    @Test
    @DisplayName("获取订单状态 - 订单不存在")
    void getOrderState_shouldReturnEmpty_whenOrderNotFound() {
        // Given
        String orderId = "NOT_EXIST";
        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        // When
        Optional<CashierOrderStateDTO> result = cashierService.getOrderState(orderId);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("获取订单状态 - 包含返回URL")
    void getOrderState_shouldIncludeReturnUrl() {
        // Given
        String orderId = "H202411250001";
        String returnUrl = "http://merchant.com/return";
        OrderEntity order = createOrder(orderId, 1);
        order.setReturnUrl(returnUrl);

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(order));

        // When
        Optional<CashierOrderStateDTO> result = cashierService.getOrderState(orderId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(returnUrl, result.get().getReturnUrl());
    }

    // ==================== 辅助方法 ====================

    private OrderEntity createOrder(String orderId, int state) {
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
        order.setState(state);
        order.setCreateTime(LocalDateTime.now());
        order.setCloseTime(LocalDateTime.now().plusMinutes(3));
        order.setCid(1L);
        order.setAid(1L);
        return order;
    }

    private PayChannelEntity createChannel() {
        PayChannelEntity channel = new PayChannelEntity();
        channel.setId(1L);
        channel.setChannel("wxpay_qr");
        channel.setQrcode("http://example.com/qr.png");
        channel.setState(1);
        return channel;
    }
}
