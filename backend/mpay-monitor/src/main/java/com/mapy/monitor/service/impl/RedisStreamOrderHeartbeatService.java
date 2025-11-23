package com.mapy.monitor.service.impl;

import com.mapy.monitor.dto.OrderHeartbeatDTO;
import com.mapy.monitor.service.OrderHeartbeatService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.stereotype.Service;

@Service
public class RedisStreamOrderHeartbeatService implements OrderHeartbeatService {

    private static final String STREAM_KEY = "mpay:order:heartbeat";

    private final StreamOperations<String, Object, Object> streamOps;

    public RedisStreamOrderHeartbeatService(RedisTemplate<String, Object> redisTemplate) {
        this.streamOps = redisTemplate.opsForStream();
    }

    @Override
    public void publishActiveOrders(List<OrderHeartbeatDTO> orders) {
        for (OrderHeartbeatDTO order : orders) {
            Map<String, Object> fields = toMap(order);
            MapRecord<String, Object, Object> record = MapRecord.create(STREAM_KEY, fields);
            streamOps.add(record);
        }
    }

    @Override
    public List<OrderHeartbeatDTO> fetchActiveOrders(String pid) {
        List<MapRecord<String, Object, Object>> records =
                streamOps.read(StreamOffset.fromStart(STREAM_KEY));
        if (records == null) {
            return List.of();
        }
        List<OrderHeartbeatDTO> all = records.stream()
                .map(MapRecord::getValue)
                .map(this::fromMap)
                .collect(Collectors.toCollection(ArrayList::new));
        if (pid == null) {
            return all;
        }
        return all.stream()
                .filter(dto -> pid.equals(String.valueOf(dto.getPid())))
                .collect(Collectors.toList());
    }

    private Map<String, Object> toMap(OrderHeartbeatDTO dto) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", dto.getOrderId());
        map.put("pid", dto.getPid());
        map.put("aid", dto.getAid());
        map.put("cid", dto.getCid());
        map.put("type", dto.getType());
        map.put("expiresAt", dto.getExpiresAt() != null ? dto.getExpiresAt().toString() : null);
        map.put("pattern", dto.getPattern());
        return map;
    }

    private OrderHeartbeatDTO fromMap(Map<Object, Object> map) {
        return OrderHeartbeatDTO.builder()
                .orderId((String) map.get("orderId"))
                .pid(parseLong(map.get("pid")))
                .aid(parseLong(map.get("aid")))
                .cid(parseLong(map.get("cid")))
                .type((String) map.get("type"))
                .expiresAt(parseInstant(map.get("expiresAt")))
                .pattern(parseInteger(map.get("pattern")))
                .build();
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        return Long.parseLong(value.toString());
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        return Integer.parseInt(value.toString());
    }

    private Instant parseInstant(Object value) {
        if (value == null) {
            return null;
        }
        return Instant.parse(value.toString());
    }
}
