package com.mapy.monitor.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PaymentRecordDTO {
    Long pid;
    Long aid;
    String payway;
    String channel;
    BigDecimal price;
    String platformOrder;
    String rawPayload;
}
