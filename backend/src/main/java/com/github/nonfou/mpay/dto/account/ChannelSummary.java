package com.github.nonfou.mpay.dto.account;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChannelSummary {
    Long id;
    String channel;
    String qrcode;
    Integer state;
    String type;
}
