package com.github.nonfou.mpay.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MatchRequest {
    private Long pid;
    private Long aid;
    private String channel;
    private String payway;
    private BigDecimal price;
    private String platformOrder;
}
