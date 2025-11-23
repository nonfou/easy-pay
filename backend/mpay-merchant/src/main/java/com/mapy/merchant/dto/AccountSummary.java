package com.mapy.merchant.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AccountSummary {
    Long id;
    Long pid;
    String platform;
    String account;
    Integer state;
    Integer pattern;
    Integer channelCount;
}
