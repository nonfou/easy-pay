package com.mapy.payment.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PublicCreateOrderResult {
    String orderId;
    String cashierUrl;
}
