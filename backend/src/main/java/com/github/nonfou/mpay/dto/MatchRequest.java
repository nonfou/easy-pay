package com.github.nonfou.mpay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class MatchRequest {
    @NotNull(message = "商户ID不能为空")
    private Long pid;

    @NotNull(message = "账号ID不能为空")
    private Long aid;

    private String channel;

    @NotBlank(message = "支付方式不能为空")
    private String payway;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0")
    private BigDecimal price;

    private String platformOrder;
}
