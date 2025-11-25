package com.github.nonfou.mpay.service;

import com.github.nonfou.mpay.common.error.BusinessException;
import com.github.nonfou.mpay.common.error.ErrorCode;
import com.github.nonfou.mpay.dto.monitor.PaymentRecordDTO;
import com.github.nonfou.mpay.service.impl.PaymentMatchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PaymentMatchService 单元测试
 * 测试支付记录匹配服务
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentMatchService 支付匹配服务测试")
class PaymentMatchServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private PaymentMatchServiceImpl paymentMatchService;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        paymentMatchService = new PaymentMatchServiceImpl(webClientBuilder, redisTemplate);
    }

    @Nested
    @DisplayName("处理支付记录测试")
    class HandlePaymentRecordTests {

        @Test
        @DisplayName("处理支付记录成功 - 首次收到")
        @SuppressWarnings("unchecked")
        void handlePaymentRecord_shouldSucceed_whenFirstTime() {
            // Given
            PaymentRecordDTO record = createPaymentRecord("PLT123456");

            when(redisTemplate.hasKey("mpay:record:PLT123456")).thenReturn(false);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            setupSuccessfulWebClientMock();

            // When
            paymentMatchService.handlePaymentRecord(record);

            // Then
            verify(valueOperations).set("mpay:record:PLT123456", "1");
            verify(webClient).post();
        }

        @Test
        @DisplayName("跳过重复支付记录")
        void handlePaymentRecord_shouldSkip_whenDuplicate() {
            // Given
            PaymentRecordDTO record = createPaymentRecord("PLT123456");

            when(redisTemplate.hasKey("mpay:record:PLT123456")).thenReturn(true);

            // When
            paymentMatchService.handlePaymentRecord(record);

            // Then
            verify(webClient, never()).post();
            verify(redisTemplate, never()).opsForValue();
        }

        @Test
        @DisplayName("处理支付记录 - 无平台订单号时跳过去重检查")
        @SuppressWarnings("unchecked")
        void handlePaymentRecord_shouldNotCheckDedup_whenNoPlatformOrder() {
            // Given
            PaymentRecordDTO record = PaymentRecordDTO.builder()
                    .pid(1001L)
                    .aid(1L)
                    .payway("wxpay")
                    .price(new BigDecimal("100.00"))
                    .platformOrder(null)
                    .build();

            setupSuccessfulWebClientMock();

            // When
            paymentMatchService.handlePaymentRecord(record);

            // Then
            verify(redisTemplate, never()).hasKey(anyString());
            verify(redisTemplate, never()).opsForValue();
            verify(webClient).post();
        }

        @Test
        @DisplayName("处理支付记录失败 - 通知服务异常")
        @SuppressWarnings("unchecked")
        void handlePaymentRecord_shouldThrowException_whenNotifyFails() {
            // Given
            PaymentRecordDTO record = createPaymentRecord("PLT123456");

            when(redisTemplate.hasKey("mpay:record:PLT123456")).thenReturn(false);

            doReturn(requestBodyUriSpec).when(webClient).post();
            doReturn(requestBodyUriSpec).when(requestBodyUriSpec).uri(anyString());
            doReturn(requestHeadersSpec).when(requestBodyUriSpec).bodyValue(any());
            doReturn(responseSpec).when(requestHeadersSpec).retrieve();
            doReturn(responseSpec).when(responseSpec).onStatus(any(), any());
            when(responseSpec.bodyToMono(Void.class)).thenThrow(new RuntimeException("Connection refused"));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> paymentMatchService.handlePaymentRecord(record));

            assertEquals(ErrorCode.SERVER_ERROR, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("支付匹配失败"));

            // 失败时不应该设置去重key
            verify(redisTemplate, never()).opsForValue();
        }
    }

    @Nested
    @DisplayName("去重键生成测试")
    class DedupKeyTests {

        @Test
        @DisplayName("去重键格式正确")
        void dedupKey_shouldHaveCorrectFormat() {
            // Given
            PaymentRecordDTO record = createPaymentRecord("WX202411250001");

            when(redisTemplate.hasKey("mpay:record:WX202411250001")).thenReturn(true);

            // When
            paymentMatchService.handlePaymentRecord(record);

            // Then
            verify(redisTemplate).hasKey("mpay:record:WX202411250001");
        }
    }

    @Nested
    @DisplayName("请求构建测试")
    class RequestBuildingTests {

        @Test
        @DisplayName("请求发送到正确的URI")
        @SuppressWarnings("unchecked")
        void handlePaymentRecord_shouldSendToCorrectUri() {
            // Given
            PaymentRecordDTO record = createPaymentRecord("ALI123456");

            when(redisTemplate.hasKey(anyString())).thenReturn(false);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            doReturn(requestBodyUriSpec).when(webClient).post();
            doReturn(requestBodyUriSpec).when(requestBodyUriSpec).uri("/api/internal/orders/match");
            doReturn(requestHeadersSpec).when(requestBodyUriSpec).bodyValue(any());
            doReturn(responseSpec).when(requestHeadersSpec).retrieve();
            doReturn(responseSpec).when(responseSpec).onStatus(any(), any());
            when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

            // When
            paymentMatchService.handlePaymentRecord(record);

            // Then
            verify(requestBodyUriSpec).uri("/api/internal/orders/match");
        }

        @Test
        @DisplayName("请求包含所有必需字段")
        @SuppressWarnings("unchecked")
        void handlePaymentRecord_shouldIncludeAllFields() {
            // Given
            PaymentRecordDTO record = PaymentRecordDTO.builder()
                    .pid(1001L)
                    .aid(2L)
                    .payway("alipay")
                    .channel("personal")
                    .price(new BigDecimal("99.99"))
                    .platformOrder("ALI123456")
                    .build();

            when(redisTemplate.hasKey(anyString())).thenReturn(false);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            doReturn(requestBodyUriSpec).when(webClient).post();
            doReturn(requestBodyUriSpec).when(requestBodyUriSpec).uri(anyString());
            doReturn(requestHeadersSpec).when(requestBodyUriSpec).bodyValue(any());
            doReturn(responseSpec).when(requestHeadersSpec).retrieve();
            doReturn(responseSpec).when(responseSpec).onStatus(any(), any());
            when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

            // When
            paymentMatchService.handlePaymentRecord(record);

            // Then
            verify(requestBodyUriSpec).bodyValue(argThat(payload -> {
                if (payload instanceof java.util.Map) {
                    java.util.Map<String, Object> map = (java.util.Map<String, Object>) payload;
                    return map.containsKey("pid")
                            && map.containsKey("aid")
                            && map.containsKey("payway")
                            && map.containsKey("channel")
                            && map.containsKey("price")
                            && map.containsKey("platformOrder");
                }
                return false;
            }));
        }
    }

    // ==================== 辅助方法 ====================

    @SuppressWarnings("unchecked")
    private void setupSuccessfulWebClientMock() {
        doReturn(requestBodyUriSpec).when(webClient).post();
        doReturn(requestBodyUriSpec).when(requestBodyUriSpec).uri(anyString());
        doReturn(requestHeadersSpec).when(requestBodyUriSpec).bodyValue(any());
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        doReturn(responseSpec).when(responseSpec).onStatus(any(), any());
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());
    }

    private PaymentRecordDTO createPaymentRecord(String platformOrder) {
        return PaymentRecordDTO.builder()
                .pid(1001L)
                .aid(1L)
                .payway("wxpay")
                .channel("personal")
                .price(new BigDecimal("100.00"))
                .platformOrder(platformOrder)
                .build();
    }
}
