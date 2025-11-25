package com.github.nonfou.mpay.service.impl;

import com.github.nonfou.mpay.dto.monitor.OrderHeartbeatDTO;
import com.github.nonfou.mpay.service.OrderHeartbeatService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisStreamOrderHeartbeatService implements OrderHeartbeatService {

    private static final String STREAM_KEY = "mpay:order:heartbeat";

    private final StreamOperations<String, String, String> streamOps;

    public RedisStreamOrderHeartbeatService(StringRedisTemplate redisTemplate) {
        this.streamOps = redisTemplate.opsForStream();
    }

    @Override
    public void publishActiveOrders(List<OrderHeartbeatDTO> orders) {
        for (OrderHeartbeatDTO order : orders) {
            Map<String, String> fields = toMap(order);
            MapRecord<String, String, String> record = MapRecord.create(STREAM_KEY, fields);
            streamOps.add(record);
        }
    }

    @Override
    public List<OrderHeartbeatDTO> fetchActiveOrders(String pid) {
        List<MapRecord<String, String, String>> records =
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

    private Map<String, String> toMap(OrderHeartbeatDTO dto) {
        Map<String, String> map = new HashMap<>();
        map.put("orderId", dto.getOrderId());
        map.put("pid", dto.getPid() != null ? String.valueOf(dto.getPid()) : "");
        map.put("aid", dto.getAid() != null ? String.valueOf(dto.getAid()) : "");
        map.put("cid", dto.getCid() != null ? String.valueOf(dto.getCid()) : "");
        map.put("type", dto.getType() != null ? dto.getType() : "");
        map.put("expiresAt", dto.getExpiresAt() != null ? dto.getExpiresAt().toString() : "");
        map.put("pattern", dto.getPattern() != null ? String.valueOf(dto.getPattern()) : "");
        return map;
    }

    private OrderHeartbeatDTO fromMap(Map<String, String> map) {
        return OrderHeartbeatDTO.builder()
                .orderId(map.get("orderId"))
                .pid(parseLong(map.get("pid")))
                .aid(parseLong(map.get("aid")))
                .cid(parseLong(map.get("cid")))
                .type(map.get("type"))
                .expiresAt(parseInstant(map.get("expiresAt")))
                .pattern(parseInteger(map.get("pattern")))
                .build();
    }

    private Long parseLong(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return Long.parseLong(value);
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return Integer.parseInt(value);
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return Instant.parse(value);
    }
}
