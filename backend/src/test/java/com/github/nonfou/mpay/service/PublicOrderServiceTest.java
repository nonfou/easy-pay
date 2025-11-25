package com.github.nonfou.mpay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nonfou.mpay.common.error.BusinessException;
import com.github.nonfou.mpay.common.error.ErrorCode;
import com.github.nonfou.mpay.dto.PublicCreateOrderDTO;
import com.github.nonfou.mpay.dto.PublicCreateOrderResult;
import com.github.nonfou.mpay.entity.OrderEntity;
import com.github.nonfou.mpay.event.OrderEventPublisher;
import com.github.nonfou.mpay.repository.OrderRepository;
import com.github.nonfou.mpay.service.impl.PublicOrderServiceImpl;
import com.github.nonfou.mpay.signature.SignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PublicOrderService 单元测试
 * 测试订单创建流程
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PublicOrderService 公共订单服务测试")
class PublicOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @Mock
    private MerchantSecretService merchantSecretService;

    @Mock
    private ChannelSelector channelSelector;

    @Mock
    private PriceAllocator priceAllocator;

    @Mock
    private SignatureService signatureService;

    private ObjectMapper objectMapper;

    private PublicOrderServiceImpl publicOrderService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        publicOrderService = new PublicOrderServiceImpl(
                orderRepository,
                orderEventPublisher,
                merchantSecretService,
                channelSelector,
                priceAllocator,
                signatureService,
                objectMapper
        );
    }

    @Nested
    @DisplayName("创建订单成功场景")
    class CreateOrderSuccessTests {

        @Test
        @DisplayName("正常创建订单 - 所有参数有效")
        void createOrder_shouldSucceed_whenAllParamsValid() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            String merchantSecret = "test-secret-key";

            when(orderRepository.findByOutTradeNoAndPid(anyString(), anyLong()))
                    .thenReturn(Optional.empty());
            when(merchantSecretService.getSecret(anyLong()))
                    .thenReturn(Optional.of(merchantSecret));
            when(signatureService.verify(any(), eq(merchantSecret)))
                    .thenReturn(true);
            when(channelSelector.select(anyLong(), anyString()))
                    .thenReturn(Optional.of(new ChannelSelector.ChannelSelection(1L, 1L, 1)));
            when(priceAllocator.allocate(any(), anyLong(), anyLong(), anyString()))
                    .thenReturn(new BigDecimal("100.01"));
            when(orderRepository.save(any(OrderEntity.class)))
                    .thenAnswer(i -> i.getArgument(0));

            // When
            PublicCreateOrderResult result = publicOrderService.createOrder(request);

            // Then
            assertNotNull(result);
            assertNotNull(result.getOrderId());
            assertTrue(result.getOrderId().startsWith("H"));
            assertTrue(result.getCashierUrl().contains(result.getOrderId()));

            // 验证订单保存
            ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
            verify(orderRepository).save(orderCaptor.capture());

            OrderEntity savedOrder = orderCaptor.getValue();
            assertEquals(request.getPid(), savedOrder.getPid());
            assertEquals(request.getType(), savedOrder.getType());
            assertEquals(request.getOutTradeNo(), savedOrder.getOutTradeNo());
            assertEquals(request.getNotifyUrl(), savedOrder.getNotifyUrl());
            assertEquals(request.getName(), savedOrder.getName());
            assertEquals(request.getMoney(), savedOrder.getMoney());
            assertEquals(0, savedOrder.getState()); // 待支付状态

            // 验证事件发布
            verify(orderEventPublisher).publish(any(OrderEntity.class));
        }

        @Test
        @DisplayName("创建订单 - 金额分配后有调整")
        void createOrder_shouldApplyAllocatedPrice() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            BigDecimal originalMoney = new BigDecimal("100.00");
            BigDecimal allocatedPrice = new BigDecimal("100.01");
            request.setMoney(originalMoney);

            when(orderRepository.findByOutTradeNoAndPid(anyString(), anyLong()))
                    .thenReturn(Optional.empty());
            when(merchantSecretService.getSecret(anyLong()))
                    .thenReturn(Optional.of("secret"));
            when(signatureService.verify(any(), anyString()))
                    .thenReturn(true);
            when(channelSelector.select(anyLong(), anyString()))
                    .thenReturn(Optional.of(new ChannelSelector.ChannelSelection(1L, 2L, 1)));
            when(priceAllocator.allocate(eq(originalMoney), anyLong(), anyLong(), anyString()))
                    .thenReturn(allocatedPrice);
            when(orderRepository.save(any(OrderEntity.class)))
                    .thenAnswer(i -> i.getArgument(0));

            // When
            publicOrderService.createOrder(request);

            // Then
            ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
            verify(orderRepository).save(orderCaptor.capture());

            OrderEntity savedOrder = orderCaptor.getValue();
            assertEquals(originalMoney, savedOrder.getMoney());
            assertEquals(allocatedPrice, savedOrder.getReallyPrice());
        }

        @Test
        @DisplayName("创建订单 - 包含附加参数")
        void createOrder_shouldSerializeAttach() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            request.setAttach(Map.of("key1", "value1", "key2", 123));

            when(orderRepository.findByOutTradeNoAndPid(anyString(), anyLong()))
                    .thenReturn(Optional.empty());
            when(merchantSecretService.getSecret(anyLong()))
                    .thenReturn(Optional.of("secret"));
            when(signatureService.verify(any(), anyString()))
                    .thenReturn(true);
            when(channelSelector.select(anyLong(), anyString()))
                    .thenReturn(Optional.of(new ChannelSelector.ChannelSelection(1L, 1L, 1)));
            when(priceAllocator.allocate(any(), anyLong(), anyLong(), anyString()))
                    .thenReturn(new BigDecimal("100.00"));
            when(orderRepository.save(any(OrderEntity.class)))
                    .thenAnswer(i -> i.getArgument(0));

            // When
            publicOrderService.createOrder(request);

            // Then
            ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
            verify(orderRepository).save(orderCaptor.capture());

            OrderEntity savedOrder = orderCaptor.getValue();
            assertNotNull(savedOrder.getParam());
            assertTrue(savedOrder.getParam().contains("key1"));
            assertTrue(savedOrder.getParam().contains("value1"));
        }
    }

    @Nested
    @DisplayName("创建订单失败场景")
    class CreateOrderFailureTests {

        @Test
        @DisplayName("金额为null时抛出异常")
        void createOrder_shouldThrowException_whenMoneyIsNull() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            request.setMoney(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> publicOrderService.createOrder(request));

            assertEquals(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("money"));

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("金额为0时抛出异常")
        void createOrder_shouldThrowException_whenMoneyIsZero() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            request.setMoney(BigDecimal.ZERO);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> publicOrderService.createOrder(request));

            assertEquals(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("金额为负数时抛出异常")
        void createOrder_shouldThrowException_whenMoneyIsNegative() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            request.setMoney(new BigDecimal("-100.00"));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> publicOrderService.createOrder(request));

            assertEquals(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("重复订单号时抛出异常")
        void createOrder_shouldThrowException_whenDuplicateOutTradeNo() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            OrderEntity existingOrder = new OrderEntity();
            existingOrder.setOutTradeNo(request.getOutTradeNo());

            when(orderRepository.findByOutTradeNoAndPid(request.getOutTradeNo(), request.getPid()))
                    .thenReturn(Optional.of(existingOrder));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> publicOrderService.createOrder(request));

            assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("duplicate"));

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("商户密钥不存在时抛出异常")
        void createOrder_shouldThrowException_whenMerchantSecretNotFound() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();

            when(orderRepository.findByOutTradeNoAndPid(anyString(), anyLong()))
                    .thenReturn(Optional.empty());
            when(merchantSecretService.getSecret(request.getPid()))
                    .thenReturn(Optional.empty());

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> publicOrderService.createOrder(request));

            assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("secret"));

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("签名验证失败时抛出异常")
        void createOrder_shouldThrowException_whenSignatureInvalid() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();

            when(orderRepository.findByOutTradeNoAndPid(anyString(), anyLong()))
                    .thenReturn(Optional.empty());
            when(merchantSecretService.getSecret(anyLong()))
                    .thenReturn(Optional.of("secret"));
            when(signatureService.verify(any(), anyString()))
                    .thenReturn(false);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> publicOrderService.createOrder(request));

            assertEquals(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("signature"));

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("无可用通道时抛出异常")
        void createOrder_shouldThrowException_whenNoChannelAvailable() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();

            when(orderRepository.findByOutTradeNoAndPid(anyString(), anyLong()))
                    .thenReturn(Optional.empty());
            when(merchantSecretService.getSecret(anyLong()))
                    .thenReturn(Optional.of("secret"));
            when(signatureService.verify(any(), anyString()))
                    .thenReturn(true);
            when(channelSelector.select(anyLong(), anyString()))
                    .thenReturn(Optional.empty());

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> publicOrderService.createOrder(request));

            assertEquals(ErrorCode.SERVICE_UNAVAILABLE, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("channel"));

            verify(orderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("订单实体构建测试")
    class OrderEntityBuildingTests {

        @Test
        @DisplayName("订单ID以H开头")
        void createOrder_shouldGenerateOrderIdStartingWithH() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            setupSuccessfulMocks();

            // When
            PublicCreateOrderResult result = publicOrderService.createOrder(request);

            // Then
            assertTrue(result.getOrderId().startsWith("H"));
        }

        @Test
        @DisplayName("收银台URL包含订单ID")
        void createOrder_shouldGenerateCashierUrlWithOrderId() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            setupSuccessfulMocks();

            // When
            PublicCreateOrderResult result = publicOrderService.createOrder(request);

            // Then
            assertEquals("/cashier/" + result.getOrderId(), result.getCashierUrl());
        }

        @Test
        @DisplayName("订单初始状态为待支付")
        void createOrder_shouldSetInitialStateToZero() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            setupSuccessfulMocks();

            // When
            publicOrderService.createOrder(request);

            // Then
            ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
            verify(orderRepository).save(orderCaptor.capture());

            assertEquals(0, orderCaptor.getValue().getState());
        }

        @Test
        @DisplayName("订单关闭时间为创建时间加3分钟")
        void createOrder_shouldSetCloseTimeToThreeMinutesAfterCreate() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            setupSuccessfulMocks();

            // When
            publicOrderService.createOrder(request);

            // Then
            ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
            verify(orderRepository).save(orderCaptor.capture());

            OrderEntity savedOrder = orderCaptor.getValue();
            assertNotNull(savedOrder.getCreateTime());
            assertNotNull(savedOrder.getCloseTime());
            assertEquals(savedOrder.getCreateTime().plusMinutes(3), savedOrder.getCloseTime());
        }

        @Test
        @DisplayName("空附加参数时不设置param字段")
        void createOrder_shouldNotSetParam_whenAttachIsNull() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            request.setAttach(null);
            setupSuccessfulMocks();

            // When
            publicOrderService.createOrder(request);

            // Then
            ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
            verify(orderRepository).save(orderCaptor.capture());

            assertNull(orderCaptor.getValue().getParam());
        }

        @Test
        @DisplayName("空Map附加参数时不设置param字段")
        void createOrder_shouldNotSetParam_whenAttachIsEmpty() {
            // Given
            PublicCreateOrderDTO request = createValidRequest();
            request.setAttach(Map.of());
            setupSuccessfulMocks();

            // When
            publicOrderService.createOrder(request);

            // Then
            ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
            verify(orderRepository).save(orderCaptor.capture());

            assertNull(orderCaptor.getValue().getParam());
        }
    }

    // ==================== 辅助方法 ====================

    private PublicCreateOrderDTO createValidRequest() {
        PublicCreateOrderDTO request = new PublicCreateOrderDTO();
        request.setPid(1001L);
        request.setType("wxpay");
        request.setOutTradeNo("OUT202411250001");
        request.setName("测试商品");
        request.setMoney(new BigDecimal("100.00"));
        request.setNotifyUrl("http://merchant.com/notify");
        request.setReturnUrl("http://merchant.com/return");
        request.setClientIp("127.0.0.1");
        request.setDevice("web");
        request.setSign("test-sign");
        return request;
    }

    private void setupSuccessfulMocks() {
        when(orderRepository.findByOutTradeNoAndPid(anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(merchantSecretService.getSecret(anyLong()))
                .thenReturn(Optional.of("secret"));
        when(signatureService.verify(any(), anyString()))
                .thenReturn(true);
        when(channelSelector.select(anyLong(), anyString()))
                .thenReturn(Optional.of(new ChannelSelector.ChannelSelection(1L, 1L, 1)));
        when(priceAllocator.allocate(any(), anyLong(), anyLong(), anyString()))
                .thenReturn(new BigDecimal("100.00"));
        when(orderRepository.save(any(OrderEntity.class)))
                .thenAnswer(i -> i.getArgument(0));
    }
}
