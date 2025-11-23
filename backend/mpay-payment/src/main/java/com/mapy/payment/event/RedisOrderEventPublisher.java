package com.mapy.payment.event;

import com.mapy.payment.entity.OrderEntity;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.stereotype.Component;

@Component
public class RedisOrderEventPublisher implements OrderEventPublisher {

    private static final String STREAM_KEY = "mpay:order:heartbeat";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final StreamOperations<String, Object, Object> streamOps;

    public RedisOrderEventPublisher(org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate) {
        this.streamOps = redisTemplate.opsForStream();
    }

    @Override
    public void publish(OrderEntity order) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", order.getOrderId());
        payload.put("pid", order.getPid());
        payload.put("aid", order.getAid());
        payload.put("cid", order.getCid());
        payload.put("type", order.getType());
        payload.put("pattern", order.getPatt());
        payload.put("expiresAt", order.getCloseTime() != null ? order.getCloseTime().format(FORMATTER) : null);
        MapRecord<String, Object, Object> record = MapRecord.create(STREAM_KEY, payload);
        streamOps.add(record);
    }
}
