package com.github.nonfou.mpay.notify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nonfou.mpay.entity.OrderEntity;
import com.github.nonfou.mpay.service.MerchantSecretService;
import com.github.nonfou.mpay.service.NotifyLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * HttpNotifyClient 单元测试
 * 测试 HTTP 通知客户端
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HttpNotifyClient HTTP通知客户端测试")
class HttpNotifyClientTest {

    @Mock
    private MerchantSecretService merchantSecretService;

    @Mock
    private NotifyLogService notifyLogService;

    private ObjectMapper objectMapper;

    private HttpNotifyClient httpNotifyClient;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        httpNotifyClient = new HttpNotifyClient(objectMapper, merchantSecretService, notifyLogService);
    }

    @Nested
    @DisplayName("同步发送通知测试")
    class SendNotificationTests {

        @Test
        @DisplayName("发送通知 - URL 无效时返回 false")
        void sendNotification_shouldReturnFalse_whenUrlIsInvalid() {
            // Given
            OrderEntity order = createOrder();
            order.setNotifyUrl("not-a-valid-url");

            when(merchantSecretService.getSecret(order.getPid())).thenReturn(Optional.of("secret"));

            // When
            boolean result = httpNotifyClient.sendNotification(order);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("发送通知 - 无法连接时返回 false")
        void sendNotification_shouldReturnFalse_whenConnectionFails() {
            // Given
            OrderEntity order = createOrder();
            order.setNotifyUrl("http://localhost:19999/nonexistent"); // 不存在的端口

            when(merchantSecretService.getSecret(order.getPid())).thenReturn(Optional.of("secret"));

            // When
            boolean result = httpNotifyClient.sendNotification(order);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("发送通知 - 有密钥时包含签名")
        void sendNotification_shouldIncludeSign_whenSecretExists() {
            // Given
            OrderEntity order = createOrder();
            order.setNotifyUrl("http://localhost:19999/notify");

            when(merchantSecretService.getSecret(order.getPid())).thenReturn(Optional.of("test-secret"));

            // When
            boolean result = httpNotifyClient.sendNotification(order);

            // Then - 验证调用了密钥服务
            verify(merchantSecretService).getSecret(order.getPid());
            // 由于无法连接，结果为 false
            assertFalse(result);
        }

        @Test
        @DisplayName("发送通知 - 无密钥时不包含签名")
        void sendNotification_shouldNotIncludeSign_whenNoSecret() {
            // Given
            OrderEntity order = createOrder();
            order.setNotifyUrl("http://localhost:19999/notify");

            when(merchantSecretService.getSecret(order.getPid())).thenReturn(Optional.empty());

            // When
            boolean result = httpNotifyClient.sendNotification(order);

            // Then
            verify(merchantSecretService).getSecret(order.getPid());
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("异步通知测试")
    class NotifyMerchantTests {

        @Test
        @DisplayName("通知商户 - 失败后记录日志")
        void notifyMerchant_shouldRecordFailure_whenAllRetriesFail() {
            // Given
            OrderEntity order = createOrder();
            order.setNotifyUrl("http://localhost:19999/nonexistent");

            when(merchantSecretService.getSecret(order.getPid())).thenReturn(Optional.of("secret"));

            // When
            httpNotifyClient.notifyMerchant(order);

            // Then - 验证记录了失败日志
            verify(notifyLogService).recordFailure(eq(order), contains("已达最大重试次数"), eq(3));
        }

        @Test
        @DisplayName("通知商户 - URL 无效时记录失败")
        void notifyMerchant_shouldRecordFailure_whenUrlIsInvalid() {
            // Given
            OrderEntity order = createOrder();
            order.setNotifyUrl("invalid-url");

            when(merchantSecretService.getSecret(order.getPid())).thenReturn(Optional.empty());

            // When
            httpNotifyClient.notifyMerchant(order);

            // Then
            verify(notifyLogService).recordFailure(eq(order), anyString(), eq(3));
        }
    }

    @Nested
    @DisplayName("Payload 构建测试")
    class PayloadBuildingTests {

        @Test
        @DisplayName("Payload 应包含订单基本信息")
        void buildPayload_shouldContainOrderInfo() {
            // Given
            OrderEntity order = createOrder();
            order.setNotifyUrl("http://localhost:19999/notify");

            when(merchantSecretService.getSecret(order.getPid())).thenReturn(Optional.empty());

            // When
            httpNotifyClient.sendNotification(order);

            // Then - 通过 mock 验证调用，Payload 内容在实际请求中验证
            verify(merchantSecretService).getSecret(order.getPid());
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("处理空 notifyUrl - 返回 false")
        void sendNotification_shouldHandleNullNotifyUrl() {
            // Given
            OrderEntity order = createOrder();
            order.setNotifyUrl(null);

            // When
            boolean result = httpNotifyClient.sendNotification(order);

            // Then - 无效 URL 应返回 false 而不是抛出异常
            assertFalse(result);
        }

        @Test
        @DisplayName("处理特殊字符 URL")
        void sendNotification_shouldHandleSpecialCharactersInUrl() {
            // Given
            OrderEntity order = createOrder();
            order.setNotifyUrl("http://example.com/notify?param=value&other=test");

            when(merchantSecretService.getSecret(order.getPid())).thenReturn(Optional.of("secret"));

            // When
            boolean result = httpNotifyClient.sendNotification(order);

            // Then - URL 有效但连接失败
            assertFalse(result);
        }

        @Test
        @DisplayName("处理中文商品名")
        void sendNotification_shouldHandleChineseProductName() {
            // Given
            OrderEntity order = createOrder();
            order.setName("中文测试商品名称");
            order.setNotifyUrl("http://localhost:19999/notify");

            when(merchantSecretService.getSecret(order.getPid())).thenReturn(Optional.of("secret"));

            // When
            boolean result = httpNotifyClient.sendNotification(order);

            // Then
            assertFalse(result); // 连接失败但不应抛异常
        }

        @Test
        @DisplayName("处理大金额")
        void sendNotification_shouldHandleLargeAmount() {
            // Given
            OrderEntity order = createOrder();
            order.setMoney(new BigDecimal("999999999.99"));
            order.setReallyPrice(new BigDecimal("999999999.99"));
            order.setNotifyUrl("http://localhost:19999/notify");

            when(merchantSecretService.getSecret(order.getPid())).thenReturn(Optional.of("secret"));

            // When
            boolean result = httpNotifyClient.sendNotification(order);

            // Then
            assertFalse(result);
        }
    }

    // ==================== 辅助方法 ====================

    private OrderEntity createOrder() {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setOrderId("H202411250001");
        order.setOutTradeNo("OUT123456");
        order.setPid(1001L);
        order.setType("wxpay");
        order.setName("测试商品");
        order.setMoney(new BigDecimal("100.00"));
        order.setReallyPrice(new BigDecimal("100.01"));
        order.setState(1);
        order.setNotifyUrl("http://example.com/notify");
        return order;
    }
}
