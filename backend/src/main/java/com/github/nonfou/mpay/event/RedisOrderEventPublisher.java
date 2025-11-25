package com.github.nonfou.mpay.event;

import com.github.nonfou.mpay.entity.OrderEntity;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisOrderEventPublisher implements OrderEventPublisher {

    private static final String STREAM_KEY = "mpay:order:heartbeat";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final StreamOperations<String, String, String> streamOps;

    public RedisOrderEventPublisher(StringRedisTemplate redisTemplate) {
        this.streamOps = redisTemplate.opsForStream();
    }

    @Override
    public void publish(OrderEntity order) {
        Map<String, String> payload = new HashMap<>();
        payload.put("orderId", order.getOrderId());
        payload.put("pid", String.valueOf(order.getPid()));
        payload.put("aid", order.getAid() != null ? String.valueOf(order.getAid()) : "");
        payload.put("cid", order.getCid() != null ? String.valueOf(order.getCid()) : "");
        payload.put("type", order.getType());
        payload.put("pattern", order.getPatt() != null ? String.valueOf(order.getPatt()) : "");
        payload.put("expiresAt", order.getCloseTime() != null ? order.getCloseTime().format(FORMATTER) : "");
        MapRecord<String, String, String> record = MapRecord.create(STREAM_KEY, payload);
        streamOps.add(record);
    }
}
