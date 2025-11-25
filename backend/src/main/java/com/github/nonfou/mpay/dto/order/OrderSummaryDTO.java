package com.github.nonfou.mpay.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 订单摘要 DTO
 */
@Data
@Builder
public class OrderSummaryDTO {

    private Long id;
    private String orderId;
    private String outTradeNo;
    private Long pid;
    private String type;
    private String name;
    private BigDecimal money;
    private BigDecimal reallyPrice;
    private Integer state;
    private String stateName;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime closeTime;

    /** 是否超时 */
    private Boolean expired;
}
