package com.github.nonfou.mpay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;
import lombok.Data;

@Data
public class PublicCreateOrderDTO {

    @NotNull
    private Long pid;

    @NotBlank
    private String type;

    @NotBlank
    private String outTradeNo;

    @NotBlank
    private String name;

    @DecimalMin(value = "0.01")
    private BigDecimal money;

    @NotBlank
    private String notifyUrl;

    private String returnUrl;

    private String clientIp;

    private String device;

    private Map<String, Object> attach;

    @NotBlank
    private String sign;

    private String signType;
}
