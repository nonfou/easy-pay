package com.github.nonfou.mpay.dto.account;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChannelCreateRequest {

    @NotBlank
    private String channel;

    private String qrcode;

    private String type;
}
