package com.github.nonfou.mpay.service.impl;

import com.github.nonfou.mpay.dto.monitor.OrderHeartbeatDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RedisStreamOrderHeartbeatService 单元测试
 * 测试 Redis Stream 订单心跳服务
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisStreamOrderHeartbeatService 订单心跳服务测试")
@SuppressWarnings("unchecked")
class RedisStreamOrderHeartbeatServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOps;

    private RedisStreamOrderHeartbeatService heartbeatService;

    private static final String STREAM_KEY = "mpay:order:heartbeat";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForStream()).thenReturn(streamOps);
        heartbeatService = new RedisStreamOrderHeartbeatService(redisTemplate);
    }

    @Nested
    @DisplayName("发布活跃订单测试")
    class PublishActiveOrdersTests {

        @Test
        @DisplayName("发布成功 - 单个订单")
        void publishActiveOrders_shouldAddToStream_forSingleOrder() {
            // Given
            OrderHeartbeatDTO order = createHeartbeat("ORDER001", 1001L, 1L, 10L, "wxpay", 0);

            // When
            heartbeatService.publishActiveOrders(List.of(order));

            // Then
            verify(streamOps, times(1)).add(any(MapRecord.class));
        }

        @Test
        @DisplayName("发布成功 - 多个订单")
        void publishActiveOrders_shouldAddToStream_forMultipleOrders() {
            // Given
            List<OrderHeartbeatDTO> orders = List.of(
                    createHeartbeat("ORDER001", 1001L, 1L, 10L, "wxpay", 0),
                    createHeartbeat("ORDER002", 1002L, 2L, 20L, "alipay", 1),
                    createHeartbeat("ORDER003", 1001L, 1L, 10L, "wxpay", 0)
            );

            // When
            heartbeatService.publishActiveOrders(orders);

            // Then
            verify(streamOps, times(3)).add(any(MapRecord.class));
        }

        @Test
        @DisplayName("发布成功 - 空列表不调用 add")
        void publishActiveOrders_shouldNotAddToStream_forEmptyList() {
            // When
            heartbeatService.publishActiveOrders(List.of());

            // Then
            verify(streamOps, never()).add(any(MapRecord.class));
        }

        @Test
        @DisplayName("发布成功 - 记录包含正确的字段")
        @SuppressWarnings("unchecked")
        void publishActiveOrders_shouldContainCorrectFields() {
            // Given
            Instant expiresAt = Instant.now().plusSeconds(300);
            OrderHeartbeatDTO order = OrderHeartbeatDTO.builder()
                    .orderId("ORDER001")
                    .pid(1001L)
                    .aid(1L)
                    .cid(10L)
                    .type("wxpay")
                    .expiresAt(expiresAt)
                    .pattern(1)
                    .build();

            ArgumentCaptor<MapRecord<String, String, String>> captor = ArgumentCaptor.forClass(MapRecord.class);

            // When
            heartbeatService.publishActiveOrders(List.of(order));

            // Then
            verify(streamOps).add(captor.capture());
            MapRecord<String, String, String> record = captor.getValue();

            assertEquals(STREAM_KEY, record.getStream());
            Map<String, String> value = record.getValue();
            assertEquals("ORDER001", value.get("orderId"));
            assertEquals("1001", value.get("pid"));
            assertEquals("1", value.get("aid"));
            assertEquals("10", value.get("cid"));
            assertEquals("wxpay", value.get("type"));
            assertEquals(expiresAt.toString(), value.get("expiresAt"));
            assertEquals("1", value.get("pattern"));
        }

        @Test
        @DisplayName("发布成功 - 处理 null 值字段")
        @SuppressWarnings("unchecked")
        void publishActiveOrders_shouldHandleNullFields() {
            // Given
            OrderHeartbeatDTO order = OrderHeartbeatDTO.builder()
                    .orderId("ORDER001")
                    .pid(null)
                    .aid(null)
                    .cid(null)
                    .type(null)
                    .expiresAt(null)
                    .pattern(null)
                    .build();

            ArgumentCaptor<MapRecord<String, String, String>> captor = ArgumentCaptor.forClass(MapRecord.class);

            // When
            heartbeatService.publishActiveOrders(List.of(order));

            // Then
            verify(streamOps).add(captor.capture());
            Map<String, String> value = captor.getValue().getValue();
            assertEquals("ORDER001", value.get("orderId"));
            assertEquals("", value.get("pid"));
            assertEquals("", value.get("aid"));
            assertEquals("", value.get("cid"));
            assertEquals("", value.get("type"));
            assertEquals("", value.get("expiresAt"));
            assertEquals("", value.get("pattern"));
        }
    }

    @Nested
    @DisplayName("获取活跃订单测试")
    class FetchActiveOrdersTests {

        @Test
        @DisplayName("获取成功 - 返回所有订单")
        void fetchActiveOrders_shouldReturnAllOrders_whenPidIsNull() {
            // Given
            Instant expiresAt = Instant.now().plusSeconds(300);
            MapRecord<String, String, String> record1 = createMapRecord(
                    "ORDER001", "1001", "1", "10", "wxpay", expiresAt.toString(), "0");
            MapRecord<String, String, String> record2 = createMapRecord(
                    "ORDER002", "1002", "2", "20", "alipay", expiresAt.toString(), "1");

            when(streamOps.read(any(StreamOffset.class))).thenReturn(List.of(record1, record2));

            // When
            List<OrderHeartbeatDTO> result = heartbeatService.fetchActiveOrders(null);

            // Then
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("获取成功 - 按 PID 筛选")
        void fetchActiveOrders_shouldFilterByPid_whenPidProvided() {
            // Given
            Instant expiresAt = Instant.now().plusSeconds(300);
            MapRecord<String, String, String> record1 = createMapRecord(
                    "ORDER001", "1001", "1", "10", "wxpay", expiresAt.toString(), "0");
            MapRecord<String, String, String> record2 = createMapRecord(
                    "ORDER002", "1002", "2", "20", "alipay", expiresAt.toString(), "1");

            when(streamOps.read(any(StreamOffset.class))).thenReturn(List.of(record1, record2));

            // When
            List<OrderHeartbeatDTO> result = heartbeatService.fetchActiveOrders("1001");

            // Then
            assertEquals(1, result.size());
            assertEquals("ORDER001", result.get(0).getOrderId());
            assertEquals(1001L, result.get(0).getPid());
        }

        @Test
        @DisplayName("获取成功 - 正确解析 DTO 字段")
        void fetchActiveOrders_shouldParseFieldsCorrectly() {
            // Given
            Instant expiresAt = Instant.parse("2025-11-25T12:00:00Z");
            MapRecord<String, String, String> record = createMapRecord(
                    "ORDER001", "1001", "5", "50", "wxpay", expiresAt.toString(), "1");

            when(streamOps.read(any(StreamOffset.class))).thenReturn(List.of(record));

            // When
            List<OrderHeartbeatDTO> result = heartbeatService.fetchActiveOrders(null);

            // Then
            assertEquals(1, result.size());
            OrderHeartbeatDTO dto = result.get(0);
            assertEquals("ORDER001", dto.getOrderId());
            assertEquals(1001L, dto.getPid());
            assertEquals(5L, dto.getAid());
            assertEquals(50L, dto.getCid());
            assertEquals("wxpay", dto.getType());
            assertEquals(expiresAt, dto.getExpiresAt());
            assertEquals(1, dto.getPattern());
        }

        @Test
        @DisplayName("获取成功 - 处理空值字段")
        void fetchActiveOrders_shouldHandleEmptyFields() {
            // Given
            MapRecord<String, String, String> record = createMapRecord(
                    "ORDER001", "", "", "", "", "", "");

            when(streamOps.read(any(StreamOffset.class))).thenReturn(List.of(record));

            // When
            List<OrderHeartbeatDTO> result = heartbeatService.fetchActiveOrders(null);

            // Then
            assertEquals(1, result.size());
            OrderHeartbeatDTO dto = result.get(0);
            assertEquals("ORDER001", dto.getOrderId());
            assertNull(dto.getPid());
            assertNull(dto.getAid());
            assertNull(dto.getCid());
            assertEquals("", dto.getType());
            assertNull(dto.getExpiresAt());
            assertNull(dto.getPattern());
        }

        @Test
        @DisplayName("获取成功 - 流为空返回空列表")
        void fetchActiveOrders_shouldReturnEmptyList_whenStreamIsEmpty() {
            // Given
            when(streamOps.read(any(StreamOffset.class))).thenReturn(List.of());

            // When
            List<OrderHeartbeatDTO> result = heartbeatService.fetchActiveOrders(null);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("获取成功 - 流返回 null 返回空列表")
        void fetchActiveOrders_shouldReturnEmptyList_whenStreamReturnsNull() {
            // Given
            when(streamOps.read(any(StreamOffset.class))).thenReturn(null);

            // When
            List<OrderHeartbeatDTO> result = heartbeatService.fetchActiveOrders(null);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("获取成功 - PID 筛选无匹配返回空列表")
        void fetchActiveOrders_shouldReturnEmptyList_whenNoMatchingPid() {
            // Given
            Instant expiresAt = Instant.now();
            MapRecord<String, String, String> record = createMapRecord(
                    "ORDER001", "1001", "1", "10", "wxpay", expiresAt.toString(), "0");

            when(streamOps.read(any(StreamOffset.class))).thenReturn(List.of(record));

            // When
            List<OrderHeartbeatDTO> result = heartbeatService.fetchActiveOrders("9999");

            // Then
            assertTrue(result.isEmpty());
        }
    }

    // ==================== 辅助方法 ====================

    private OrderHeartbeatDTO createHeartbeat(String orderId, Long pid, Long aid, Long cid, String type, Integer pattern) {
        return OrderHeartbeatDTO.builder()
                .orderId(orderId)
                .pid(pid)
                .aid(aid)
                .cid(cid)
                .type(type)
                .expiresAt(Instant.now().plusSeconds(300))
                .pattern(pattern)
                .build();
    }

    private MapRecord<String, String, String> createMapRecord(
            String orderId, String pid, String aid, String cid, String type, String expiresAt, String pattern) {
        Map<String, String> fields = Map.of(
                "orderId", orderId,
                "pid", pid,
                "aid", aid,
                "cid", cid,
                "type", type,
                "expiresAt", expiresAt,
                "pattern", pattern
        );
        return MapRecord.create(STREAM_KEY, fields);
    }
}
