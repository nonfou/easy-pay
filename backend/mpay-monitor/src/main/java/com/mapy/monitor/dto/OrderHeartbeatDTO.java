package com.mapy.monitor.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OrderHeartbeatDTO {
    String orderId;
    Long pid;
    Long aid;
    Long cid;
    String type;
    Instant expiresAt;
    Integer pattern;
}
