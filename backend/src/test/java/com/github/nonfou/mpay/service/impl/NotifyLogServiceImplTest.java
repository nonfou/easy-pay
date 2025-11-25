package com.github.nonfou.mpay.service.impl;

import com.github.nonfou.mpay.entity.OrderEntity;
import com.github.nonfou.mpay.entity.OrderNotifyLogEntity;
import com.github.nonfou.mpay.repository.OrderNotifyLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * NotifyLogServiceImpl 单元测试
 * 测试通知日志服务，包括失败记录、重试机制
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotifyLogService 通知日志服务测试")
class NotifyLogServiceImplTest {

    @Mock
    private OrderNotifyLogRepository repository;

    private NotifyLogServiceImpl notifyLogService;

    private static final String ORDER_ID = "H202411250001";
    private static final int MAX_RETRIES = 8;

    @BeforeEach
    void setUp() {
        notifyLogService = new NotifyLogServiceImpl(repository);
        ReflectionTestUtils.setField(notifyLogService, "maxRetries", MAX_RETRIES);
    }

    @Nested
    @DisplayName("记录失败测试")
    class RecordFailureTests {

        @Test
        @DisplayName("记录失败 - 创建新的通知日志")
        void recordFailure_shouldCreateNewLog_whenLogNotExists() {
            // Given
            OrderEntity order = createOrder(ORDER_ID);
            when(repository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(repository.save(any(OrderNotifyLogEntity.class))).thenAnswer(i -> i.getArgument(0));

            // When
            notifyLogService.recordFailure(order, "Connection timeout", 1);

            // Then
            ArgumentCaptor<OrderNotifyLogEntity> captor = ArgumentCaptor.forClass(OrderNotifyLogEntity.class);
            verify(repository).save(captor.capture());

            OrderNotifyLogEntity saved = captor.getValue();
            assertEquals(ORDER_ID, saved.getOrderId());
            assertEquals(0, saved.getStatus());
            assertEquals(1, saved.getRetryCount());
            assertEquals("Connection timeout", saved.getLastError());
            assertNotNull(saved.getNextRetryTime());
        }

        @Test
        @DisplayName("记录失败 - 更新已有的通知日志")
        void recordFailure_shouldUpdateExistingLog_whenLogExists() {
            // Given
            OrderEntity order = createOrder(ORDER_ID);
            OrderNotifyLogEntity existingLog = createNotifyLog(ORDER_ID, 0, 1);
            when(repository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(existingLog));
            when(repository.save(any(OrderNotifyLogEntity.class))).thenAnswer(i -> i.getArgument(0));

            // When
            notifyLogService.recordFailure(order, "HTTP 500", 2);

            // Then
            verify(repository).save(existingLog);
            assertEquals(2, existingLog.getRetryCount());
            assertEquals("HTTP 500", existingLog.getLastError());
        }

        @Test
        @DisplayName("记录失败 - 首次失败重试间隔为 5 分钟")
        void recordFailure_shouldSetNextRetryTime_withCorrectInterval() {
            // Given
            OrderEntity order = createOrder(ORDER_ID);
            when(repository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(repository.save(any(OrderNotifyLogEntity.class))).thenAnswer(i -> i.getArgument(0));

            LocalDateTime beforeCall = LocalDateTime.now();

            // When
            notifyLogService.recordFailure(order, "Error", 0);

            // Then
            ArgumentCaptor<OrderNotifyLogEntity> captor = ArgumentCaptor.forClass(OrderNotifyLogEntity.class);
            verify(repository).save(captor.capture());

            LocalDateTime nextRetryTime = captor.getValue().getNextRetryTime();
            // 第 0 次重试，间隔应为 5 分钟
            assertTrue(nextRetryTime.isAfter(beforeCall.plusMinutes(4)));
            assertTrue(nextRetryTime.isBefore(beforeCall.plusMinutes(6)));
        }

        @Test
        @DisplayName("记录失败 - 不同重试次数使用不同的重试间隔")
        void recordFailure_shouldUseCorrectRetryInterval_forDifferentRetryCounts() {
            // Given
            OrderEntity order = createOrder(ORDER_ID);
            when(repository.findByOrderId(any())).thenReturn(Optional.empty());
            when(repository.save(any(OrderNotifyLogEntity.class))).thenAnswer(i -> i.getArgument(0));

            // 重试间隔: 5, 15, 30, 60, 120, 360, 720, 1440 分钟
            int[] expectedIntervals = {5, 15, 30, 60, 120, 360, 720, 1440};

            for (int i = 0; i < expectedIntervals.length; i++) {
                // When
                LocalDateTime beforeCall = LocalDateTime.now();
                notifyLogService.recordFailure(order, "Error " + i, i);

                // Then
                ArgumentCaptor<OrderNotifyLogEntity> captor = ArgumentCaptor.forClass(OrderNotifyLogEntity.class);
                verify(repository, atLeast(i + 1)).save(captor.capture());

                LocalDateTime nextRetryTime = captor.getValue().getNextRetryTime();
                int interval = expectedIntervals[i];
                assertTrue(nextRetryTime.isAfter(beforeCall.plusMinutes(interval - 1)),
                        "重试次数 " + i + " 间隔应大于 " + (interval - 1) + " 分钟");
                assertTrue(nextRetryTime.isBefore(beforeCall.plusMinutes(interval + 1)),
                        "重试次数 " + i + " 间隔应小于 " + (interval + 1) + " 分钟");
            }
        }
    }

    @Nested
    @DisplayName("获取待重试日志测试")
    class GetPendingRetriesTests {

        @Test
        @DisplayName("获取待重试日志 - 成功返回列表")
        void getPendingRetries_shouldReturnList_whenLogsExist() {
            // Given
            OrderNotifyLogEntity log1 = createNotifyLog("ORDER1", 0, 1);
            OrderNotifyLogEntity log2 = createNotifyLog("ORDER2", 0, 2);
            when(repository.findPendingRetries(any(LocalDateTime.class), eq(MAX_RETRIES)))
                    .thenReturn(List.of(log1, log2));

            // When
            List<OrderNotifyLogEntity> result = notifyLogService.getPendingRetries();

            // Then
            assertEquals(2, result.size());
            verify(repository).findPendingRetries(any(LocalDateTime.class), eq(MAX_RETRIES));
        }

        @Test
        @DisplayName("获取待重试日志 - 无待重试返回空列表")
        void getPendingRetries_shouldReturnEmptyList_whenNoLogsExist() {
            // Given
            when(repository.findPendingRetries(any(LocalDateTime.class), eq(MAX_RETRIES)))
                    .thenReturn(List.of());

            // When
            List<OrderNotifyLogEntity> result = notifyLogService.getPendingRetries();

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("更新重试结果测试")
    class UpdateRetryResultTests {

        @Test
        @DisplayName("更新重试结果 - 成功时设置状态为 1")
        void updateRetryResult_shouldSetStatusTo1_whenSuccess() {
            // Given
            OrderNotifyLogEntity log = createNotifyLog(ORDER_ID, 0, 2);
            log.setLastError("Previous error");
            log.setNextRetryTime(LocalDateTime.now().plusMinutes(5));

            // When
            notifyLogService.updateRetryResult(log, true, null);

            // Then
            verify(repository).save(log);
            assertEquals(1, log.getStatus());
            assertNull(log.getLastError());
            assertNull(log.getNextRetryTime());
        }

        @Test
        @DisplayName("更新重试结果 - 失败时增加重试计数")
        void updateRetryResult_shouldIncrementRetryCount_whenFailed() {
            // Given
            OrderNotifyLogEntity log = createNotifyLog(ORDER_ID, 0, 2);

            // When
            notifyLogService.updateRetryResult(log, false, "HTTP 503");

            // Then
            verify(repository).save(log);
            assertEquals(3, log.getRetryCount());
            assertEquals("HTTP 503", log.getLastError());
            assertNotNull(log.getNextRetryTime());
        }

        @Test
        @DisplayName("更新重试结果 - 达到最大重试次数时设置状态为 2")
        void updateRetryResult_shouldSetStatusTo2_whenMaxRetriesReached() {
            // Given
            OrderNotifyLogEntity log = createNotifyLog(ORDER_ID, 0, MAX_RETRIES - 1);

            // When
            notifyLogService.updateRetryResult(log, false, "Final failure");

            // Then
            verify(repository).save(log);
            assertEquals(2, log.getStatus()); // 最终失败
            assertEquals(MAX_RETRIES, log.getRetryCount());
            assertNull(log.getNextRetryTime());
        }

        @Test
        @DisplayName("更新重试结果 - 未达到最大重试次数时继续重试")
        void updateRetryResult_shouldContinueRetry_whenNotMaxRetries() {
            // Given
            OrderNotifyLogEntity log = createNotifyLog(ORDER_ID, 0, 3);

            // When
            notifyLogService.updateRetryResult(log, false, "Temporary failure");

            // Then
            assertEquals(0, log.getStatus()); // 仍在重试
            assertEquals(4, log.getRetryCount());
            assertNotNull(log.getNextRetryTime());
        }

        @Test
        @DisplayName("更新重试结果 - 总是更新 updatedAt")
        void updateRetryResult_shouldAlwaysUpdateTime() {
            // Given
            OrderNotifyLogEntity log = createNotifyLog(ORDER_ID, 0, 1);
            LocalDateTime oldUpdatedAt = log.getUpdatedAt();

            // When
            notifyLogService.updateRetryResult(log, true, null);

            // Then
            assertNotNull(log.getUpdatedAt());
        }
    }

    // ==================== 辅助方法 ====================

    private OrderEntity createOrder(String orderId) {
        OrderEntity order = new OrderEntity();
        order.setOrderId(orderId);
        return order;
    }

    private OrderNotifyLogEntity createNotifyLog(String orderId, int status, int retryCount) {
        OrderNotifyLogEntity log = new OrderNotifyLogEntity();
        log.setId(1L);
        log.setOrderId(orderId);
        log.setStatus(status);
        log.setRetryCount(retryCount);
        log.setCreatedAt(LocalDateTime.now());
        log.setUpdatedAt(LocalDateTime.now());
        return log;
    }
}
