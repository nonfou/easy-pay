package com.github.nonfou.mpay.dto.statistics;

import lombok.Builder;
import lombok.Data;

/**
 * 收入统计 DTO
 */
@Data
@Builder
public class RevenueStatisticsDTO {

    /** 今日收入 */
    private Double todayRevenue;

    /** 昨日收入 */
    private Double yesterdayRevenue;

    /** 本周收入 */
    private Double weekRevenue;

    /** 本月收入 */
    private Double monthRevenue;

    /** 今日订单数 */
    private Long todayOrderCount;

    /** 昨日订单数 */
    private Long yesterdayOrderCount;

    /** 本周订单数 */
    private Long weekOrderCount;

    /** 本月订单数 */
    private Long monthOrderCount;

    /** 今日成功率 */
    private Double todaySuccessRate;
}
