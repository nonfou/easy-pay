package com.github.nonfou.mpay.dto.account;

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
