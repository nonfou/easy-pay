package com.github.nonfou.mpay.dto.statistics;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

/**
 * 订单趋势 DTO
 */
@Data
@Builder
public class OrderTrendDTO {

    /** 日期 */
    private LocalDate date;

    /** 订单数 */
    private Long orderCount;

    /** 成交数 */
    private Long successCount;

    /** 成交金额 */
    private Double totalAmount;
}
